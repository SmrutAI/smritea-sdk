"""Unit tests for SmriteaClient."""

from __future__ import annotations

import random
import time
from unittest.mock import MagicMock, patch

import pytest

from smritea import SmriteaClient
from smritea.exceptions import (
    SmriteaAuthError,
    SmriteaError,
    SmriteaNotFoundError,
    SmriteaQuotaError,
    SmriteaRateLimitError,
    SmriteaValidationError,
)
from smritea.types import MemoryScope
from smritea._internal.autogen.smritea_cloud_sdk.exceptions import ApiException


@pytest.fixture
def mock_api(mocker):
    """Mock SDKMemoryApi for testing."""
    mock_cls = mocker.patch('smritea.client.SDKMemoryApi')
    return mock_cls.return_value


@pytest.fixture
def client(mock_api):
    """Create a SmriteaClient with mocked API."""
    return SmriteaClient(api_key='test-key', app_id='app-123')


# ==============================================================================
# Test 1: actor scope for add()
# ==============================================================================


class TestAddActorScope:
    """Test actor_id/actor_type scope parameters for client.add()."""

    def test_add_with_actor_id_and_type_user(self, client, mock_api):
        """When actor_id='alice' and actor_type='user' are passed, scope is built correctly."""
        mock_memory = MagicMock()
        mock_api.create_memory.return_value = mock_memory

        result = client.add('test content', scope=MemoryScope(actor_id='alice', actor_type='user'))

        assert result is mock_memory
        mock_api.create_memory.assert_called_once()
        call_args = mock_api.create_memory.call_args[0][0]
        assert call_args.scope.actor_id == 'alice'
        assert call_args.scope.actor_type == 'user'
        assert call_args.content == 'test content'
        assert call_args.app_id == 'app-123'

    def test_add_with_explicit_actor_id_and_type(self, client, mock_api):
        """When actor_id and actor_type are passed, they pass through unchanged."""
        mock_memory = MagicMock()
        mock_api.create_memory.return_value = mock_memory

        result = client.add(
            'test content',
            scope=MemoryScope(actor_id='bot-1', actor_type='agent'),
        )

        assert result is mock_memory
        call_args = mock_api.create_memory.call_args[0][0]
        assert call_args.scope.actor_id == 'bot-1'
        assert call_args.scope.actor_type == 'agent'

    def test_add_with_all_parameters(self, client, mock_api):
        """Test add() with all optional parameters."""
        mock_memory = MagicMock()
        mock_api.create_memory.return_value = mock_memory

        result = client.add(
            'test content',
            scope=MemoryScope(
                actor_id='alice',
                actor_type='user',
                actor_name='Alice Smith',
                conversation_id='conv-123',
            ),
            metadata={'key': 'value'},
        )

        assert result is mock_memory
        call_args = mock_api.create_memory.call_args[0][0]
        assert call_args.scope.actor_id == 'alice'
        assert call_args.scope.actor_type == 'user'
        assert call_args.scope.actor_name == 'Alice Smith'
        assert call_args.metadata == {'key': 'value'}
        assert call_args.scope.conversation_id == 'conv-123'


# ==============================================================================
# Test 2: actor scope for search()
# ==============================================================================


