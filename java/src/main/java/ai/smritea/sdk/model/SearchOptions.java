package ai.smritea.sdk.model;

/**
 * Builder-style options for searching memories.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * new SearchOptions()
 *     .withScope(MemoryScope.builder().actorId("alice").actorType("user").build())
 *     .withLimit(10);
 * }</pre>
 */
public final class SearchOptions {
  private MemoryScope scope;
  private Integer limit;
  private Float threshold;
  private Integer graphDepth;
  private String fromTime;
  private String toTime;
  private String validAt;

  /** Creates a new SearchOptions with all fields unset. */
  public SearchOptions() {}

  /**
   * Sets the memory scope (actor and conversation context).
   *
   * @param scope the memory scope
   * @return this instance for chaining
   */
  public SearchOptions withScope(MemoryScope scope) {
    this.scope = scope;
    return this;
  }

  /**
   * Sets the maximum number of results to return.
   *
   * @param limit the result limit
   * @return this instance for chaining
   */
  public SearchOptions withLimit(Integer limit) {
    this.limit = limit;
    return this;
  }

  /**
   * Sets the minimum relevance threshold for results.
   *
   * @param threshold the score threshold
   * @return this instance for chaining
   */
  public SearchOptions withThreshold(Float threshold) {
    this.threshold = threshold;
    return this;
  }

  /**
   * Sets the graph traversal depth for graph-aware search.
   *
   * @param graphDepth the graph depth
   * @return this instance for chaining
   */
  public SearchOptions withGraphDepth(Integer graphDepth) {
    this.graphDepth = graphDepth;
    return this;
  }

  /**
   * Sets the start of the time range filter (ISO 8601 format). Must be used together with toTime.
   *
   * @param fromTime the start of the time range (ISO 8601)
   * @return this instance for chaining
   */
  public SearchOptions withFromTime(String fromTime) {
    this.fromTime = fromTime;
    return this;
  }

  /**
   * Sets the end of the time range filter (ISO 8601 format). Must be used together with fromTime.
   *
   * @param toTime the end of the time range (ISO 8601)
   * @return this instance for chaining
   */
  public SearchOptions withToTime(String toTime) {
    this.toTime = toTime;
    return this;
  }

  /**
   * Sets a point-in-time filter (ISO 8601 format). Mutually exclusive with fromTime/toTime.
   *
   * @param validAt the point-in-time to filter by (ISO 8601)
   * @return this instance for chaining
   */
  public SearchOptions withValidAt(String validAt) {
    this.validAt = validAt;
    return this;
  }

  /** Returns the memory scope. */
  public MemoryScope getScope() {
    return scope;
  }

  /** Returns the result limit. */
  public Integer getLimit() {
    return limit;
  }

  /** Returns the score threshold. */
  public Float getThreshold() {
    return threshold;
  }

  /** Returns the graph depth. */
  public Integer getGraphDepth() {
    return graphDepth;
  }

  /** Returns the start of the time range filter. */
  public String getFromTime() {
    return fromTime;
  }

  /** Returns the end of the time range filter. */
  public String getToTime() {
    return toTime;
  }

  /** Returns the point-in-time filter. */
  public String getValidAt() {
    return validAt;
  }
}
