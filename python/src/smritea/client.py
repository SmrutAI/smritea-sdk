"""SmriteaClient — main entry point for the smritea Python SDK."""

from __future__ import annotations

import json
import random
import time
from typing import Any, Callable, NoReturn, TypeVar

from smritea._internal.autogen.smritea_cloud_sdk import ApiClient, Configuration
from smritea._internal.autogen.smritea_cloud_sdk.api.sdk_memory_api import SDKMemoryApi
from smritea._internal.autogen.smritea_cloud_sdk.exceptions import ApiException
from smritea._internal.autogen.smritea_cloud_sdk.models import (
    CommondtoMemoryScope,
    CommondtoRelativeStandingConfig,
    MemoryCreateMemoryRequest,
    MemorySearchMemoryRequest,
)
from smritea.exceptions import (
    SmriteaAuthError,
    SmriteaError,
    SmriteaNotFoundError,
    SmriteaQuotaError,
    SmriteaRateLimitError,
    SmriteaValidationError,
)
from smritea.types import Memory, MemoryCreationResult, MemoryScope, RelativeStanding, SearchResult

_T = TypeVar("_T")
_RETRY_CAP_SECONDS = 30.0


class SmriteaClient:
    """Python SDK client for smritea AI memory system.

    Example::

        client = SmriteaClient(api_key='sk-...', app_id='app_xyz')
        client.add('I love hiking', scope=MemoryScope(actor_id='alice', actor_type='user'))
        scope = MemoryScope(actor_id='alice', actor_type='user')
        results = client.search('outdoor activities', scope=scope)
    """

    def __init__(
        self,
        api_key: str,
        app_id: str,
        base_url: str = "https://api.smritea.ai",
        max_retries: int = 2,
    ) -> None:
        """Initialise the smritea client.

        Args:
            api_key: Your smritea API key (starts with sk-).
            app_id: The app ID to use for all memory operations.
            base_url: Override the default API base URL.
            max_retries: Automatic retries on HTTP 429. Uses Retry-After header
                when provided, otherwise exponential backoff with jitter (capped
                at 30 s). Set to 0 to disable auto-retry. Default: 2.
        """
        self._app_id = app_id
        self._max_retries = max_retries
        configuration = Configuration(host=base_url)
        configuration.api_key["ApiKeyAuth"] = api_key
        self._api_client = ApiClient(configuration)
        self._memory_api = SDKMemoryApi(self._api_client)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def add(
        self,
        content: str,
        *,
        scope: MemoryScope | None = None,
        metadata: dict[str, Any] | None = None,
        event_occurred_at: str | None = None,
        relative_standing: RelativeStanding | None = None,
    ) -> MemoryCreationResult:
        """Add a new memory.

        Args:
            content: The memory content to store.
            scope: Optional MemoryScope object for actor and conversation context.
            metadata: Optional string-to-string key-value metadata attached to the memory.
            event_occurred_at: ISO-8601 datetime string — when this content was created
                or occurred. Used by the extraction LLM to resolve relative temporal
                expressions like "last year" or "yesterday". Defaults to current time
                if omitted.
            relative_standing: Optional importance and temporal decay configuration.

        Returns:
            MemoryCreationResult containing all memories created from the extracted
            facts (result.memories), plus metadata: facts_extracted,
            skipped_count, updated_count.
        """
        if metadata is not None and not isinstance(metadata, dict):
            raise SmriteaValidationError(
                f"metadata must be a dictionary, got {type(metadata).__name__}", 400
            )

        # Build autogen scope from the MemoryScope object
        autogen_scope = None
        if scope is not None:
            autogen_scope = CommondtoMemoryScope(
                actor_id=scope.actor_id,
                actor_type=scope.actor_type,
                actor_name=scope.actor_name,
                conversation_id=scope.conversation_id,
                source_type=scope.source_type,
                participant_ids=scope.participant_ids,
            )

        # Build autogen relative_standing from SDK type
        autogen_relative_standing = None
        if relative_standing is not None:
            autogen_relative_standing = CommondtoRelativeStandingConfig(
                importance=relative_standing.importance,
                decay_factor=relative_standing.decay_factor,
                decay_function=relative_standing.decay_function,
            )

        request = MemoryCreateMemoryRequest(
            app_id=self._app_id,
            content=content,
            scope=autogen_scope,
            metadata=metadata,
            event_occurred_at=event_occurred_at,
            relative_standing=autogen_relative_standing,
        )
        return self._execute_with_retry(lambda: self._memory_api.create_memory(request))

    def search(
        self,
        query: str,
        *,
        scope: MemoryScope | None = None,
        limit: int | None = None,
        threshold: float | None = None,
        graph_depth: int | None = None,
        from_time: str | None = None,
        to_time: str | None = None,
        valid_at: str | None = None,
        method: str | None = None,
        reranker_type: str | None = None,
        metadata_filter: dict[str, Any] | None = None,
    ) -> list[SearchResult]:
        """Search for memories semantically.

        Args:
            query: Natural language search query.
            scope: Optional MemoryScope object for actor and conversation context.
            limit: Maximum number of results. None = use app default.
            threshold: Minimum relevance score filter (0.0–1.0).
            graph_depth: Graph traversal depth override.
            from_time: ISO-8601 datetime — only return memories created at or after this time.
            to_time: ISO-8601 datetime — only return memories created at or before this time.
            valid_at: ISO-8601 datetime — return memories valid at exactly this point in time.
            method: Search method override. Accepted values: ``"quick_search"``,
                ``"deep_search"``, ``"context_aware_search"``. Defaults to app config.
            reranker_type: Reranker override. Accepted values: ``"rrf_temporal"``,
                ``"rrf"``, ``"temporal"``, ``"node_distance"``, ``"mmr"``,
                ``"cross_encoder"``. Only applies to deep_search. Defaults to app config.
            metadata_filter: MongoDB-style operator DSL for filtering search results by
                their metadata. Supports ``$eq``, ``$ne``, ``$gt``, ``$gte``, ``$lt``,
                ``$lte``, ``$in``, ``$nin``, ``$and``, ``$or``, ``$not``,
                and wildcard ``"*"``. Values must be str, int, or float — booleans and
                nested objects are rejected by the server.
                Simple equality: ``{"department": "engineering"}``
                Range: ``{"level": {"$gte": 4}}``
                Logical: ``{"$and": [{"department": "eng"}, {"level": {"$gt": 3}}]}``
                Note: ``$contains`` is not supported and is rejected with HTTP 400.
                None = no metadata filtering.

        Returns:
            List of SearchResult objects ordered by relevance score.
        """
        # Build autogen scope from the MemoryScope object
        autogen_scope = None
        if scope is not None:
            autogen_scope = CommondtoMemoryScope(
                actor_id=scope.actor_id,
                actor_type=scope.actor_type,
                conversation_id=scope.conversation_id,
                participant_ids=scope.participant_ids,
            )

        request = MemorySearchMemoryRequest(
            app_id=self._app_id,
            query=query,
            scope=autogen_scope,
            limit=limit,
            threshold=threshold,
            graph_depth=graph_depth,
            from_time=from_time,
            to_time=to_time,
            valid_at=valid_at,
            method=method,
            reranker_type=reranker_type,
            metadata_filter=metadata_filter,
        )
        response = self._execute_with_retry(lambda: self._memory_api.search_memories(request))
        return list(response.memories or [])

    def get(self, memory_id: str) -> Memory:
        """Get a single memory by ID.

        Args:
            memory_id: The memory ID (e.g. 'mem_abc123').

        Returns:
            The Memory object.

        Raises:
            SmriteaNotFoundError: If no memory with this ID exists.
        """
        return self._execute_with_retry(lambda: self._memory_api.get_memory(memory_id))

    def delete(self, memory_id: str) -> None:
        """Delete a memory by ID.

        Args:
            memory_id: The memory ID to delete.

        Raises:
            SmriteaNotFoundError: If no memory with this ID exists.
        """
        self._execute_with_retry(lambda: self._memory_api.delete_memory(memory_id))

    def get_all(
        self,
        *,
        limit: int = 50,
        offset: int = 0,
    ) -> list[Memory]:
        """List all memories.

        Note: The list endpoint is not yet available in the SDK API.

        Raises:
            NotImplementedError: Always — endpoint not yet available.
        """
        raise NotImplementedError(
            "get_all() is not yet available. The list memories endpoint is pending "
            "dashboard testing. Use search() to find specific memories."
        )

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _execute_with_retry(self, fn: Callable[[], _T]) -> _T:
        """Execute fn, retrying on HTTP 429 up to max_retries times.

        On each 429 the SDK sleeps before retrying:
        - Uses the Retry-After header value when the server provides one.
        - Falls back to exponential backoff with ±25 % jitter otherwise.
        - Sleep duration is always capped at 30 seconds.

        After all retries are exhausted the final exception is re-raised as a
        typed SmriteaError subclass with retry_after populated if available.
        """
        for attempt in range(self._max_retries + 1):
            try:
                return fn()
            except ApiException as exc:
                if exc.status == 429 and attempt < self._max_retries:
                    time.sleep(self._retry_delay(attempt, self._parse_retry_after(exc)))
                    continue
                self._handle_error(exc)
            except Exception as exc:
                self._handle_error(exc)
        raise AssertionError("unreachable")  # _handle_error always raises

    def _retry_delay(self, attempt: int, retry_after: int | None) -> float:
        """Calculate seconds to sleep before the next retry attempt.

        Uses Retry-After if provided and positive. Otherwise uses exponential
        backoff starting at 1 s, doubling per attempt, with ±25 % jitter.
        Always capped at 30 seconds.
        """
        if retry_after is not None and retry_after > 0:
            # Sleep for 90% of the server-specified wait time. The 10% headroom
            # accounts for one-way network latency: by the time the request
            # arrives at the server the rate-limit window will have expired.
            return min(float(retry_after) * 0.9, _RETRY_CAP_SECONDS)
        base = min(1.0 * (2.0**attempt), _RETRY_CAP_SECONDS)
        jitter = base * (0.75 + 0.5 * random.random())
        return min(jitter, _RETRY_CAP_SECONDS)

    def _parse_retry_after(self, exc: ApiException) -> int | None:
        """Extract the Retry-After header value in seconds, or None."""
        if not exc.headers:
            return None
        try:
            return int(dict(exc.headers)["Retry-After"])
        except (KeyError, ValueError, TypeError):
            return None

    def _handle_error(self, exc: Exception) -> NoReturn:
        """Map auto-gen ApiException to typed SDK exceptions. Always raises."""
        if isinstance(exc, ApiException):
            status = exc.status
            message = "Unknown error"
            error_code = "INTERNAL_ERROR"
            try:
                body_dict = json.loads(exc.body) if isinstance(exc.body, (str, bytes)) else exc.body
                message = body_dict.get("message", "Unknown error")
                error_code = body_dict.get("code", "INTERNAL_ERROR")
            except (json.JSONDecodeError, TypeError, KeyError):
                message = str(exc.body) if exc.body else str(exc)
            if status == 400:
                raise SmriteaValidationError(message, status, error_code) from exc
            if status == 401:
                raise SmriteaAuthError(message, status, error_code) from exc
            if status == 402:
                raise SmriteaQuotaError(message, status, error_code) from exc
            if status == 404:
                raise SmriteaNotFoundError(message, status, error_code) from exc
            if status == 429:
                retry_after = self._parse_retry_after(exc)
                raise SmriteaRateLimitError(
                    message, status, retry_after=retry_after, error_code=error_code
                ) from exc
            raise SmriteaError(message, status, error_code) from exc
        raise SmriteaError(str(exc)) from exc