class TestSearchActorScope:
    """Test actor_id/actor_type scope parameters for client.search()."""

    def test_search_with_actor_id_and_type_user(self, client, mock_api):
        """When actor_id='alice' and actor_type='user' are passed, scope is built correctly."""
        mock_response = MagicMock()
        mock_response.memories = []
        mock_api.search_memories.return_value = mock_response

        result = client.search('query', scope=MemoryScope(actor_id='alice', actor_type='user'))

        assert result == []
        mock_api.search_memories.assert_called_once()
        call_args = mock_api.search_memories.call_args[0][0]
        assert call_args.scope.actor_id == 'alice'
        assert call_args.scope.actor_type == 'user'
        assert call_args.query == 'query'

    def test_search_with_explicit_actor_id_and_type(self, client, mock_api):
        """When actor_id and actor_type are passed, they pass through unchanged."""
        mock_response = MagicMock()
        mock_response.memories = []
        mock_api.search_memories.return_value = mock_response

        result = client.search(
            'query',
            scope=MemoryScope(actor_id='bot-1', actor_type='agent'),
        )

        call_args = mock_api.search_memories.call_args[0][0]
        assert call_args.scope.actor_id == 'bot-1'
        assert call_args.scope.actor_type == 'agent'

    def test_search_with_all_parameters(self, client, mock_api):
        """Test search() with all optional parameters."""
        mock_response = MagicMock()
        mock_response.memories = []
        mock_api.search_memories.return_value = mock_response

        result = client.search(
            'query',
            scope=MemoryScope(
                actor_id='alice',
                actor_type='user',
                conversation_id='conv-123',
            ),
            limit=10,
            threshold=0.5,
            graph_depth=2,
        )

        call_args = mock_api.search_memories.call_args[0][0]
        assert call_args.scope.actor_id == 'alice'
        assert call_args.scope.actor_type == 'user'
        assert call_args.limit == 10
        assert call_args.threshold == 0.5
        assert call_args.graph_depth == 2
        assert call_args.scope.conversation_id == 'conv-123'

    def test_search_with_temporal_filters(self, client, mock_api):
        """Test search() with temporal filter parameters."""
        mock_response = MagicMock()
        mock_response.memories = []
        mock_api.search_memories.return_value = mock_response

        result = client.search(
            'query',
            scope=MemoryScope(actor_id='alice', actor_type='user'),
            from_time='2024-01-01T00:00:00Z',
            to_time='2024-12-31T23:59:59Z',
            valid_at='2024-06-15T12:00:00Z',
        )

        call_args = mock_api.search_memories.call_args[0][0]
        assert call_args.scope.actor_id == 'alice'
        assert call_args.scope.actor_type == 'user'
        assert call_args.from_time == '2024-01-01T00:00:00Z'
        assert call_args.to_time == '2024-12-31T23:59:59Z'
        assert call_args.valid_at == '2024-06-15T12:00:00Z'


# ==============================================================================
# Test 3: Error mapping from ApiException
# ==============================================================================


class TestErrorMapping:
    """Test mapping of HTTP status codes to typed exceptions."""

    def test_400_validation_error(self, client, mock_api):
        """HTTP 400 raises SmriteaValidationError."""
        exc = ApiException(status=400)
        exc.body = '{"code": "VALIDATION_ERROR", "message": "validation failed"}'
        exc.headers = {}
        mock_api.create_memory.side_effect = exc

        with pytest.raises(SmriteaValidationError) as exc_info:
            client.add('content')

        assert exc_info.value.status_code == 400
        assert exc_info.value.message == 'validation failed'
        assert exc_info.value.error_code == 'VALIDATION_ERROR'

    def test_401_auth_error(self, client, mock_api):
        """HTTP 401 raises SmriteaAuthError."""
        exc = ApiException(status=401)
        exc.body = 'unauthorized'
        exc.headers = {}
        mock_api.create_memory.side_effect = exc

        with pytest.raises(SmriteaAuthError) as exc_info:
            client.add('content')

        assert exc_info.value.status_code == 401

    def test_402_quota_error(self, client, mock_api):
        """HTTP 402 raises SmriteaQuotaError."""
        exc = ApiException(status=402)
        exc.body = 'quota exceeded'
        exc.headers = {}
        mock_api.create_memory.side_effect = exc

        with pytest.raises(SmriteaQuotaError) as exc_info:
            client.add('content')

        assert exc_info.value.status_code == 402

    def test_404_not_found_error(self, client, mock_api):
        """HTTP 404 raises SmriteaNotFoundError."""
        exc = ApiException(status=404)
        exc.body = 'memory not found'
        exc.headers = {}
        mock_api.get_memory.side_effect = exc

        with pytest.raises(SmriteaNotFoundError) as exc_info:
            client.get('nonexistent')

        assert exc_info.value.status_code == 404

    def test_429_rate_limit_error(self, client, mock_api):
        """HTTP 429 raises SmriteaRateLimitError without retries (max_retries=0)."""
        exc = ApiException(status=429)
        exc.body = 'rate limited'
        exc.headers = {}
        mock_api.create_memory.side_effect = exc

        client._max_retries = 0
        with pytest.raises(SmriteaRateLimitError) as exc_info:
            client.add('content')

        assert exc_info.value.status_code == 429
        assert exc_info.value.retry_after is None

    def test_429_with_retry_after_header(self, client, mock_api):
        """HTTP 429 with Retry-After header populates retry_after field."""
        exc = ApiException(status=429)
        exc.body = 'rate limited'
        exc.headers = {'Retry-After': '10'}
        mock_api.create_memory.side_effect = exc

        client._max_retries = 0
        with pytest.raises(SmriteaRateLimitError) as exc_info:
            client.add('content')

        assert exc_info.value.retry_after == 10

    def test_500_generic_error(self, client, mock_api):
        """HTTP 500 raises SmriteaError (base class)."""
        exc = ApiException(status=500)
        exc.body = 'internal server error'
        exc.headers = {}
        mock_api.create_memory.side_effect = exc

        client._max_retries = 0
        with pytest.raises(SmriteaError) as exc_info:
            client.add('content')

        assert exc_info.value.status_code == 500
        assert not isinstance(exc_info.value, SmriteaValidationError)

    def test_non_api_exception_wrapped(self, client, mock_api):
        """Non-ApiException errors are wrapped as SmriteaError."""
        mock_api.create_memory.side_effect = RuntimeError('network error')

        client._max_retries = 0
        with pytest.raises(SmriteaError) as exc_info:
            client.add('content')

        assert 'network error' in exc_info.value.message


