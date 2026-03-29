// <copyright file="SearchOptions.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Builder-style options for the SearchAsync method.
/// Use fluent <c>With*</c> methods for ergonomic construction:
/// <code>new SearchOptions().WithScope(new Scope().WithActorId("alice")).WithLimit(10)</code>
/// </summary>
public sealed class SearchOptions
{
    /// <summary>Gets the scope containing actor and conversation context.</summary>
    public Scope? Scope { get; private set; }

    /// <summary>Gets the maximum number of results to return.</summary>
    public int? Limit { get; private set; }

    /// <summary>Gets the minimum similarity threshold (0.0–1.0).</summary>
    public float? Threshold { get; private set; }

    /// <summary>Gets the graph traversal depth for graph-augmented search.</summary>
    public int? GraphDepth { get; private set; }

    /// <summary>Gets iSO-8601 datetime string — only return memories created at or after this time.</summary>
    public string? FromTime { get; private set; }

    /// <summary>Gets iSO-8601 datetime string — only return memories created at or before this time.</summary>
    public string? ToTime { get; private set; }

    /// <summary>Gets iSO-8601 datetime string — return memories valid at exactly this point in time.</summary>
    public string? ValidAt { get; private set; }

    /// <summary>Sets the scope containing actor and conversation context.</summary>
    /// <param name="scope">The scope object grouping actor and conversation fields.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithScope(Scope scope)
    {
        this.Scope = scope;
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
