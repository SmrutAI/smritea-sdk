"""Tests of the request timeout of SmriteaClient, over real loopback sockets."""

from __future__ import annotations

import socket
import threading
import time
from dataclasses import dataclass
from typing import Callable
from unittest.mock import patch

import pytest
import urllib3
from urllib3.exceptions import ConnectTimeoutError, ProtocolError, ReadTimeoutError
from urllib3.response import HTTPResponse

import smritea
from smritea import DEFAULT_TIMEOUT_SECONDS, SmriteaClient
from smritea._http_client import (
    _default_pool_manager,
    _NoTimeoutRetry,
    _PoolManagerAdapter,
    _supplied_pool_manager,
)
from smritea.exceptions import SmriteaError, SmriteaNotFoundError

_SHORT_TIMEOUT = 0.5
_TOLERANCE = 1.0
_PARTIAL_HEAD = b'HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 100\r\n\r\n'
_PARTIAL_BODY = b'{"code":"X'  # 10 of the 100 bytes announced by Content-Length
_NOT_FOUND_BODY = b'{"code":"MEMORY_NOT_FOUND","message":"Memory not found"}'


@dataclass(frozen=True)
class _Action:
    """What the loopback server does with one request."""

    kind: str  # 'silent', 'partial_body' or 'respond'
    status: int = 200
    body: bytes = b'{}'
    delay: float = 0.0
    headers: tuple[tuple[str, str], ...] = ()


_SILENT = _Action('silent')
_PARTIAL_BODY_ACTION = _Action('partial_body')


class _LoopbackServer:
    """A real TCP server on 127.0.0.1 that applies a scripted action to each request."""

    def __init__(self, script: Callable[[int], _Action]) -> None:
        self._script = script
        self._stop = threading.Event()
        self._lock = threading.Lock()
        self._connections: list[socket.socket] = []
        self.accepted = 0
        self.requests = 0
        self._listener = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self._listener.bind(('127.0.0.1', 0))
        self._listener.listen(16)
        self.url = f'http://127.0.0.1:{self._listener.getsockname()[1]}'
        threading.Thread(target=self._accept_loop, daemon=True).start()

    def close(self) -> None:
        self._stop.set()
        self._listener.close()
        with self._lock:
            for conn in self._connections:
                conn.close()

    def _accept_loop(self) -> None:
        while not self._stop.is_set():
            try:
                conn, _ = self._listener.accept()
            except OSError:
                return
            with self._lock:
                self._connections.append(conn)
                self.accepted += 1
            threading.Thread(target=self._serve, args=(conn,), daemon=True).start()

    def _serve(self, conn: socket.socket) -> None:
        try:
            while not self._stop.is_set():
                if not self._read_request(conn):
                    return
                with self._lock:
                    index = self.requests
                    self.requests += 1
                if not self._apply(conn, self._script(index)):
                    return
        except OSError:
            return

    @staticmethod
    def _read_request(conn: socket.socket) -> bool:
        data = b''
        while b'\r\n\r\n' not in data:
            chunk = conn.recv(4096)
            if not chunk:
                return False
            data += chunk
        head, _, rest = data.partition(b'\r\n\r\n')
        length = 0
        for line in head.split(b'\r\n')[1:]:
            name, _, value = line.partition(b':')
            if name.strip().lower() == b'content-length':
                length = int(value.strip())
        while len(rest) < length:
            chunk = conn.recv(4096)
            if not chunk:
                return False
            rest += chunk
        return True

    def _apply(self, conn: socket.socket, action: _Action) -> bool:
        """Apply the action; return False when the connection must not serve another request."""
        if action.kind == 'silent':
            self._stop.wait()
            return False
        if action.kind == 'partial_body':
            conn.sendall(_PARTIAL_HEAD + _PARTIAL_BODY)
            self._stop.wait()
            return False
        if self._stop.wait(action.delay):
            return False
        extra = ''.join(f'{name}: {value}\r\n' for name, value in action.headers)
        head = (
            f'HTTP/1.1 {action.status} X\r\nContent-Type: application/json\r\n{extra}'
            f'Content-Length: {len(action.body)}\r\n\r\n'
        )
        conn.sendall(head.encode() + action.body)
        return True


@pytest.fixture
def make_server():
    """Start loopback servers; every server started is closed at teardown."""
    servers: list[_LoopbackServer] = []

    def _make(script: Callable[[int], _Action]) -> _LoopbackServer:
        server = _LoopbackServer(script)
        servers.append(server)
        return server

    yield _make
    for server in servers:
        server.close()


class RecordingPoolManager(urllib3.PoolManager):
    """A PoolManager that records the keyword arguments of each request, with no network call."""

    def __init__(self, **kwargs) -> None:
        super().__init__(**kwargs)
        self.kwargs: list[dict] = []

    def request(self, method, url, **kwargs):
        self.kwargs.append(kwargs)
        return HTTPResponse(
            body=_NOT_FOUND_BODY,
            status=404,
            headers={'Content-Type': 'application/json'},
            reason='Not Found',
            preload_content=True,
        )


