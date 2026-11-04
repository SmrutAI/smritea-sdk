"""smritea — Python SDK for smritea AI memory system."""

from importlib.metadata import PackageNotFoundError as _PackageNotFoundError
from importlib.metadata import version as _distribution_version

from smritea.client import SmriteaClient
from smritea.exceptions import (
    SmriteaBadRequestError,
    SmriteaConflictError,
    SmriteaDeserializationError,
    SmriteaError,
    SmriteaForbiddenError,
    SmriteaNotFoundError,
    SmriteaPaymentRequiredError,
    SmriteaTooManyRequestsError,
    SmriteaUnauthorizedError,
    SmriteaUnprocessableError,
)
from smritea.types import Memory, MemoryCreationResult, MemoryScope, SearchResult

__all__ = [
    "SmriteaClient",
    "Memory",
    "MemoryCreationResult",
    "MemoryScope",
    "SearchResult",
    "SmriteaError",
    "SmriteaBadRequestError",
    "SmriteaUnauthorizedError",
    "SmriteaPaymentRequiredError",
    "SmriteaForbiddenError",
    "SmriteaDeserializationError",
    "SmriteaNotFoundError",
    "SmriteaConflictError",
    "SmriteaUnprocessableError",
    "SmriteaTooManyRequestsError",
]

try:
    __version__ = _distribution_version("smritea-sdk")
except _PackageNotFoundError:  # pragma: no cover - running from a source checkout
    __version__ = "0.0.0"
