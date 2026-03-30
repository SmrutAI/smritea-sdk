// <copyright file="Scope.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Groups actor and conversation context fields for memory operations.
/// Use fluent <c>With*</c> methods for ergonomic construction:
/// <code>new Scope().WithActorId("alice").WithActorType("user")</code>
/// </summary>
public sealed class Scope
{
    /// <summary>
    /// Gets the actor ID. Max 64 characters; must be paired with <see cref="ActorType"/>.
    /// </summary>
    public string? ActorId { get; private set; }

    /// <summary>
    /// Gets the actor type. Accepted values: <c>"user"</c>, <c>"agent"</c>, <c>"system"</c>.
    /// Must be paired with <see cref="ActorId"/>.
    /// </summary>
    public string? ActorType { get; private set; }

    /// <summary>
    /// Gets the human-readable display name of the actor. Optional; max 255 characters.
    /// </summary>
    public string? ActorName { get; private set; }

    /// <summary>
    /// Gets the conversation ID. Max 64 characters. Mutually exclusive with
    /// <see cref="ParticipantIds"/>; if both are set, <c>ConversationId</c> takes precedence.
    /// </summary>
    public string? ConversationId { get; private set; }

    /// <summary>
    /// Gets the source type. Accepted values: <c>"conversation"</c>, <c>"document"</c>,
    /// <c>"api"</c>. Defaults to <c>"api"</c> on the server when omitted.
    /// </summary>
    public string? SourceType { get; private set; }

    /// <summary>
    /// Gets the participant IDs for multi-actor search. When set, the search service finds all
    /// conversations where <b>every</b> listed actor participated (AND semantics) and searches
    /// memories within those conversations.
    /// </summary>
    /// <remarks>
    /// Requires at least 2 IDs; each ID must be 1–64 characters. Mutually exclusive with
    /// <see cref="ConversationId"/>; if both are set, <c>ConversationId</c> wins.
    /// Only relevant for search — ignored on add.
    /// </remarks>
    public IReadOnlyList<string>? ParticipantIds { get; private set; }

    /// <summary>Sets the actor ID.</summary>
    /// <param name="actorId">The actor identifier.</param>
    /// <returns>This <see cref="Scope"/> instance for fluent chaining.</returns>
    public Scope WithActorId(string actorId)
    {
        this.ActorId = actorId;
        return this;
    }

    /// <summary>Sets the actor type.</summary>
    /// <param name="actorType">The actor type (e.g. "user", "agent", "system").</param>
    /// <returns>This <see cref="Scope"/> instance for fluent chaining.</returns>
    public Scope WithActorType(string actorType)
    {
        this.ActorType = actorType;
        return this;
    }

    /// <summary>Sets the actor display name.</summary>
    /// <param name="actorName">The actor display name.</param>
    /// <returns>This <see cref="Scope"/> instance for fluent chaining.</returns>
    public Scope WithActorName(string actorName)
    {
        this.ActorName = actorName;
        return this;
    }

    /// <summary>Sets the conversation ID.</summary>
    /// <param name="conversationId">The conversation identifier.</param>
    /// <returns>This <see cref="Scope"/> instance for fluent chaining.</returns>
    public Scope WithConversationId(string conversationId)
    {
        this.ConversationId = conversationId;
        return this;
    }

    /// <summary>Sets the source type.</summary>
    /// <param name="sourceType">The source type (e.g. "conversation", "document", "api").</param>
    /// <returns>This <see cref="Scope"/> instance for fluent chaining.</returns>
    public Scope WithSourceType(string sourceType)
    {
        this.SourceType = sourceType;
        return this;
    }

    /// <summary>Sets the participant IDs for multi-actor search.</summary>
    /// <param name="participantIds">
    /// Actor IDs whose shared conversations to search. The service finds all conversations
    /// where every listed actor participated (AND semantics). Requires at least 2 IDs;
    /// each ID must be 1–64 characters. Mutually exclusive with <see cref="ConversationId"/>.
    /// </param>
    /// <returns>This <see cref="Scope"/> instance for fluent chaining.</returns>
    public Scope WithParticipantIds(IReadOnlyList<string> participantIds)
    {
        this.ParticipantIds = participantIds;
        return this;
    }
}
