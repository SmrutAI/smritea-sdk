"""Public-facing data types for the smritea SDK.

Return types (Memory, SearchResult) are the auto-generated pydantic models from
``smritea._internal`` — re-exported here as canonical SDK names so that callers
can ``from smritea import Memory`` without knowing about ``_internal``.

Input option types (AddMemoryOptions, SearchOptions) are new classes that do not
exist in the auto-gen SDK; they are defined here as pydantic models.
"""
from __future__ import annotations

from typing import Any

from pydantic import BaseModel

# ---------------------------------------------------------------------------
# Re-export auto-gen return types under canonical SDK names.
# These are the real pydantic model classes produced by the openapi-generator.
# Regenerated from smritea-cloud via: cd ../smritea-cloud && make generate-public-sdk
# ---------------------------------------------------------------------------

from smritea._internal.smritea_cloud_sdk.models import (
    MemoryMemoryResponse as Memory,
    MemorySearchMemoriesResponse,
    MemorySearchMemoryResponse as SearchResult,
)


# ---------------------------------------------------------------------------
# Input option types — new classes that don't exist in the auto-gen SDK.
# Defined as pydantic models for validation and IDE autocomplete.
# ---------------------------------------------------------------------------


class AddMemoryOptions(BaseModel):
    """Options for SmriteaClient.add().

    ``user_id`` is a convenience shorthand: sets ``actor_id`` and forces
    ``actor_type="user"``.  If both ``user_id`` and ``actor_id`` are provided,
    ``user_id`` takes precedence.
    """

    user_id: str | None = None
    actor_id: str | None = None
    actor_type: str = "user"
    actor_name: str | None = None
    metadata: dict[str, Any] | None = None
    conversation_id: str | None = None


class SearchOptions(BaseModel):
    """Options for SmriteaClient.search().

    ``user_id`` is a convenience shorthand: sets ``actor_id`` and forces
    ``actor_type="user"``.
    """

    user_id: str | None = None
    actor_id: str | None = None
    actor_type: str | None = None
    limit: int | None = None
    method: str | None = None
    threshold: float | None = None
    graph_depth: int | None = None
    conversation_id: str | None = None
