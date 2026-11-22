"""Request timeout handling for the pool manager that sends every SDK request.

The generated REST layer passes ``timeout=None`` to ``PoolManager.request`` on every call. An
explicit ``None`` overrides the timeout of the pool manager itself, so without this adapter a
timeout configured on any pool manager would be ignored.
"""

from __future__ import annotations

from types import TracebackType
from typing import Any

import urllib3
from urllib3.exceptions import ConnectTimeoutError, ReadTimeoutError
from urllib3.response import BaseHTTPResponse
from urllib3.util.retry import Retry


class _NoTimeoutRetry(Retry):
    """A Retry that never retries a connect or read timeout.

    urllib3 retries a read timeout of an idempotent request (GET, DELETE), and a connect timeout
    of any request. Every retry starts a fresh timeout, so one call could take several times the
    timeout. Every other failure follows the urllib3 default policy.
    """

    def increment(
        self,
        method: str | None = None,
        url: str | None = None,
        response: BaseHTTPResponse | None = None,
        error: Exception | None = None,
        _pool: Any = None,
        _stacktrace: TracebackType | None = None,
    ) -> _NoTimeoutRetry:
        """Re-raise a timeout at once; delegate every other failure to urllib3."""
        if isinstance(error, (ConnectTimeoutError, ReadTimeoutError)):
            raise error.with_traceback(_stacktrace)
        return super().increment(method, url, response, error, _pool, _stacktrace)


class _PoolManagerAdapter:
    """Wraps a pool manager and decides which timeout applies when the caller gives none.

    ``request`` receives ``timeout=None`` from the generated REST layer. The adapter replaces it
    with the SDK default timeout, or, when it has none, drops it so the timeout of the wrapped
    pool manager applies. Every other attribute is delegated to the wrapped pool manager.
    """

    def __init__(
        self,
        pool_manager: urllib3.PoolManager,
        default_timeout: urllib3.Timeout | None,
        default_retries: Retry | None,
    ) -> None:
        self.pool_manager = pool_manager
        self.default_timeout = default_timeout
        self.default_retries = default_retries

    def request(self, method: str, url: str, **kwargs: Any) -> BaseHTTPResponse:
        """Send the request, resolving the timeout and retries described in the class docstring."""
        if kwargs.get("timeout") is None:
            kwargs.pop("timeout", None)
            if self.default_timeout is not None:
                kwargs["timeout"] = self.default_timeout
        if self.default_retries is not None:
            kwargs.setdefault("retries", self.default_retries)
        return self.pool_manager.request(method, url, **kwargs)

    def __getattr__(self, name: str) -> Any:
        """Delegate every other attribute to the wrapped pool manager."""
        if name == "pool_manager":  # attribute not set yet (for example during copy): stop here
            raise AttributeError(name)
        return getattr(self.pool_manager, name)


def _default_pool_manager(
    pool_manager: urllib3.PoolManager, timeout_seconds: float
) -> _PoolManagerAdapter:
    """Wrap the SDK-built pool manager so each request gets a total timeout and no timeout retry."""
    return _PoolManagerAdapter(
        pool_manager,
        urllib3.Timeout(total=timeout_seconds),
        _NoTimeoutRetry(total=Retry.DEFAULT.total),
    )


def _supplied_pool_manager(pool_manager: urllib3.PoolManager) -> _PoolManagerAdapter:
    """Wrap the caller's pool manager so its own timeout and retries apply, with none added."""
    return _PoolManagerAdapter(pool_manager, None, None)
