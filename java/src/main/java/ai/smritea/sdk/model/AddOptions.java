package ai.smritea.sdk.model;

import java.util.Map;

/** Builder-style options for adding a memory. */
public final class AddOptions {
    private String userId;
    private String actorId;
    private String actorType;
    private String actorName;
    private Map<String, Object> metadata;
    private String conversationId;

    /** Creates a new AddOptions with all fields unset. */
    public AddOptions() {}

    /**
     * Sets the user ID.
     *
     * @param userId the user ID
     * @return this instance for chaining
     */
    public AddOptions withUserId(String userId) {
        this.userId = userId;
        return this;
    }

    /**
     * Sets the actor ID.
     *
     * @param actorId the actor ID
     * @return this instance for chaining
     */
    public AddOptions withActorId(String actorId) {
        this.actorId = actorId;
        return this;
    }

    /**
     * Sets the actor type (e.g. "user", "agent").
     *
     * @param actorType the actor type
     * @return this instance for chaining
     */
    public AddOptions withActorType(String actorType) {
        this.actorType = actorType;
        return this;
    }

    /**
     * Sets the actor display name.
     *
     * @param actorName the actor name
     * @return this instance for chaining
     */
    public AddOptions withActorName(String actorName) {
        this.actorName = actorName;
        return this;
    }

    /**
     * Sets arbitrary key-value metadata.
     *
     * @param metadata the metadata map
     * @return this instance for chaining
     */
    public AddOptions withMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        return this;
    }

    /**
     * Sets the conversation ID.
     *
     * @param conversationId the conversation ID
     * @return this instance for chaining
     */
    public AddOptions withConversationId(String conversationId) {
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

    /** Returns the actor name. */
    public String getActorName() {
        return actorName;
    }

    /** Returns the metadata map. */
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    /** Returns the conversation ID. */
    public String getConversationId() {
        return conversationId;
    }
}
