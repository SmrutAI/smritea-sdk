"""Exceptions for the smritea SDK."""

from __future__ import annotations

import contextlib


class SmriteaError(Exception):
    """Base exception for all smritea SDK errors."""

    def __init__(
        self, message: str, status_code: int | None = None, error_code: str | None = None
    ) -> None:
        self.message = message
        self.status_code = status_code
        self.error_code = error_code or "INTERNAL_ERROR"
        super().__init__(message)

    def __repr__(self) -> str:
        return (
            f"{self.__class__.__name__}(message={self.message!r}, "
            f"status_code={self.status_code!r}, error_code={self.error_code!r})"
        )


class SmriteaAuthError(SmriteaError):
    """Raised on HTTP 401 — invalid or missing API key."""


class SmriteaNotFoundError(SmriteaError):
    """Raised on HTTP 404 — memory not found."""


class SmriteaValidationError(SmriteaError):
    """Raised on HTTP 400 — request validation failed."""


class SmriteaQuotaError(SmriteaError):
    """Raised on HTTP 402 — quota exceeded for this organization."""


class SmriteaDeserializationError(SmriteaError):
    """Raised when the server returns a response that cannot be deserialized.

    This typically indicates an unexpected API response format or a server-side
    error that produced a malformed body.
    """


class SmriteaRateLimitError(SmriteaError):
    """Raised on HTTP 429 — rate limit exceeded after all retries are exhausted.

    Attributes:
        retry_after: Seconds the server requested before retrying, if provided.
            This is informational — the SDK already waited this long during retries.
    """

    def __init__(
        self,
        message: str,
        status_code: int | None = 429,
        retry_after: int | None = None,
        error_code: str | None = None,
    ) -> None:
        super().__init__(message, status_code, error_code)
        self.retry_after = retry_after


def raise_for_status(
    status_code: int, message: str, headers: dict | None = None, error_code: str | None = None
) -> None:
    """Raise the appropriate SmriteaError subclass for an HTTP error status code.

    Args:
        status_code: HTTP response status code.
        message: Error message from the response body.
        headers: Optional response headers (used to extract Retry-After for 429).
        error_code: Error code from the response body.

    Raises:
        SmriteaValidationError: On HTTP 400.
        SmriteaAuthError: On HTTP 401.
        SmriteaQuotaError: On HTTP 402.
        SmriteaNotFoundError: On HTTP 404.
        SmriteaRateLimitError: On HTTP 429.
        SmriteaError: On HTTP 5xx or any other error status.
    """
    if status_code == 400:
        raise SmriteaValidationError(message, status_code, error_code)
    if status_code == 401:
        raise SmriteaAuthError(message, status_code, error_code)
    if status_code == 402:
        raise SmriteaQuotaError(message, status_code, error_code)
    if status_code == 404:
        raise SmriteaNotFoundError(message, status_code, error_code)
    if status_code == 429:
        retry_after: int | None = None
        if headers:
            with contextlib.suppress(KeyError, ValueError, TypeError):
                retry_after = int(headers["Retry-After"])
        raise SmriteaRateLimitError(
            message, status_code, retry_after=retry_after, error_code=error_code
        )
    raise SmriteaError(message, status_code, error_code)