_OPERATIONS = [
    ('add', lambda c: c.add('content')),
    ('search', lambda c: c.search('query')),
    ('get', lambda c: c.get('mem_1')),
    ('delete', lambda c: c.delete('mem_1')),
]
_STALLS = [('silent server', _SILENT), ('stalled body', _PARTIAL_BODY_ACTION)]


def _client(base_url: str, **kwargs) -> SmriteaClient:
    return SmriteaClient(api_key='test-key', app_id='app-123', base_url=base_url, **kwargs)


def _use_short_timeout(client: SmriteaClient) -> None:
    """Rebuild the SDK default adapter around the generated pool manager, with a short timeout."""
    rest_client = client._api_client.rest_client
    rest_client.pool_manager = _default_pool_manager(
        rest_client.pool_manager.pool_manager, _SHORT_TIMEOUT
    )


def _elapsed_of_failure(operation: Callable[[SmriteaClient], object], client: SmriteaClient):
    started = time.monotonic()
    with pytest.raises(SmriteaError) as excinfo:
        operation(client)
    return time.monotonic() - started, excinfo.value


@pytest.mark.parametrize(('stall_name', 'stall'), _STALLS)
@pytest.mark.parametrize(('op_name', 'operation'), _OPERATIONS)
def test_default_client_fails_within_timeout(make_server, op_name, operation, stall_name, stall):
    """The SDK-built client gives up within the timeout and urllib3 sends the request only once."""
    server = make_server(lambda _: stall)
    client = _client(server.url, max_retries=0)
    _use_short_timeout(client)

    elapsed, error = _elapsed_of_failure(operation, client)

    case = f'{op_name} / {stall_name}'
    assert _SHORT_TIMEOUT * 0.8 <= elapsed < _SHORT_TIMEOUT + _TOLERANCE, case
    assert 'timed out' in str(error).lower(), f'{case}: {error}'
    assert isinstance(error.__cause__, urllib3.exceptions.TimeoutError), case
    assert server.requests == 1, case
    assert server.accepted == 1, case


@pytest.mark.parametrize(('stall_name', 'stall'), _STALLS)
@pytest.mark.parametrize(('op_name', 'operation'), _OPERATIONS)
def test_supplied_pool_manager_timeout_is_honoured(
    make_server, op_name, operation, stall_name, stall
):
    """The timeout of a PoolManager given as http_client takes effect; the SDK adds none."""
    server = make_server(lambda _: stall)
    pool = urllib3.PoolManager(timeout=urllib3.Timeout(total=_SHORT_TIMEOUT), retries=False)
    client = _client(server.url, max_retries=0, http_client=pool)

    elapsed, error = _elapsed_of_failure(operation, client)

    case = f'{op_name} / {stall_name}'
    assert _SHORT_TIMEOUT * 0.8 <= elapsed < _SHORT_TIMEOUT + _TOLERANCE, case
    assert 'timed out' in str(error).lower(), f'{case}: {error}'
    assert server.requests == 1, case


def test_default_client_retry_on_429_gets_a_fresh_timeout(make_server):
    """Each HTTP 429 retry is a new exchange: two slow exchanges may exceed one timeout together."""
    exchange_delay = _SHORT_TIMEOUT * 0.6  # one exchange fits the budget, two exchanges do not
    server = make_server(
        lambda index: _Action(
            'respond',
            status=429 if index == 0 else 404,
            body=_NOT_FOUND_BODY,
            delay=exchange_delay,
            headers=(('Retry-After', '1'),) if index == 0 else (),
        )
    )
    client = _client(server.url, max_retries=1)
    _use_short_timeout(client)

    started = time.monotonic()
    with patch('smritea.client.time.sleep') as sleep, pytest.raises(SmriteaNotFoundError):
        client.get('mem_1')
    elapsed = time.monotonic() - started

    assert server.requests == 2
    assert elapsed > _SHORT_TIMEOUT
    sleep.assert_called_once()


def test_public_constructor_wires_the_default_timeout():
    """Without http_client the SDK wraps its own pool manager with the 10 s default timeout."""
    client = _client('http://127.0.0.1:9')

    adapter = client._api_client.rest_client.pool_manager

    assert DEFAULT_TIMEOUT_SECONDS == 10.0
    assert smritea.client.DEFAULT_TIMEOUT_SECONDS == DEFAULT_TIMEOUT_SECONDS
    assert isinstance(adapter, _PoolManagerAdapter)
    assert adapter.default_timeout.total == DEFAULT_TIMEOUT_SECONDS
    assert isinstance(adapter.default_retries, _NoTimeoutRetry)
    assert type(adapter.pool_manager) is urllib3.PoolManager


