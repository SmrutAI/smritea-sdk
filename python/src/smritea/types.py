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
    MemoryCreateMemoryResponse as MemoryCreationResult,
)
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
# MemoryCreationResult is the response from add(). Contains all memories
# created from the extracted facts (memories[]), plus extraction metadata:
# facts_extracted, extraction_confidence, skipped_count, updated_count.
# ---------------------------------------------------------------------------

__all__ = [
    "MemoryCreationResult",
    "Memory",
    "SearchResult",
    "MemorySearchMemoriesResponse",
    "MemoryScope",
]


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


class RelativeStanding(BaseModel):
    """Importance and temporal decay configuration for a memory.

    Controls how the memory's relevance score decays over time in search results.
    All fields are optional — omitted fields use server defaults
    (importance=1.0, decay_factor=0.2, decay_function="exponential").
    """

    importance: float | None = None
    """How important is this memory (0.0–1.0). Higher = ranks higher in search."""
    decay_factor: float | None = None
    """Rate of relevance decay over time (>=0). 0 = no decay (memory score is pinned
    permanently). 0.2 = light decay (default). 1.0 = standard. 3.0+ = aggressive."""
    decay_function: str | None = None
    """Decay curve shape. Accepted values: ``"exponential"``, ``"gaussian"``, ``"linear"``."""


class AddOptions(BaseModel):
    """Options for SmriteaClient.add()."""

    scope: MemoryScope | None = None
    metadata: dict[str, Any] | None = None
    event_occurred_at: str | None = None
    """ISO-8601 datetime string — when this content was created or occurred.
    Used by the extraction LLM to resolve relative temporal expressions
    like "last year" or "yesterday". Defaults to current time if omitted."""
    relative_standing: RelativeStanding | None = None
    """Importance and temporal decay configuration for this memory."""


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
    method: str | None = None
    """Search method override. Accepted values: ``"quick_search"``, ``"deep_search"``,
    ``"context_aware_search"``. Defaults to app config if omitted."""
    reranker_type: str | None = None
    """Reranker override. Accepted values: ``"rrf_temporal"``, ``"rrf"``, ``"temporal"``,
    ``"node_distance"``, ``"mmr"``, ``"cross_encoder"``. Only applies to deep_search.
    Defaults to app config if omitted."""
    metadata_filter: dict[str, Any] | None = None
    """MongoDB-style operator DSL for filtering search results by their metadata.
    Supports ``$eq``, ``$ne``, ``$gt``, ``$gte``, ``$lt``, ``$lte``, ``$in``, ``$nin``,
    ``$contains``, ``$and``, ``$or``, ``$not``, and wildcard ``"*"``.
    Values must be str, int, or float — booleans and nested objects are rejected.
    Simple equality: ``{"department": "engineering"}``
    Range: ``{"level": {"$gte": 4}}``
    Logical: ``{"$and": [{"department": "eng"}, {"level": {"$gt": 3}}]}``
    Note: ``$contains`` is applied as a post-filter and may return fewer results than
    ``limit``. ``$contains`` inside ``$or`` is rejected with HTTP 400.
    ``None`` = no metadata filtering."""
