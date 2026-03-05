package ai.smritea.sdk.model;

import ai.smritea.sdk._internal.autogen.model.MemoryMemoryResponse;

import java.util.Map;

/**
 * Public-facing Memory type. Delegates to the auto-generated {@link MemoryMemoryResponse}.
 *
 * <p>Use the {@link Builder} to construct instances in tests — avoids error-prone positional args.
 * In production, instances are created internally by {@code SmriteaClient} from deserialized
 * autogen responses.
 */
public final class Memory {
    private final MemoryMemoryResponse inner;

    /**
     * Creates a Memory from the auto-generated response type. Called by SmriteaClient after
     * deserializing the server response. External callers should not use this constructor directly
     * (the autogen type is an internal implementation detail).
     *
     * @param inner the autogen response object
     */
    public Memory(MemoryMemoryResponse inner) {
        this.inner = inner;
    }

    /**
     * Returns a new builder with required fields pre-set.
     *
     * @param id the memory ID (required)
     * @param appId the app ID this memory belongs to (required)
     * @param content the memory content text (required)
     * @param createdAt ISO-8601 creation timestamp (required)
     * @param updatedAt ISO-8601 last update timestamp (required)
     */
    public static Builder builder(
            String id, String appId, String content, String createdAt, String updatedAt) {
        return new Builder(id, appId, content, createdAt, updatedAt);
    }

    /** Returns the memory ID. */
    public String getId() {
        return inner.getId();
    }

    /** Returns the app ID this memory belongs to. */
    public String getAppId() {
        return inner.getAppId();
    }

    /** Returns the memory content text. */
    public String getContent() {
        return inner.getContent();
    }

    /** Returns the actor ID who created this memory. */
    public String getActorId() {
        return inner.getActorId();
    }

    /** Returns the type of actor (e.g. "user", "agent"). */
    public String getActorType() {
        return inner.getActorType();
    }

    /** Returns the display name of the actor. */
    public String getActorName() {
        return inner.getActorName();
    }

    /**
     * Returns the arbitrary key-value metadata, or null if not set. The autogen type stores
     * metadata as {@code Object}; this getter casts it to a typed map.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetadata() {
        Object raw = inner.getMetadata();
        if (raw instanceof Map) {
            return (Map<String, Object>) raw;
        }
        return null;
    }

    /** Returns the conversation ID this memory belongs to. */
    public String getConversationId() {
        return inner.getConversationId();
    }

    /** Returns the specific message ID within the conversation. */
    public String getConversationMessageId() {
        return inner.getConversationMessageId();
    }

    /** Returns the ISO-8601 timestamp for when this memory becomes active. */
    public String getActiveFrom() {
        return inner.getActiveFrom();
    }

    /** Returns the ISO-8601 timestamp for when this memory expires. */
    public String getActiveTo() {
        return inner.getActiveTo();
    }

    /** Returns the ISO-8601 timestamp for creation time. */
    public String getCreatedAt() {
        return inner.getCreatedAt();
    }

    /** Returns the ISO-8601 timestamp for last update time. */
    public String getUpdatedAt() {
        return inner.getUpdatedAt();
    }

    /**
     * Builder for constructing {@link Memory} instances with named fields. Internally populates a
     * {@link MemoryMemoryResponse} and wraps it.
     */
    public static final class Builder {
        private final MemoryMemoryResponse delegate;

        private Builder(
                String id, String appId, String content, String createdAt, String updatedAt) {
            delegate = new MemoryMemoryResponse();
            delegate.setId(id);
            delegate.setAppId(appId);
            delegate.setContent(content);
            delegate.setCreatedAt(createdAt);
            delegate.setUpdatedAt(updatedAt);
        }

        public Builder actorId(String actorId) {
            delegate.setActorId(actorId);
            return this;
        }

        public Builder actorType(String actorType) {
            delegate.setActorType(actorType);
            return this;
        }

        public Builder actorName(String actorName) {
            delegate.setActorName(actorName);
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            delegate.setMetadata(metadata);
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

        public Builder activeFrom(String activeFrom) {
            delegate.setActiveFrom(activeFrom);
            return this;
        }

        public Builder activeTo(String activeTo) {
            delegate.setActiveTo(activeTo);
            return this;
        }

        /** Builds an immutable {@link Memory} instance. */
        public Memory build() {
            return new Memory(delegate);
        }
    }
}