# ==============================================================================
# Test 4: Retry logic
# ==============================================================================


class TestRetryLogic:
    """Test automatic retry on HTTP 429."""

    def test_429_twice_then_success(self, client, mock_api, mocker):
        """After two 429s, third attempt succeeds. Sleeps twice."""
        sleep_mock = mocker.patch('smritea.client.time.sleep')
        mock_memory = MagicMock()

        # First two calls return 429, third succeeds
        exc = ApiException(status=429)
        exc.body = 'rate limited'
        exc.headers = {}
        mock_api.create_memory.side_effect = [exc, exc, mock_memory]

        result = client.add('content')

        assert result is mock_memory
        assert mock_api.create_memory.call_count == 3
        assert sleep_mock.call_count == 2

    def test_max_retries_zero(self, client, mock_api, mocker):
        """With max_retries=0, no retries on 429. Raises immediately."""
        sleep_mock = mocker.patch('smritea.client.time.sleep')
        exc = ApiException(status=429)
        exc.body = 'rate limited'
        exc.headers = {}
        mock_api.create_memory.side_effect = exc

        client._max_retries = 0
        with pytest.raises(SmriteaRateLimitError):
            client.add('content')

        assert mock_api.create_memory.call_count == 1
        assert sleep_mock.call_count == 0

    def test_retries_exhausted(self, client, mock_api, mocker):
        """After max_retries exhausted, final exception is raised."""
        sleep_mock = mocker.patch('smritea.client.time.sleep')
        exc = ApiException(status=429)
        exc.body = 'rate limited'
        exc.headers = {}
        mock_api.create_memory.side_effect = exc

        client._max_retries = 2
        with pytest.raises(SmriteaRateLimitError):
            client.add('content')

        # max_retries=2 means: attempt 0, attempt 1, attempt 2 (all fail)
        assert mock_api.create_memory.call_count == 3
        assert sleep_mock.call_count == 2

    def test_non_429_not_retried(self, client, mock_api, mocker):
        """Non-429 errors are raised immediately without retry."""
        sleep_mock = mocker.patch('smritea.client.time.sleep')
        exc = ApiException(status=404)
        exc.body = 'not found'
        exc.headers = {}
        mock_api.get_memory.side_effect = exc

        client._max_retries = 2
        with pytest.raises(SmriteaNotFoundError):
            client.get('nonexistent')

        assert mock_api.get_memory.call_count == 1
        assert sleep_mock.call_count == 0


# ==============================================================================
# Test 5: _retry_delay calculation
# ==============================================================================


