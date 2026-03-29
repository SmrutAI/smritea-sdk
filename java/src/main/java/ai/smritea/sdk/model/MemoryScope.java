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
 *     "conversation_message_id": null,
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
   * Creates a MemoryScope from the auto-generated scope type. Called internally — external callers
   * should not use this constructor directly (the autogen type is an internal implementation
   * detail).
   *
   * @param inner the autogen scope object
   */
  public MemoryScope(CommondtoMemoryScope inner) {
    this.inner = inner;
  }

  /** Returns a new builder for constructing MemoryScope instances in tests. */
  public static Builder builder() {
    return new Builder();
  }

  /** Returns the actor ID, or null if not set. */
  public String getActorId() {
    return inner.getActorId();
  }

  /** Returns the actor type as a string (e.g. "user", "agent", "system"), or null if not set. */
  public String getActorType() {
    if (inner.getActorType() == null) {
      return null;
    }
    return inner.getActorType().getValue();
  }

  /** Returns the display name of the actor, or null if not set. */
  public String getActorName() {
    return inner.getActorName();
  }

  /** Returns the conversation ID, or null if not set. */
  public String getConversationId() {
    return inner.getConversationId();
  }

  /** Returns the conversation message ID, or null if not set. */
  public String getConversationMessageId() {
    return inner.getConversationMessageId();
  }

  /** Returns the source type (e.g. "conversation", "document", "api"), or null if not set. */
  public String getSourceType() {
    if (inner.getSourceType() == null) {
      return null;
    }
    return inner.getSourceType().getValue();
  }

  /** Builder for constructing {@link MemoryScope} instances in tests. */
  public static final class Builder {
    private final CommondtoMemoryScope delegate;

    private Builder() {
      delegate = new CommondtoMemoryScope();
    }

    public Builder actorId(String actorId) {
      delegate.setActorId(actorId);
      return this;
    }

    public Builder actorType(String actorType) {
      delegate.setActorType(CommondtoMemoryScope.ActorTypeEnum.fromValue(actorType));
      return this;
    }

    public Builder actorName(String actorName) {
      delegate.setActorName(actorName);
      return this;
    }

    public Builder conversationId(String conversationId) {
      delegate.setConversationId(conversationId);
      return this;
    }

    public Builder conversationMessageId(String conversationMessageId) {
      delegate.setConversationMessageId(conversationMessageId);
      return this;
    }

    public Builder sourceType(String sourceType) {
      delegate.setSourceType(CommondtoMemoryScope.SourceTypeEnum.fromValue(sourceType));
      return this;
    }

    /** Builds an immutable {@link MemoryScope} instance. */
    public MemoryScope build() {
      return new MemoryScope(delegate);
    }
  }
}
