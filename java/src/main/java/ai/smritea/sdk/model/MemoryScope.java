package ai.smritea.sdk.model;

import ai.smritea.sdk._internal.autogen.model.CommondtoMemoryScope;

/**
 * Public-facing scope type representing the actor and conversation context of a memory. Delegates
 * to the auto-generated {@link CommondtoMemoryScope}.
 *
 * <p>Matches the nested {@code scope} JSON object returned by the API:
 *
 * <pre>{@code
 * {
 *   "scope": {
 *     "actor_id": "alice",
 *     "actor_type": "user",
 *     "actor_name": "Alice",
 *     "conversation_id": "conv-123",
 *     "source_type": "conversation"
 *   }
 * }
 * }</pre>
 *
 * <p>In production, instances are created internally by {@link Memory} and {@link SearchResult}
 * from deserialized autogen responses. For tests, use the {@link Builder}.
 */
public final class MemoryScope {
  private final CommondtoMemoryScope inner;

  /**
   * Participant IDs stored locally until autogen regeneration picks up {@code participant_ids} from
   * the updated OpenAPI spec.
   */
  private final java.util.List<String> participantIds;

  /**
   * Creates a MemoryScope from the auto-generated scope type. Called internally — external callers
   * should not use this constructor directly (the autogen type is an internal implementation
   * detail).
   *
   * @param inner the autogen scope object
   */
  public MemoryScope(CommondtoMemoryScope inner) {
    this.inner = inner;
    this.participantIds = null;
  }

  private MemoryScope(CommondtoMemoryScope inner, java.util.List<String> participantIds) {
    this.inner = inner;
    this.participantIds = participantIds;
  }

  /** Returns a new builder for constructing MemoryScope instances in tests. */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the actor ID, or null if not set.
   *
   * <p>Max 64 characters. Must be paired with {@link #getActorType()}.
   */
  public String getActorId() {
    return inner.getActorId();
  }

  /**
   * Returns the actor type as a string ({@code "user"}, {@code "agent"}, or {@code "system"}), or
   * null if not set.
   *
   * <p>Must be paired with {@link #getActorId()}.
   */
  public String getActorType() {
    if (inner.getActorType() == null) {
      return null;
    }
    return inner.getActorType().getValue();
  }

  /**
   * Returns the human-readable display name of the actor, or null if not set.
   *
   * <p>Max 255 characters. Optional — used for labelling only.
   */
  public String getActorName() {
    return inner.getActorName();
  }

  /**
   * Returns the conversation ID, or null if not set.
   *
   * <p>Max 64 characters. Mutually exclusive with {@link #getParticipantIds()}; if both are set,
   * {@code conversationId} takes precedence.
   */
  public String getConversationId() {
    return inner.getConversationId();
  }

  /**
   * Returns the source type ({@code "conversation"}, {@code "document"}, or {@code "api"}), or null
   * if not set. Defaults to {@code "api"} on the server when omitted.
   */
  public String getSourceType() {
    if (inner.getSourceType() == null) {
      return null;
    }
    return inner.getSourceType().getValue();
  }

  /**
   * Returns the participant IDs for multi-actor search, or null if not set.
   *
   * <p>When set, the search service finds all conversations where <em>every</em> listed actor
   * participated (AND semantics) and searches memories within those conversations. Requires at
   * least 2 IDs; each ID must be 1–64 characters. Mutually exclusive with {@link
   * #getConversationId()}; if both are set, {@code conversationId} wins. Only relevant for search —
   * ignored on add.
   */
  public java.util.List<String> getParticipantIds() {
    return participantIds;
  }

  /** Builder for constructing {@link MemoryScope} instances in tests. */
  public static final class Builder {
    private final CommondtoMemoryScope delegate;

    private Builder() {
      delegate = new CommondtoMemoryScope();
    }

    private java.util.List<String> participantIds;

    /** Sets the actor ID. Max 64 characters; must be paired with {@code actorType}. */
    public Builder actorId(String actorId) {
      delegate.setActorId(actorId);
      return this;
    }

    /**
     * Sets the actor type. Accepted values: {@code "user"}, {@code "agent"}, {@code "system"}. Must
     * be paired with {@code actorId}.
     */
    public Builder actorType(String actorType) {
      delegate.setActorType(CommondtoMemoryScope.ActorTypeEnum.fromValue(actorType));
      return this;
    }

    /** Sets the human-readable display name of the actor. Max 255 characters. */
    public Builder actorName(String actorName) {
      delegate.setActorName(actorName);
      return this;
    }

    /**
     * Sets the conversation ID. Max 64 characters. Mutually exclusive with {@code participantIds};
     * if both are set, {@code conversationId} takes precedence.
     */
    public Builder conversationId(String conversationId) {
      delegate.setConversationId(conversationId);
      return this;
    }

    /**
     * Sets the source type. Accepted values: {@code "conversation"}, {@code "document"}, {@code
     * "api"}. Defaults to {@code "api"} on the server when omitted.
     */
    public Builder sourceType(String sourceType) {
      delegate.setSourceType(CommondtoMemoryScope.SourceTypeEnum.fromValue(sourceType));
      return this;
    }

    /**
     * Sets participant IDs for multi-actor search.
     *
     * <p>The search service will find all conversations where <em>every</em> listed actor
     * participated (AND semantics). Requires at least 2 IDs; each ID must be 1–64 characters.
     * Mutually exclusive with {@code conversationId}; if both are set, {@code conversationId} wins.
     * Only relevant for search — ignored on add.
     */
    public Builder participantIds(java.util.List<String> participantIds) {
      this.participantIds = participantIds;
      return this;
    }

    /** Builds an immutable {@link MemoryScope} instance. */
    public MemoryScope build() {
      return new MemoryScope(delegate, participantIds);
    }
  }
}
