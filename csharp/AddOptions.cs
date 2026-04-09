// <copyright file="AddOptions.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Builder-style options for the AddAsync method.
/// Use fluent <c>With*</c> methods for ergonomic construction:
/// <code>new AddOptions().WithScope(new MemoryScope().WithActorId("alice").WithActorType("user"))</code>
/// </summary>
public sealed class AddOptions
{
    /// <summary>Gets the scope containing actor and conversation context.</summary>
    public MemoryScope? Scope { get; private set; }

    /// <summary>Gets the arbitrary key-value metadata.</summary>
    public Dictionary<string, object>? Metadata { get; private set; }

    /// <summary>Gets the ISO-8601 datetime string for when this content was created or occurred.</summary>
    public string? EventOccurredAt { get; private set; }

    /// <summary>Gets the importance and temporal decay configuration.</summary>
    public RelativeStanding? RelativeStanding { get; private set; }

    /// <summary>Sets the scope containing actor and conversation context.</summary>
    /// <param name="scope">The scope object grouping actor and conversation fields.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithScope(MemoryScope scope)
    {
        this.Scope = scope;
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

    /// <summary>Sets the temporal anchor for extraction (ISO-8601 datetime string).
    /// Used by the extraction LLM to resolve relative temporal expressions like "last year".
    /// Defaults to current time if omitted.</summary>
    /// <param name="eventOccurredAt">ISO-8601 datetime string.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithEventOccurredAt(string eventOccurredAt)
    {
        this.EventOccurredAt = eventOccurredAt;
        return this;
    }

    /// <summary>Sets importance and temporal decay configuration for this memory.</summary>
    /// <param name="relativeStanding">The relative standing configuration.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithRelativeStanding(RelativeStanding relativeStanding)
    {
        this.RelativeStanding = relativeStanding;
        return this;
    }
}
