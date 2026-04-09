// <copyright file="SearchOptions.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

/// <summary>
/// Builder-style options for the SearchAsync method.
/// Use fluent <c>With*</c> methods for ergonomic construction:
/// <code>new SearchOptions().WithScope(new MemoryScope().WithActorId("alice")).WithLimit(10)</code>
/// </summary>
public sealed class SearchOptions
{
    /// <summary>Gets the scope containing actor and conversation context.</summary>
    public MemoryScope? Scope { get; private set; }

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

    /// <summary>Gets the search method override. Accepted values: "quick_search", "deep_search", "context_aware_search".</summary>
    public string? Method { get; private set; }

    /// <summary>Gets the reranker type override. Accepted values: "rrf_temporal", "rrf", "temporal", "node_distance", "mmr", "cross_encoder".</summary>
    public string? RerankerType { get; private set; }

    /// <summary>
    /// Gets the MongoDB-style operator DSL filter on memory metadata.
    /// Supports $eq, $ne, $gt, $gte, $lt, $lte, $in, $nin, $contains, $and, $or, $not, and wildcard "*".
    /// Values must be string, int, long, or double — booleans and nested objects are rejected by the server.
    /// Simple equality: <c>new Dictionary&lt;string, object&gt; { ["department"] = "engineering" }</c>
    /// Range: <c>new Dictionary&lt;string, object&gt; { ["level"] = new Dictionary&lt;string, object&gt; { ["$gte"] = 4 } }</c>
    /// Note: $contains is applied as a post-filter and may return fewer results than Limit.
    /// $contains inside $or is rejected with HTTP 400.
    /// </summary>
    public Dictionary<string, object>? MetadataFilter { get; private set; }

    /// <summary>Sets the scope containing actor and conversation context.</summary>
    /// <param name="scope">The scope object grouping actor and conversation fields.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithScope(MemoryScope scope)
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

    /// <summary>Sets the search method override. Defaults to app config if omitted.</summary>
    /// <param name="method">Accepted values: "quick_search", "deep_search", "context_aware_search".</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithMethod(string method)
    {
        this.Method = method;
        return this;
    }

    /// <summary>Sets the reranker type override. Only applies to deep_search. Defaults to app config if omitted.</summary>
    /// <param name="rerankerType">Accepted values: "rrf_temporal", "rrf", "temporal", "node_distance", "mmr", "cross_encoder".</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithRerankerType(string rerankerType)
    {
        this.RerankerType = rerankerType;
        return this;
    }

    /// <summary>Sets the MongoDB-style operator DSL filter on memory metadata.</summary>
    /// <param name="metadataFilter">A dictionary using MongoDB-style operators ($eq, $ne, $gt, $gte, $lt, $lte, $in, $nin, $contains, $and, $or, $not, "*").</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithMetadataFilter(Dictionary<string, object> metadataFilter)
    {
        this.MetadataFilter = metadataFilter;
        return this;
    }
}
