package ai.smritea.sdk.model;

import java.util.Map;

/**
 * Public-facing Memory type. Wraps the auto-generated MemoryMemoryResponse. When autogen types are
 * generated, the constructor will accept the inner type. For now, this uses direct fields that match
 * the server response.
 */
public final class Memory {
    private final String id;
    private final String appId;
    private final String content;
    private final String actorId;
    private final String actorType;
    private final String actorName;
    private final Map<String, Object> metadata;
    private final String conversationId;
    private final String conversationMessageId;
    private final String activeFrom;
    private final String activeTo;
    private final String createdAt;
    private final String updatedAt;

    /**
     * Creates a new Memory instance.
     *
     * @param id the memory ID
     * @param appId the app ID this memory belongs to
     * @param content the memory content text
     * @param actorId the actor ID who created this memory
     * @param actorType the type of actor (e.g. "user", "agent")
     * @param actorName the display name of the actor
     * @param metadata arbitrary key-value metadata
     * @param conversationId the conversation this memory belongs to
     * @param conversationMessageId the specific message ID within the conversation
     * @param activeFrom ISO-8601 timestamp for when this memory becomes active
     * @param activeTo ISO-8601 timestamp for when this memory expires
     * @param createdAt ISO-8601 timestamp for creation time
     * @param updatedAt ISO-8601 timestamp for last update time
     */
    public Memory(
            String id,
            String appId,
            String content,
            String actorId,
            String actorType,
            String actorName,
            Map<String, Object> metadata,
            String conversationId,
            String conversationMessageId,
            String activeFrom,
            String activeTo,
            String createdAt,
            String updatedAt) {
        this.id = id;
        this.appId = appId;
        this.content = content;
        this.actorId = actorId;
        this.actorType = actorType;
        this.actorName = actorName;
        this.metadata = metadata;
        this.conversationId = conversationId;
        this.conversationMessageId = conversationMessageId;
        this.activeFrom = activeFrom;
        this.activeTo = activeTo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Returns the memory ID. */
    public String getId() {
        return id;
    }

    /** Returns the app ID this memory belongs to. */
    public String getAppId() {
        return appId;
    }

    /** Returns the memory content text. */
    public String getContent() {
        return content;
    }

    /** Returns the actor ID who created this memory. */
    public String getActorId() {
        return actorId;
    }

    /** Returns the type of actor (e.g. "user", "agent"). */
    public String getActorType() {
        return actorType;
    }

    /** Returns the display name of the actor. */
    public String getActorName() {
        return actorName;
    }

    /** Returns the arbitrary key-value metadata. */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /** Returns the conversation ID this memory belongs to. */
    public String getConversationId() {
        return conversationId;
    }

    /** Returns the specific message ID within the conversation. */
    public String getConversationMessageId() {
        return conversationMessageId;
    }

    /** Returns the ISO-8601 timestamp for when this memory becomes active. */
    public String getActiveFrom() {
        return activeFrom;
    }

    /** Returns the ISO-8601 timestamp for when this memory expires. */
    public String getActiveTo() {
        return activeTo;
    }

    /** Returns the ISO-8601 timestamp for creation time. */
    public String getCreatedAt() {
        return createdAt;
    }

    /** Returns the ISO-8601 timestamp for last update time. */
    public String getUpdatedAt() {
        return updatedAt;
    }
}