class TestRetryDelay:
    """Test _retry_delay() calculation logic."""

    def test_retry_delay_with_server_provided_90_percent(self):
        """With retry_after=10, returns 90% (9.0), not capped."""
        client = SmriteaClient(api_key='test', app_id='app')
        delay = client._retry_delay(0, retry_after=10)
        assert delay == 9.0

    def test_retry_delay_with_server_provided_capped_at_30(self):
        """With retry_after=40, 90% is 36.0, capped at 30.0."""
        client = SmriteaClient(api_key='test', app_id='app')
        delay = client._retry_delay(0, retry_after=40)
        assert delay == 30.0

    def test_retry_delay_with_zero_retry_after_uses_exponential(self):
        """With retry_after=0, falls back to exponential (not 0*0.9=0)."""
        client = SmriteaClient(api_key='test', app_id='app')
        delay = client._retry_delay(0, retry_after=0)
        # attempt=0: base = 1.0 * 2^0 = 1.0
        # jitter = 1.0 * (0.75 + 0.5 * random) = [0.75, 1.25]
        # capped at 30
        assert 0.75 <= delay <= 1.25

    def test_retry_delay_with_none_retry_after_uses_exponential(self):
        """With retry_after=None, uses exponential backoff with jitter."""
        client = SmriteaClient(api_key='test', app_id='app')
        delay = client._retry_delay(0, retry_after=None)
        # attempt=0: base = 1.0, jitter = [0.75, 1.25]
        assert 0.75 <= delay <= 1.25

    def test_retry_delay_exponential_backoff_attempt_1(self):
        """Attempt 1 with exponential: base = 2.0, jitter = [1.5, 2.5]."""
        client = SmriteaClient(api_key='test', app_id='app')
        delay = client._retry_delay(1, retry_after=None)
        # attempt=1: base = 1.0 * 2^1 = 2.0
        # jitter = 2.0 * (0.75 + 0.5 * random) = [1.5, 2.5]
        assert 1.5 <= delay <= 2.5

    def test_retry_delay_exponential_capped_at_30(self):
        """Large attempt values are capped at 30 seconds."""
        client = SmriteaClient(api_key='test', app_id='app')
        delay = client._retry_delay(10, retry_after=None)
        # attempt=10: base = 1.0 * 2^10 = 1024, capped at 30
        # jitter of 30 = [22.5, 30.0]
        assert 22.5 <= delay <= 30.0


# ==============================================================================
# Test 6: get_all raises NotImplementedError
# ==============================================================================


class TestGetAll:
    """Test get_all() raises NotImplementedError."""

    def test_get_all_not_implemented(self, client):
        """get_all() always raises NotImplementedError."""
        with pytest.raises(NotImplementedError) as exc_info:
            client.get_all()

        assert 'not yet available' in str(exc_info.value).lower()

    def test_get_all_with_parameters_not_implemented(self, client):
        """get_all() with parameters still raises NotImplementedError."""
        with pytest.raises(NotImplementedError):
            client.get_all(limit=10, offset=0)


# ==============================================================================
# Additional integration tests
# ==============================================================================


class TestRetryWithActualSleep:
    """Test retry with mocked time.sleep to verify sleep calls."""

    def test_retry_sleeps_with_exponential_backoff(self, client, mock_api, mocker):
        """Verify that sleep is called with approximately correct exponential values."""
        sleep_calls = []

        def capture_sleep(duration):
            sleep_calls.append(duration)

        mocker.patch('smritea.client.time.sleep', side_effect=capture_sleep)
        mock_memory = MagicMock()
        exc = ApiException(status=429)
        exc.body = 'rate limited'
        exc.headers = {}

        mock_api.create_memory.side_effect = [exc, exc, mock_memory]

        result = client.add('content')

        assert result is mock_memory
        assert len(sleep_calls) == 2
        # First sleep: base = 1.0, jitter range [0.75, 1.25]
        assert 0.75 <= sleep_calls[0] <= 1.25
        # Second sleep: base = 2.0, jitter range [1.5, 2.5]
        assert 1.5 <= sleep_calls[1] <= 2.5

    def test_retry_respects_retry_after_header(self, client, mock_api, mocker):
        """Verify that Retry-After header is respected (90% of value, capped at 30)."""
        sleep_calls = []

        def capture_sleep(duration):
            sleep_calls.append(duration)

        mocker.patch('smritea.client.time.sleep', side_effect=capture_sleep)
        mock_memory = MagicMock()

        exc1 = ApiException(status=429)
        exc1.body = 'rate limited'
        exc1.headers = {'Retry-After': '10'}

        exc2 = ApiException(status=429)
        exc2.body = 'rate limited'
        exc2.headers = {'Retry-After': '5'}

        mock_api.create_memory.side_effect = [exc1, exc2, mock_memory]

        result = client.add('content')

        assert result is mock_memory
        assert len(sleep_calls) == 2
        # 10 * 0.9 = 9.0
        assert sleep_calls[0] == 9.0
        # 5 * 0.9 = 4.5
        assert sleep_calls[1] == 4.5


