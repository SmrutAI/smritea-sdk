"""Unit tests for Retry-After parsing in raise_for_status()."""

from __future__ import annotations

import pytest

from smritea.exceptions import SmriteaTooManyRequestsError, raise_for_status


@pytest.mark.parametrize(
    ('name', 'headers', 'expected'),
    [
        ('canonical case', {'Retry-After': '7'}, 7),
        ('lowercase header name', {'retry-after': '7'}, 7),
        ('uppercase header name', {'RETRY-AFTER': '7'}, 7),
        ('non-integer value', {'Retry-After': 'soon'}, None),
    ],
)
def test_raise_for_status_429_parses_retry_after(name, headers, expected):
    """raise_for_status() matches the Retry-After header name case-insensitively and sets retry_after."""
    with pytest.raises(SmriteaTooManyRequestsError) as exc_info:
        raise_for_status(429, 'rate limited', headers=headers)

    assert exc_info.value.retry_after == expected, (
        f'{name}: headers={headers!r}, expected retry_after={expected!r}, '
        f'got {exc_info.value.retry_after!r}'
    )
