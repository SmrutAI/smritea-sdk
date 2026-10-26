"""Unit tests of the http_client option of SmriteaClient."""

from __future__ import annotations

import pytest
import urllib3
from urllib3.response import HTTPResponse

from smritea import SmriteaClient
from smritea.exceptions import SmriteaNotFoundError

_BASE_URL = 'http://127.0.0.1:9'
_NOT_FOUND_BODY = b'{"code":"MEMORY_NOT_FOUND","message":"Memory not found"}'


class RecordingPoolManager(urllib3.PoolManager):
    """A PoolManager that records each request and answers 404 without a network call."""

    def __init__(self) -> None:
        super().__init__()
        self.calls: list[tuple[str, str, dict[str, str]]] = []

    def request(self, method, url, body=None, fields=None, headers=None, **kwargs):
        self.calls.append((method, url, dict(headers or {})))
        return HTTPResponse(
            body=_NOT_FOUND_BODY,
            status=404,
            headers={'Content-Type': 'application/json'},
            reason='Not Found',
            preload_content=True,
        )


@pytest.mark.parametrize(
    ('name', 'trace_id'),
    [
        ('custom client without trace id', None),
        ('custom client with trace id', 'trace-abc12345'),
    ],
)
def test_custom_pool_manager_sends_every_request(name, trace_id):
    """The PoolManager given as http_client sends the request with the SDK headers."""
    pool = RecordingPoolManager()
    client = SmriteaClient(
        api_key='test-key',
        app_id='app-123',
        base_url=_BASE_URL,
        max_retries=0,
        trace_id=trace_id,
        http_client=pool,
    )

    with pytest.raises(SmriteaNotFoundError):
        client.get('mem_1')

    assert client._api_client.rest_client.pool_manager is pool, name
    assert len(pool.calls) == 1, name
    method, url, headers = pool.calls[0]
    assert method == 'GET', name
    assert url == f'{_BASE_URL}/api/v1/sdk/memories/mem_1', name
    assert headers.get('X-API-Key') == 'test-key', name
    assert headers.get('X-Trace-ID') == trace_id, name


@pytest.mark.parametrize(
    ('name', 'extra'),
    [
        ('option absent', {}),
        ('option None', {'http_client': None}),
    ],
)
def test_without_option_the_sdk_builds_its_own_pool_manager(name, extra):
    """Without http_client the generated client keeps its own urllib3 PoolManager."""
    client = SmriteaClient(api_key='test-key', app_id='app-123', base_url=_BASE_URL, **extra)

    assert type(client._api_client.rest_client.pool_manager) is urllib3.PoolManager, name