class TestGetAndDelete:
    """Test get() and delete() methods."""

    def test_get_memory(self, client, mock_api):
        """get() calls get_memory with the ID."""
        mock_memory = MagicMock()
        mock_api.get_memory.return_value = mock_memory

        result = client.get('mem-123')

        assert result is mock_memory
        mock_api.get_memory.assert_called_once_with('mem-123')

    def test_delete_memory(self, client, mock_api):
        """delete() calls delete_memory with the ID."""
        mock_api.delete_memory.return_value = None

        result = client.delete('mem-123')

        assert result is None
        mock_api.delete_memory.assert_called_once_with('mem-123')

    def test_get_memory_not_found(self, client, mock_api):
        """get() with nonexistent ID raises SmriteaNotFoundError."""
        exc = ApiException(status=404)
        exc.body = 'not found'
        exc.headers = {}
        mock_api.get_memory.side_effect = exc

        client._max_retries = 0
        with pytest.raises(SmriteaNotFoundError):
            client.get('nonexistent')

    def test_delete_memory_not_found(self, client, mock_api):
        """delete() with nonexistent ID raises SmriteaNotFoundError."""
        exc = ApiException(status=404)
        exc.body = 'not found'
        exc.headers = {}
        mock_api.delete_memory.side_effect = exc

        client._max_retries = 0
        with pytest.raises(SmriteaNotFoundError):
            client.delete('nonexistent')


class TestSearchResponseHandling:
    """Test search() response handling."""

    def test_search_empty_response(self, client, mock_api):
        """search() with no results returns empty list."""
        mock_response = MagicMock()
        mock_response.memories = None
        mock_api.search_memories.return_value = mock_response

        result = client.search('query')

        assert result == []

    def test_search_with_results(self, client, mock_api):
        """search() returns list of SearchResult objects."""
        mock_result1 = MagicMock()
        mock_result2 = MagicMock()
        mock_response = MagicMock()
        mock_response.memories = [mock_result1, mock_result2]
        mock_api.search_memories.return_value = mock_response

        result = client.search('query')

        assert len(result) == 2
        assert result[0] is mock_result1
        assert result[1] is mock_result2


class TestClientInitialization:
    """Test SmriteaClient initialization."""

    def test_default_initialization(self, mock_api):
        """Client initializes with default base_url and max_retries."""
        client = SmriteaClient(api_key='test-key', app_id='app-123')

        assert client._app_id == 'app-123'
        assert client._max_retries == 2

    def test_custom_base_url(self, mock_api):
        """Client accepts custom base_url."""
        client = SmriteaClient(
            api_key='test-key',
            app_id='app-123',
            base_url='https://custom.api.com',
        )

        assert client._api_client.configuration.host == 'https://custom.api.com'

    def test_custom_max_retries(self, mock_api):
        """Client accepts custom max_retries."""
        client = SmriteaClient(
            api_key='test-key',
            app_id='app-123',
            max_retries=5,
        )

        assert client._max_retries == 5

    def test_zero_max_retries(self, mock_api):
        """Client accepts max_retries=0 to disable retries."""
        client = SmriteaClient(
            api_key='test-key',
            app_id='app-123',
            max_retries=0,
        )

        assert client._max_retries == 0


class TestParseRetryAfter:
    """Test _parse_retry_after() header parsing."""

    def test_parse_retry_after_valid(self):
        """_parse_retry_after() returns integer when header is valid."""
        client = SmriteaClient(api_key='test', app_id='app')
        exc = ApiException(status=429)
        exc.headers = {'Retry-After': '15'}

        result = client._parse_retry_after(exc)

        assert result == 15

    def test_parse_retry_after_no_headers(self):
        """_parse_retry_after() returns None when headers is None."""
        client = SmriteaClient(api_key='test', app_id='app')
        exc = ApiException(status=429)
        exc.headers = None

        result = client._parse_retry_after(exc)

        assert result is None

    def test_parse_retry_after_missing_header(self):
        """_parse_retry_after() returns None when Retry-After is missing."""
        client = SmriteaClient(api_key='test', app_id='app')
        exc = ApiException(status=429)
        exc.headers = {'Content-Type': 'application/json'}

        result = client._parse_retry_after(exc)

        assert result is None

    def test_parse_retry_after_invalid_value(self):
        """_parse_retry_after() returns None when header value is not an integer."""
        client = SmriteaClient(api_key='test', app_id='app')
        exc = ApiException(status=429)
        exc.headers = {'Retry-After': 'not-a-number'}

        result = client._parse_retry_after(exc)

        assert result is None
