// <copyright file="AddOptions.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Builder-style options for the AddAsync method.
/// Use fluent <c>With*</c> methods for ergonomic construction:
/// <code>new AddOptions().WithScope(new Scope().WithActorId("alice").WithActorType("user"))</code>
/// </summary>
public sealed class AddOptions
{
    /// <summary>Gets the scope containing actor and conversation context.</summary>
    public Scope? Scope { get; private set; }

    /// <summary>Gets the arbitrary key-value metadata.</summary>
    public Dictionary<string, object>? Metadata { get; private set; }

    /// <summary>Sets the scope containing actor and conversation context.</summary>
    /// <param name="scope">The scope object grouping actor and conversation fields.</param>
    /// <returns>The current instance for method chaining.</returns>
    public AddOptions WithScope(Scope scope)
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
}
