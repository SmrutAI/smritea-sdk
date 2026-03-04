package ai.smritea.sdk.model;

/** Wraps a memory with its search relevance score. */
public final class SearchResult {
    private final Memory memory;
    private final Double score;

    /**
     * Creates a new SearchResult.
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
        return memory.getContent();
    }

    /** Convenience delegate: returns the memory ID. */
    public String getId() {
        return memory.getId();
    }
}
