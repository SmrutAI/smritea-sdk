package ai.smritea.sdk.model;

import ai.smritea.sdk._internal.autogen.model.MemorySearchMemoryResponse;

/** Wraps a memory with its search relevance score. Delegates to the auto-generated type. */
public final class SearchResult {
    private final Memory memory;
    private final Double score;

    /**
     * Creates a SearchResult from the auto-generated response type. Called by SmriteaClient after
     * deserializing the server response. External callers should not use this constructor directly
     * (the autogen type is an internal implementation detail).
     *
     * @param inner the autogen response object
     */
    public SearchResult(MemorySearchMemoryResponse inner) {
        this.memory = inner.getMemory() != null ? new Memory(inner.getMemory()) : null;
        this.score = inner.getScore() != null ? inner.getScore().doubleValue() : null;
    }

    /**
     * Creates a new SearchResult from an already-wrapped Memory and score. Primarily useful for
     * tests.
     *
     * @param memory the matched memory
     * @param score the relevance score
     */
    public SearchResult(Memory memory, Double score) {
        this.memory = memory;
        this.score = score;
    }

    /** Returns the matched memory. */
    public Memory getMemory() {
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
