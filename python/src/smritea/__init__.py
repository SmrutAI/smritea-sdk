"""smritea — Python SDK for smritea AI memory system."""

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

__version__ = "0.1.0"
