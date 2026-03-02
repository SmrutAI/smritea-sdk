"""SmriteaClient — main entry point for the smritea Python SDK."""

from __future__ import annotations

import random
import time
from typing import Any, Callable, NoReturn, TypeVar

from smritea._internal.autogen.smritea_cloud_sdk import ApiClient, Configuration
from smritea._internal.autogen.smritea_cloud_sdk.api.sdk_memory_api import SDKMemoryApi
from smritea._internal.autogen.smritea_cloud_sdk.exceptions import ApiException
from smritea._internal.autogen.smritea_cloud_sdk.models import (
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
from smritea.types import Memory, SearchResult

_T = TypeVar("_T")
_RETRY_CAP_SECONDS = 30.0


class SmriteaClient:
    """Python SDK client for smritea AI memory system.

    Example::

        client = SmriteaClient(api_key='sk-...', app_id='app_xyz')
        client.add('I love hiking', user_id='alice')
        results = client.search('outdoor activities', user_id='alice')
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
        user_id: str | None = None,
        actor_id: str | None = None,
        actor_type: str = "user",
        actor_name: str | None = None,
        metadata: dict[str, Any] | None = None,
        conversation_id: str | None = None,
    ) -> Memory:
        """Add a new memory.

        Args:
            content: The memory content to store.
            user_id: Convenience shorthand — sets actor_id and actor_type='user'.
            actor_id: Explicit actor ID (used when user_id is not provided).
            actor_type: Actor type ('user', 'agent', 'system'). Default 'user'.
            actor_name: Optional display name for the actor.
            metadata: Optional key-value metadata.
            conversation_id: Optional conversation context.

        Returns:
            The created Memory object.
        """
        # user_id convenience: overrides actor_id and forces actor_type='user'
        if user_id is not None:
            actor_id = user_id
            actor_type = "user"

        request = MemoryCreateMemoryRequest(
            app_id=self._app_id,
            content=content,
            actor_id=actor_id,
            actor_type=actor_type,
            actor_name=actor_name,
            metadata=metadata,
            conversation_id=conversation_id,
        )
        return self._execute_with_retry(lambda: self._memory_api.create_memory(request))

    def search(
        self,
        query: str,
        *,
        user_id: str | None = None,
        actor_id: str | None = None,
        actor_type: str | None = None,
        limit: int | None = None,
        method: str | None = None,
        threshold: float | None = None,
        graph_depth: int | None = None,
        conversation_id: str | None = None,
    ) -> list[SearchResult]:
        """Search for memories semantically.

        Args:
            query: Natural language search query.
            user_id: Convenience shorthand — sets actor_id and actor_type='user'.
            actor_id: Explicit actor filter.
            actor_type: Actor type filter.
            limit: Maximum number of results. None = use app default.
            method: Search method (e.g. 'quick_search', 'deep_search').
            threshold: Minimum relevance score filter (0.0–1.0).
            graph_depth: Graph traversal depth override.
            conversation_id: Filter to a specific conversation.

        Returns:
            List of SearchResult objects ordered by relevance score.
        """
        if user_id is not None:
            actor_id = user_id
            actor_type = "user"

        request = MemorySearchMemoryRequest(
            app_id=self._app_id,
            query=query,
            actor_id=actor_id,
            actor_type=actor_type,
            limit=limit,
            method=method,
            threshold=threshold,
            graph_depth=graph_depth,
            conversation_id=conversation_id,
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
        user_id: str | None = None,
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
            return min(float(retry_after), _RETRY_CAP_SECONDS)
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
            body = str(exc.body) if exc.body else str(exc)
            if status == 400:
                raise SmriteaValidationError(body, status) from exc
            if status == 401:
                raise SmriteaAuthError(body, status) from exc
            if status == 402:
                raise SmriteaQuotaError(body, status) from exc
            if status == 404:
                raise SmriteaNotFoundError(body, status) from exc
            if status == 429:
                retry_after = self._parse_retry_after(exc)
                raise SmriteaRateLimitError(body, status, retry_after=retry_after) from exc
            raise SmriteaError(body, status) from exc
        raise SmriteaError(str(exc)) from exc
