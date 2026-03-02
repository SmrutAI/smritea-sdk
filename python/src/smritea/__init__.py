"""smritea — Python SDK for smritea AI memory system."""

from smritea.client import SmriteaClient
from smritea.exceptions import (
    SmriteaAuthError,
    SmriteaError,
    SmriteaNotFoundError,
    SmriteaQuotaError,
    SmriteaRateLimitError,
    SmriteaValidationError,
)

from smritea.types import Memory, SearchResult

__all__ = [
    'SmriteaClient',
    'Memory',
    'SearchResult',
    'SmriteaError',
    'SmriteaAuthError',
    'SmriteaNotFoundError',
    'SmriteaValidationError',
    'SmriteaQuotaError',
    'SmriteaRateLimitError',
]

__version__ = '0.1.0'
