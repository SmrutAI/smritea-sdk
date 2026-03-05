// <copyright file="AddOptions.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Builder-style options for the AddAsync method.
/// Use fluent <c>With*</c> methods for ergonomic construction:
/// <code>new AddOptions().WithUserId("user-42").WithMetadata(meta)</code>
/// </summary>
public sealed class AddOptions
{
    /// <summary>Gets the user ID (convenience shorthand that forces ActorType="user").</summary>
    public string? UserId { get; private set; }

    /// <summary>Gets the actor ID.</summary>
    public string? ActorId { get; private set; }

    /// <summary>Gets the actor type (e.g. "user", "agent").</summary>
    public string? ActorType { get; private set; }

    /// <summary>Gets the actor display name.</summary>
    public string? ActorName { get; private set; }

    /// <summary>Gets the arbitrary key-value metadata.</summary>
    public Dictionary<string, object>? Metadata { get; private set; }

    /// <summary>Gets the conversation ID.</summary>
    public string? ConversationId { get; private set; }

    /// <summary>Sets the user ID and forces ActorType to "user".</summary>
    /// <param name="userId">The user identifier to attribute this memory to.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithUserId(string userId)
    {
        this.UserId = userId;
        return this;
    }

    /// <summary>Sets the actor ID.</summary>
    /// <param name="actorId">The actor identifier to attribute this memory to.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithActorId(string actorId)
    {
        this.ActorId = actorId;
        return this;
    }

    /// <summary>Sets the actor type.</summary>
    /// <param name="actorType">The actor type (e.g. "user", "agent").</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithActorType(string actorType)
    {
        this.ActorType = actorType;
        return this;
    }

    /// <summary>Sets the actor display name.</summary>
    /// <param name="actorName">The human-readable name of the actor.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithActorName(string actorName)
    {
        this.ActorName = actorName;
        return this;
    }

    /// <summary>Sets the metadata dictionary attached to the memory.</summary>
    /// <param name="metadata">Arbitrary key-value pairs to store alongside the memory.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithMetadata(Dictionary<string, object> metadata)
    {
        this.Metadata = metadata;
        return this;
    }

    /// <summary>Sets the conversation ID to scope this memory to a specific conversation.</summary>
    /// <param name="conversationId">The conversation identifier to associate with the memory.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithConversationId(string conversationId)
    {
        this.ConversationId = conversationId;
        return this;
    }
}
