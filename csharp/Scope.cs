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
    /// <summary>Gets the actor ID.</summary>
    public string? ActorId { get; private set; }

    /// <summary>Gets the actor type (e.g. "user", "agent", "system").</summary>
    public string? ActorType { get; private set; }

    /// <summary>Gets the actor display name.</summary>
    public string? ActorName { get; private set; }

    /// <summary>Gets the conversation ID.</summary>
    public string? ConversationId { get; private set; }

    /// <summary>Gets the conversation message ID.</summary>
    public string? ConversationMessageId { get; private set; }

    /// <summary>Gets the source type (e.g. "conversation", "document", "api").</summary>
    public string? SourceType { get; private set; }

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

    /// <summary>Sets the conversation message ID.</summary>
    /// <param name="id">The conversation message identifier.</param>
    /// <returns>This <see cref="Scope"/> instance for fluent chaining.</returns>
    public Scope WithConversationMessageId(string id)
    {
        this.ConversationMessageId = id;
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
}
