"""smritea — Python SDK for smritea AI memory system."""

from smritea.client import SmriteaClient
from smritea.exceptions import (
    SmriteaAuthError,
    SmriteaDeserializationError,
    SmriteaError,
    SmriteaNotFoundError,
    SmriteaQuotaError,
    SmriteaRateLimitError,
    SmriteaValidationError,
)
from smritea.types import Memory, MemoryCreationResult, MemoryScope, SearchResult

__all__ = [
    "SmriteaClient",
    "Memory",
    "MemoryCreationResult",
    "MemoryScope",
    "SearchResult",
    "SmriteaError",
    "SmriteaAuthError",
    "SmriteaDeserializationError",
    "SmriteaNotFoundError",
    "SmriteaValidationError",
    "SmriteaQuotaError",
    "SmriteaRateLimitError",
]

__version__ = "0.1.0"