def test_supplied_client_without_timeout_gets_no_sdk_timeout():
    """A PoolManager with no timeout is used as given: the SDK passes no timeout or retries."""
    pool = RecordingPoolManager()
    client = _client('http://127.0.0.1:9', max_retries=0, http_client=pool)

    with pytest.raises(SmriteaNotFoundError):
        client.get('mem_1')

    assert len(pool.kwargs) == 1
    assert 'timeout' not in pool.kwargs[0]
    assert 'retries' not in pool.kwargs[0]


def test_default_client_passes_the_default_timeout_with_each_request(monkeypatch):
    """The SDK-built client passes its total timeout and its retry policy to every request."""
    client = _client('http://127.0.0.1:9', max_retries=0)
    adapter = client._api_client.rest_client.pool_manager
    seen: list[dict] = []

    def _record(method, url, **kwargs):
        seen.append(kwargs)
        return HTTPResponse(
            body=_NOT_FOUND_BODY,
            status=404,
            headers={'Content-Type': 'application/json'},
            reason='Not Found',
            preload_content=True,
        )

    monkeypatch.setattr(adapter.pool_manager, 'request', _record)

    with pytest.raises(SmriteaNotFoundError):
        client.get('mem_1')

    assert len(seen) == 1
    assert seen[0]['timeout'].total == DEFAULT_TIMEOUT_SECONDS
    assert isinstance(seen[0]['retries'], _NoTimeoutRetry)


_EXPLICIT_TIMEOUT = urllib3.Timeout(total=3)
_EXPLICIT_RETRIES = urllib3.Retry(total=1)


@pytest.mark.parametrize(
    ('name', 'default_timeout', 'default_retries', 'passed', 'want_timeout', 'want_retries'),
    [
        ('timeout None is replaced by the default', 0.5, None, {'timeout': None}, 0.5, None),
        ('absent timeout gets the default', 0.5, None, {}, 0.5, None),
        ('explicit timeout is kept', 0.5, None, {'timeout': _EXPLICIT_TIMEOUT}, 3, None),
        ('timeout None is dropped without default', None, None, {'timeout': None}, None, None),
        ('absent timeout stays absent', None, None, {}, None, None),
        ('explicit timeout kept, no default', None, None, {'timeout': _EXPLICIT_TIMEOUT}, 3, None),
        ('default retries are added', None, _EXPLICIT_RETRIES, {}, None, _EXPLICIT_RETRIES),
        (
            'explicit retries are kept',
            None,
            urllib3.Retry(total=7),
            {'retries': _EXPLICIT_RETRIES},
            None,
            _EXPLICIT_RETRIES,
        ),
    ],
)
def test_adapter_resolves_timeout_and_retries(
    name, default_timeout, default_retries, passed, want_timeout, want_retries
):
    """The adapter replaces or drops timeout=None and fills retries only when the caller has none."""
    pool = RecordingPoolManager()
    timeout = None if default_timeout is None else urllib3.Timeout(total=default_timeout)
    adapter = _PoolManagerAdapter(pool, timeout, default_retries)

    adapter.request('GET', 'http://127.0.0.1:9/x', headers={'A': 'b'}, **passed)

    sent = pool.kwargs[0]
    assert sent['headers'] == {'A': 'b'}, name
    if want_timeout is None:
        assert 'timeout' not in sent, name
    else:
        assert sent['timeout'].total == want_timeout, name
    if want_retries is None:
        assert 'retries' not in sent, name
    else:
        assert sent['retries'] is want_retries, name


def test_adapter_delegates_other_attributes_to_the_wrapped_pool_manager():
    """Anything the adapter does not define comes from the wrapped pool manager."""
    pool = urllib3.PoolManager(timeout=urllib3.Timeout(total=2))
    adapter = _supplied_pool_manager(pool)

    assert adapter.connection_pool_kw is pool.connection_pool_kw
    assert adapter.clear == pool.clear
    with pytest.raises(AttributeError):
        _ = adapter.no_such_attribute


@pytest.mark.parametrize(
    ('name', 'method', 'error', 'want_raised'),
    [
        ('read timeout of GET', 'GET', ReadTimeoutError(None, '/x', 'read'), True),
        ('read timeout of DELETE', 'DELETE', ReadTimeoutError(None, '/x', 'read'), True),
        ('read timeout of POST', 'POST', ReadTimeoutError(None, '/x', 'read'), True),
        ('connect timeout of POST', 'POST', ConnectTimeoutError(None, 'connect'), True),
        ('connection reset of GET', 'GET', ProtocolError('reset'), False),
    ],
)
def test_no_timeout_retry_policy(name, method, error, want_raised):
    """A timeout is never retried; every other failure keeps the urllib3 default policy."""
    retry = _NoTimeoutRetry(total=3)

    if want_raised:
        with pytest.raises(type(error)):
            retry.increment(method, '/x', error=error)
    else:
        next_retry = retry.increment(method, '/x', error=error)
        assert isinstance(next_retry, _NoTimeoutRetry), name
        assert next_retry.total == 2, name
