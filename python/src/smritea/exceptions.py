"""Exceptions for the smritea SDK."""

from __future__ import annotations

import contextlib


class SmriteaError(Exception):
    """Base exception for all smritea SDK errors.

    Also raised directly for HTTP 5xx responses and any other unmapped status code.
    """

    def __init__(
        self,
        message: str,
        http_status: int | None = None,
        code: str | None = None,
        body: dict | None = None,
        retryable: bool = False,
    ) -> None:
        self.message = message
        self.http_status = http_status
        self.code = code or "INTERNAL_ERROR"
        self.body = body
        self.retryable = retryable
        super().__init__(message)

    def __repr__(self) -> str:
        return (
            f"{self.__class__.__name__}(message={self.message!r}, "
            f"http_status={self.http_status!r}, code={self.code!r}, "
            f"body={self.body!r}, retryable={self.retryable!r})"
        )


class SmriteaBadRequestError(SmriteaError):
    """Raised on HTTP 400 — request validation failed."""


class SmriteaUnauthorizedError(SmriteaError):
    """Raised on HTTP 401 — invalid or missing API key."""


class SmriteaPaymentRequiredError(SmriteaError):
    """Raised on HTTP 402 — quota exceeded for this organization."""


class SmriteaForbiddenError(SmriteaError):
    """Raised on HTTP 403 — the caller is not permitted to perform this action."""


class SmriteaNotFoundError(SmriteaError):
    """Raised on HTTP 404 — memory not found."""


class SmriteaConflictError(SmriteaError):
    """Raised on HTTP 409 — the request conflicts with the current state of the resource."""


class SmriteaUnprocessableError(SmriteaError):
    """Raised on HTTP 422 — the request was well-formed but semantically invalid."""


class SmriteaDeserializationError(SmriteaError):
    """Raised when the server returns a response that cannot be deserialized.

    This typically indicates an unexpected API response format or a server-side
    error that produced a malformed body.
    """


class SmriteaTooManyRequestsError(SmriteaError):
    """Raised on HTTP 429 — rate limit exceeded after all retries are exhausted.

    Attributes:
        retry_after: Seconds the server requested before retrying, if provided.
            This is informational — the SDK already waited this long during retries.
    """

    def __init__(
        self,
        message: str,
        http_status: int | None = 429,
        retry_after: int | None = None,
        code: str | None = None,
        body: dict | None = None,
        retryable: bool = True,
    ) -> None:
        super().__init__(message, http_status, code, body, retryable)
        self.retry_after = retry_after


def raise_for_status(
    status_code: int,
    message: str,
    headers: dict | None = None,
    error_code: str | None = None,
    body: dict | None = None,
    retryable: bool = False,
) -> None:
    """Raise the appropriate SmriteaError subclass for an HTTP error status code.

    Args:
        status_code: HTTP response status code.
        message: Error message from the response body.
        headers: Optional response headers (used to extract Retry-After for 429).
        error_code: Error code from the response body.
        body: Optional full response body as a dictionary.
        retryable: Whether the server marked this error as retryable.

    Raises:
        SmriteaBadRequestError: On HTTP 400.
        SmriteaUnauthorizedError: On HTTP 401.
        SmriteaPaymentRequiredError: On HTTP 402.
        SmriteaForbiddenError: On HTTP 403.
        SmriteaNotFoundError: On HTTP 404.
        SmriteaConflictError: On HTTP 409.
        SmriteaUnprocessableError: On HTTP 422.
        SmriteaTooManyRequestsError: On HTTP 429.
        SmriteaError: On HTTP 5xx or any other error status.
    """
    if status_code == 400:
        raise SmriteaBadRequestError(message, status_code, error_code, body, retryable)
    if status_code == 401:
        raise SmriteaUnauthorizedError(message, status_code, error_code, body, retryable)
    if status_code == 402:
        raise SmriteaPaymentRequiredError(message, status_code, error_code, body, retryable)
    if status_code == 403:
        raise SmriteaForbiddenError(message, status_code, error_code, body, retryable)
    if status_code == 404:
        raise SmriteaNotFoundError(message, status_code, error_code, body, retryable)
    if status_code == 409:
        raise SmriteaConflictError(message, status_code, error_code, body, retryable)
    if status_code == 422:
        raise SmriteaUnprocessableError(message, status_code, error_code, body, retryable)
    if status_code == 429:
        retry_after: int | None = None
        if headers:
            with contextlib.suppress(KeyError, ValueError, TypeError):
                retry_after = int(headers["Retry-After"])
        raise SmriteaTooManyRequestsError(
            message,
            status_code,
            retry_after=retry_after,
            code=error_code,
            body=body,
            retryable=True,
        )
    raise SmriteaError(message, status_code, error_code, body, retryable)
