// <copyright file="SearchOptions.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Builder-style options for the SearchAsync method.
/// Use fluent <c>With*</c> methods for ergonomic construction:
/// <code>new SearchOptions().WithLimit(10).WithThreshold(0.7f)</code>
/// </summary>
public sealed class SearchOptions
{
    /// <summary>Gets the user ID (convenience shorthand that forces ActorType="user").</summary>
    public string? UserId { get; private set; }

    /// <summary>Gets the actor ID.</summary>
    public string? ActorId { get; private set; }

    /// <summary>Gets the actor type.</summary>
    public string? ActorType { get; private set; }

    /// <summary>Gets the maximum number of results to return.</summary>
    public int? Limit { get; private set; }

    /// <summary>Gets the minimum similarity threshold (0.0–1.0).</summary>
    public float? Threshold { get; private set; }

    /// <summary>Gets the graph traversal depth for graph-augmented search.</summary>
    public int? GraphDepth { get; private set; }

    /// <summary>Gets the conversation ID to scope the search to.</summary>
    public string? ConversationId { get; private set; }

    /// <summary>Gets iSO-8601 datetime string — only return memories created at or after this time.</summary>
    public string? FromTime { get; private set; }

    /// <summary>Gets iSO-8601 datetime string — only return memories created at or before this time.</summary>
    public string? ToTime { get; private set; }

    /// <summary>Gets iSO-8601 datetime string — return memories valid at exactly this point in time.</summary>
    public string? ValidAt { get; private set; }

    /// <summary>Sets the user ID and forces ActorType to "user".</summary>
    /// <param name="userId">The user identifier to scope the search to.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithUserId(string userId)
    {
        this.UserId = userId;
        return this;
    }

    /// <summary>Sets the actor ID.</summary>
    /// <param name="actorId">The actor identifier to scope the search to.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithActorId(string actorId)
    {
        this.ActorId = actorId;
        return this;
    }

    /// <summary>Sets the actor type.</summary>
    /// <param name="actorType">The actor type to filter by (e.g. "user", "agent").</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithActorType(string actorType)
    {
        this.ActorType = actorType;
        return this;
    }

    /// <summary>Sets the maximum number of results to return.</summary>
    /// <param name="limit">The maximum number of results (must be positive).</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithLimit(int limit)
    {
        this.Limit = limit;
        return this;
    }

    /// <summary>Sets the minimum similarity threshold.</summary>
    /// <param name="threshold">The minimum score threshold in the range 0.0 to 1.0.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithThreshold(float threshold)
    {
        this.Threshold = threshold;
        return this;
    }

    /// <summary>Sets the graph traversal depth for graph-augmented search.</summary>
    /// <param name="graphDepth">The number of graph hops to traverse when expanding results.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithGraphDepth(int graphDepth)
    {
        this.GraphDepth = graphDepth;
        return this;
    }

    /// <summary>Sets the conversation ID to scope the search to a specific conversation.</summary>
    /// <param name="conversationId">The conversation identifier to filter results by.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithConversationId(string conversationId)
    {
        this.ConversationId = conversationId;
        return this;
    }

    /// <summary>Sets the lower bound of the time range filter (ISO-8601).</summary>
    /// <param name="fromTime">ISO-8601 datetime string; only memories at or after this time are returned.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithFromTime(string fromTime)
    {
        this.FromTime = fromTime;
        return this;
    }

    /// <summary>Sets the upper bound of the time range filter (ISO-8601).</summary>
    /// <param name="toTime">ISO-8601 datetime string; only memories at or before this time are returned.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithToTime(string toTime)
    {
        this.ToTime = toTime;
        return this;
    }

    /// <summary>Sets a point-in-time filter (ISO-8601). Mutually exclusive with FromTime/ToTime.</summary>
    /// <param name="validAt">ISO-8601 datetime string; only memories valid at exactly this instant are returned.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithValidAt(string validAt)
    {
        this.ValidAt = validAt;
        return this;
    }
}
