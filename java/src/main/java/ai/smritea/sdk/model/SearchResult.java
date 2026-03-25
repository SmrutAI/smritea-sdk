package ai.smritea.sdk.model;

import ai.smritea.sdk._internal.autogen.model.MemorySearchMemoryResponse;
import ai.smritea.sdk._internal.autogen.model.MemorySearchMemoryResult;

/**
 * A single search result containing a memory and its relevance score. Delegates to the
 * auto-generated types directly, matching the Python/TypeScript/Go SDKs which re-export autogen
 * types as-is.
 *
 * <p>The memory field is a {@link MemorySearchMemoryResult} — the lean search-specific type that
 * excludes internal/operational fields (appId, createdAt, updatedAt, conversationMessageId). This
 * is distinct from {@link Memory} which wraps the full CRUD response type returned by get/add.
 */
public final class SearchResult {
  private final MemorySearchMemoryResult memory;
  private final Double score;

  /**
   * Creates a SearchResult from the auto-generated response type. Called by SmriteaClient after
   * deserializing the server response. External callers should not use this constructor directly
   * (the autogen type is an internal implementation detail).
   *
   * @param inner the autogen response object
   */
  public SearchResult(MemorySearchMemoryResponse inner) {
    this.memory = inner.getMemory();
    this.score = inner.getScore() != null ? inner.getScore().doubleValue() : null;
  }

  /**
   * Creates a new SearchResult from an autogen memory and score. Primarily useful for tests.
   *
   * @param memory the matched search memory result
   * @param score the relevance score
   */
  public SearchResult(MemorySearchMemoryResult memory, Double score) {
    this.memory = memory;
    this.score = score;
  }

  /** Returns the matched memory (lean search-specific type). */
  public MemorySearchMemoryResult getMemory() {
    return memory;
  }

  /** Returns the relevance score. */
  public Double getScore() {
    return score;
  }

  /** Convenience delegate: returns the memory content. */
  public String getContent() {
    return memory != null ? memory.getContent() : null;
  }

  /** Convenience delegate: returns the memory ID. */
  public String getId() {
    return memory != null ? memory.getId() : null;
  }
}
