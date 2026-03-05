package ai.smritea.sdk.model;

/** Builder-style options for searching memories. */
public final class SearchOptions {
  private String userId;
  private String actorId;
  private String actorType;
  private Integer limit;
  private String method;
  private Float threshold;
  private Integer graphDepth;
  private String conversationId;

  /** Creates a new SearchOptions with all fields unset. */
  public SearchOptions() {}

  /**
   * Sets the user ID to scope the search.
   *
   * @param userId the user ID
   * @return this instance for chaining
   */
  public SearchOptions withUserId(String userId) {
    this.userId = userId;
    return this;
  }

  /**
   * Sets the actor ID to scope the search.
   *
   * @param actorId the actor ID
   * @return this instance for chaining
   */
  public SearchOptions withActorId(String actorId) {
    this.actorId = actorId;
    return this;
  }

  /**
   * Sets the actor type to scope the search.
   *
   * @param actorType the actor type
   * @return this instance for chaining
   */
  public SearchOptions withActorType(String actorType) {
    this.actorType = actorType;
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
   * Sets the search method (e.g. "hybrid", "semantic", "keyword").
   *
   * @param method the search method
   * @return this instance for chaining
   */
  public SearchOptions withMethod(String method) {
    this.method = method;
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
   * Sets the conversation ID to scope the search.
   *
   * @param conversationId the conversation ID
   * @return this instance for chaining
   */
  public SearchOptions withConversationId(String conversationId) {
    this.conversationId = conversationId;
    return this;
  }

  /** Returns the user ID. */
  public String getUserId() {
    return userId;
  }

  /** Returns the actor ID. */
  public String getActorId() {
    return actorId;
  }

  /** Returns the actor type. */
  public String getActorType() {
    return actorType;
  }

  /** Returns the result limit. */
  public Integer getLimit() {
    return limit;
  }

  /** Returns the search method. */
  public String getMethod() {
    return method;
  }

  /** Returns the score threshold. */
  public Float getThreshold() {
    return threshold;
  }

  /** Returns the graph depth. */
  public Integer getGraphDepth() {
    return graphDepth;
  }

  /** Returns the conversation ID. */
  public String getConversationId() {
    return conversationId;
  }
}
