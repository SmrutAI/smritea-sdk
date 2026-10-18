"""Unit tests for the X-Trace-ID client option and request_id extraction on mapped errors."""

from __future__ import annotations

import json

import pytest

from smritea import SmriteaClient
from smritea.exceptions import (
    SmriteaBadRequestError,
    SmriteaConflictError,
    SmriteaError,
    SmriteaForbiddenError,
    SmriteaNotFoundError,
    SmriteaPaymentRequiredError,
    SmriteaTooManyRequestsError,
    SmriteaUnauthorizedError,
    SmriteaUnprocessableError,
    raise_for_status,
)
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
# Shared tables
# ==============================================================================

ERROR_STATUS_TABLE = [
    pytest.param(400, SmriteaBadRequestError, id='400_bad_request'),
    pytest.param(401, SmriteaUnauthorizedError, id='401_unauthorized'),
    pytest.param(402, SmriteaPaymentRequiredError, id='402_payment_required'),
    pytest.param(403, SmriteaForbiddenError, id='403_forbidden'),
    pytest.param(404, SmriteaNotFoundError, id='404_not_found'),
    pytest.param(409, SmriteaConflictError, id='409_conflict'),
    pytest.param(422, SmriteaUnprocessableError, id='422_unprocessable'),
    pytest.param(429, SmriteaTooManyRequestsError, id='429_too_many_requests'),
    pytest.param(500, SmriteaError, id='500_generic'),
]

RESPONSE_HEADER_NAME_TABLE = [
    pytest.param('X-Request-ID', id='canonical_casing'),
    pytest.param('x-request-id', id='lowercase'),
]


def _api_exception(status: int, body: str, headers: dict[str, str]) -> ApiException:
    """Build the autogen ApiException the same way test_client.py does."""
    exc = ApiException(status=status)
    exc.body = body
    exc.headers = headers
    return exc


# ==============================================================================
# trace_id option
# ==============================================================================

VALID_TRACE_IDS = [
    pytest.param('abcdefgh', id='min_8_chars'),
    pytest.param('a' * 100, id='max_100_chars'),
    pytest.param('a.b_c:d-e', id='all_allowed_punctuation'),
]

INVALID_TRACE_IDS = [
    pytest.param('a' * 7, id='below_min_7_chars'),
    pytest.param('a' * 101, id='above_max_101_chars'),
    pytest.param('abcd efgh', id='contains_space'),
    pytest.param('abcd/efgh', id='contains_slash'),
    pytest.param('', id='empty_string'),
    pytest.param('abcdefgh\n', id='trailing_newline'),
    pytest.param('abcdefgh\x00', id='trailing_nul'),
    pytest.param('abcdefgé', id='non_ascii_letter'),
]


class TestTraceIdOption:
    """Test the trace_id constructor option and the X-Trace-ID default header."""

    @pytest.mark.parametrize('trace_id', VALID_TRACE_IDS)
    def test_valid_trace_id_sent_as_default_header(self, mock_api, trace_id):
        """A valid trace_id is set as the X-Trace-ID default header."""
        client = SmriteaClient(api_key='test-key', app_id='app-123', trace_id=trace_id)

        assert client._api_client.default_headers['X-Trace-ID'] == trace_id

    @pytest.mark.parametrize('trace_id', INVALID_TRACE_IDS)
    def test_invalid_trace_id_raises_value_error(self, mock_api, trace_id):
        """A trace_id outside 8-100 chars of the allowed set raises ValueError."""
        with pytest.raises(ValueError, match='trace_id must be 8-100 characters'):
            SmriteaClient(api_key='test-key', app_id='app-123', trace_id=trace_id)

    def test_no_trace_id_omits_header(self, mock_api):
        """Without trace_id, no X-Trace-ID default header is set."""
        client = SmriteaClient(api_key='test-key', app_id='app-123')

        assert 'X-Trace-ID' not in client._api_client.default_headers


# ==============================================================================
# request_id on mapped errors (client path)
# ==============================================================================


class TestRequestIdFromClientErrors:
    """Test request_id extraction for errors raised through SmriteaClient."""

    @pytest.mark.parametrize('status, expected_cls', ERROR_STATUS_TABLE)
    def test_request_id_from_body(self, client, mock_api, status, expected_cls):
        """Body request_id is used, and it wins over an X-Request-ID header."""
        client._max_retries = 0
        body = json.dumps(
            {'code': 'TEST_CODE', 'message': 'test message', 'request_id': 'req-body-1'}
        )
        mock_api.create_memory.side_effect = _api_exception(
            status, body, {'X-Request-ID': 'req-header-ignored'}
        )

        with pytest.raises(expected_cls) as exc_info:
            client.add('content')

        assert type(exc_info.value) is expected_cls
        assert exc_info.value.request_id == 'req-body-1'

    @pytest.mark.parametrize('header_name', RESPONSE_HEADER_NAME_TABLE)
    @pytest.mark.parametrize('status, expected_cls', ERROR_STATUS_TABLE)
    def test_request_id_from_header_when_body_missing(
        self, client, mock_api, status, expected_cls, header_name
    ):
        """Without body request_id, the X-Request-ID header is used (any header casing)."""
        client._max_retries = 0
        body = json.dumps({'code': 'TEST_CODE', 'message': 'test message'})
        mock_api.create_memory.side_effect = _api_exception(
            status, body, {header_name: 'req-header-1'}
        )

        with pytest.raises(expected_cls) as exc_info:
            client.add('content')

        assert type(exc_info.value) is expected_cls
        assert exc_info.value.request_id == 'req-header-1'

    @pytest.mark.parametrize('status, expected_cls', ERROR_STATUS_TABLE)
    def test_request_id_none_when_neither_present(self, client, mock_api, status, expected_cls):
        """With no body request_id and no X-Request-ID header, request_id is None."""
        client._max_retries = 0
        body = json.dumps({'code': 'TEST_CODE', 'message': 'test message'})
        mock_api.create_memory.side_effect = _api_exception(status, body, {})

        with pytest.raises(expected_cls) as exc_info:
            client.add('content')

        assert type(exc_info.value) is expected_cls
        assert exc_info.value.request_id is None


# ==============================================================================
# request_id on mapped errors (raise_for_status entry point)
# ==============================================================================

RAISE_FOR_STATUS_CASES = [
    pytest.param(
        {'request_id': 'req-body-1'},
        {'X-Request-ID': 'req-header-ignored'},
        'req-body-1',
        id='body_wins_over_header',
    ),
    pytest.param(
        {'message': 'test message'},
        {'X-Request-ID': 'req-header-1'},
        'req-header-1',
        id='canonical_header',
    ),
    pytest.param(
        {'message': 'test message'},
        {'x-request-id': 'req-header-1'},
        'req-header-1',
        id='lowercase_header',
    ),
    pytest.param({'message': 'test message'}, {}, None, id='neither'),
]


class TestRequestIdFromRaiseForStatus:
    """Test request_id extraction in raise_for_status, the status-to-exception entry point."""

    @pytest.mark.parametrize('body, headers, expected_request_id', RAISE_FOR_STATUS_CASES)
    @pytest.mark.parametrize('status, expected_cls', ERROR_STATUS_TABLE)
    def test_raise_for_status_request_id(
        self, status, expected_cls, body, headers, expected_request_id
    ):
        """raise_for_status maps the status and resolves request_id body-first, then header."""
        with pytest.raises(expected_cls) as exc_info:
            raise_for_status(status, 'test message', headers=headers, body=body)

        assert type(exc_info.value) is expected_cls
        assert exc_info.value.request_id == expected_request_id
