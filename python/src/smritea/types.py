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
    """Scope fields for actor and conversation context.

    All fields are optional. Supply only the fields relevant to your operation:

    - **add**: set ``actor_id`` + ``actor_type`` to attribute the memory to a specific
      actor, and ``conversation_id`` to link it to a conversation thread.
    - **search**: set ``actor_id`` + ``actor_type`` to restrict results to a single actor,
      or ``participant_ids`` to find memories from conversations where **all** listed actors
      participated (AND semantics — minimum 2 IDs required).
    """

    actor_id: str | None = None
    """Identifier for the actor (user, agent, or system) associated with this memory.
    Must be set together with ``actor_type``; max 64 characters."""

    actor_type: str | None = None
    """Role of the actor. Accepted values: ``"user"``, ``"agent"``, ``"system"``.
    Must be set together with ``actor_id``."""

    actor_name: str | None = None
    """Display name of the actor. Optional — used for human-readable labels; max 255 characters."""

    conversation_id: str | None = None
    """Conversation thread this memory belongs to or should be searched within; max 64 characters.
    Mutually exclusive with ``participant_ids``; if both are set, ``conversation_id`` takes
    precedence."""

    source_type: str | None = None
    """Origin of the memory. Accepted values: ``"conversation"``, ``"document"``, ``"api"``.
    Defaults to ``"api"`` when omitted."""

    participant_ids: list[str] | None = None
    """Search across conversations where **all** listed actors participated (AND semantics).
    The service expands this list into the matching conversation IDs before querying.
    Requires at least 2 IDs; each ID must be 1–64 characters.
    Mutually exclusive with ``conversation_id``; if both are set, ``conversation_id`` wins.
    Only relevant for ``search`` — ignored on ``add``."""


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
