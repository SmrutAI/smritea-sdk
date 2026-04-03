// <copyright file="RelativeStanding.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Importance and temporal decay configuration for a memory.
/// Controls how the memory's relevance score decays over time in search results.
/// All fields are optional — omitted fields use server defaults
/// (importance=1.0, decay_factor=0.2, decay_function="exponential").
/// <code>new RelativeStanding().WithImportance(0.9f).WithDecayFactor(0.5f).WithDecayFunction("exponential")</code>
/// </summary>
public sealed class RelativeStanding
{
    /// <summary>Gets the importance score (0.0-1.0). Higher = ranks higher in search.</summary>
    public float? Importance { get; private set; }

    /// <summary>Gets the decay factor. 0 = no decay (pinned permanently). 0.2 = light (default). 1.0 = standard. 3.0+ = aggressive.</summary>
    public float? DecayFactor { get; private set; }

    /// <summary>Gets the decay curve shape. Accepted values: "exponential", "gaussian", "linear".</summary>
    public string? DecayFunction { get; private set; }

    /// <summary>Sets the importance score (0.0-1.0).</summary>
    /// <param name="importance">Memory importance — higher values rank higher in search results.</param>
    /// <returns>The current instance for method chaining.</returns>
    public RelativeStanding WithImportance(float importance)
    {
        this.Importance = importance;
        return this;
    }

    /// <summary>Sets the decay factor.</summary>
    /// <param name="decayFactor">0 = no decay (pinned). 0.2 = light. 1.0 = standard. 3.0+ = aggressive.</param>
    /// <returns>The current instance for method chaining.</returns>
    public RelativeStanding WithDecayFactor(float decayFactor)
    {
        this.DecayFactor = decayFactor;
        return this;
    }

    /// <summary>Sets the decay function.</summary>
    /// <param name="decayFunction">Accepted values: "exponential", "gaussian", "linear".</param>
    /// <returns>The current instance for method chaining.</returns>
    public RelativeStanding WithDecayFunction(string decayFunction)
    {
        this.DecayFunction = decayFunction;
        return this;
    }
}
