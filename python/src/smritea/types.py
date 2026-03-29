"""Public-facing data types for the smritea SDK.

Return types (Memory, SearchResult) are the auto-generated pydantic models from
``smritea._internal.autogen`` — re-exported here as canonical SDK names so that
callers can ``from smritea import Memory`` without knowing about ``_internal``.

Input option types (AddMemoryOptions, SearchOptions) are new classes that do not
exist in the auto-gen SDK; they are defined here as pydantic models.
"""

from __future__ import annotations

from typing import Any

from pydantic import BaseModel

from smritea._internal.autogen.smritea_cloud_sdk.models import (
    MemoryMemoryResponse as Memory,
)
from smritea._internal.autogen.smritea_cloud_sdk.models import (
    MemorySearchMemoriesResponse,
)
from smritea._internal.autogen.smritea_cloud_sdk.models import (
    MemorySearchMemoryResponse as SearchResult,
)

# ---------------------------------------------------------------------------
# Re-export auto-gen return types under canonical SDK names.
# These are the real pydantic model classes produced by the openapi-generator.
# Regenerated from smritea-cloud via: cd ../smritea-cloud && make generate-public-sdk
# ---------------------------------------------------------------------------

__all__ = ["Memory", "SearchResult", "MemorySearchMemoriesResponse", "MemoryScope"]


# ---------------------------------------------------------------------------
# Input option types — new classes that don't exist in the auto-gen SDK.
# Defined as pydantic models for validation and IDE autocomplete.
# ---------------------------------------------------------------------------


class MemoryScope(BaseModel):
    """Scope fields for actor and conversation context."""

    actor_id: str | None = None
    actor_type: str | None = None
    actor_name: str | None = None
    conversation_id: str | None = None
    conversation_message_id: str | None = None
    source_type: str | None = None


class AddOptions(BaseModel):
    """Options for SmriteaClient.add()."""

    scope: MemoryScope | None = None
    metadata: dict[str, Any] | None = None


class SearchOptions(BaseModel):
    """Options for SmriteaClient.search()."""

    scope: MemoryScope | None = None
    limit: int | None = None
    threshold: float | None = None
    graph_depth: int | None = None
    from_time: str | None = None
    """ISO-8601 datetime string — only return memories created at or after this time."""
    to_time: str | None = None
    """ISO-8601 datetime string — only return memories created at or before this time."""
    valid_at: str | None = None
    """ISO-8601 datetime string — return memories valid at exactly this point in time."""
