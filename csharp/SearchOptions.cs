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
    /// <summary>
    /// Gets the non-filtering speaker identity for this search — identifies who is asking the
    /// query. NEVER used for filtering; it does not narrow or bias which memories are returned.
    /// The backend resolves it to the speaker's entity for speaker-context query augmentation
    /// and audit attribution. Independent of <see cref="Scope"/>, which remains the only
    /// filtering mechanism. Null for anonymous or system-initiated searches. Max 64 characters.
    /// </summary>
    public string? SpeakerActorId { get; private set; }

    /// <summary>Gets the scope containing actor and conversation context.</summary>
    public MemoryScope? Scope { get; private set; }

    /// <summary>Gets the number of results; null uses the app's top_n. The value must not exceed the server's top_n_max (default 20).</summary>
    public int? Limit { get; private set; }

    /// <summary>Gets the graph traversal depth (1-5); null uses the system default.</summary>
    public int? GraphDepth { get; private set; }

    /// <summary>Gets the start of a time range (ISO-8601 datetime string). Returns memories whose validity period overlaps [FromTime, infinity). Can be used alone (open-ended). With ToTime the range is [FromTime, ToTime]. Cannot be combined with ValidAt.</summary>
    public string? FromTime { get; private set; }

    /// <summary>Gets the end of a time range (ISO-8601 datetime string, inclusive). Returns memories whose validity period overlaps (-infinity, ToTime]. Can be used alone (open-ended). With FromTime the range is [FromTime, ToTime]. Cannot be combined with ValidAt.</summary>
    public string? ToTime { get; private set; }

    /// <summary>Gets the ISO-8601 datetime string for the point-in-time filter: memories valid at exactly this moment. Cannot be combined with FromTime or ToTime.</summary>
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

    /// <summary>Sets the non-filtering speaker identity ("who is asking") for this search.</summary>
    /// <param name="speakerActorId">The speaker actor ID. Never used as a filter by the backend.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithSpeakerActorId(string speakerActorId)
    {
        this.SpeakerActorId = speakerActorId;
        return this;
    }

    /// <summary>Sets the scope containing actor and conversation context.</summary>
    /// <param name="scope">The scope object grouping actor and conversation fields.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithScope(MemoryScope scope)
    {
        this.Scope = scope;
        return this;
    }

    /// <summary>Sets the number of results. It must not exceed the server's top_n_max (default 20). Omit it to use the app's top_n.</summary>
    /// <param name="limit">The number of results (at most the server's top_n_max).</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithLimit(int limit)
    {
        this.Limit = limit;
        return this;
    }

    /// <summary>Sets the graph traversal depth, 1-5; omit it to use the system default.</summary>
    /// <param name="graphDepth">The number of graph hops to traverse when expanding results (1-5).</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithGraphDepth(int graphDepth)
    {
        this.GraphDepth = graphDepth;
        return this;
    }

    /// <summary>Sets the start of a time range filter (ISO-8601). It can be used alone (open-ended).</summary>
    /// <param name="fromTime">ISO-8601 datetime string; memories whose validity period overlaps [fromTime, infinity) are returned.</param>
    /// <returns>The current instance for method chaining.</returns>
    public SearchOptions WithFromTime(string fromTime)
    {
        this.FromTime = fromTime;
        return this;
    }

    /// <summary>Sets the end of a time range filter (ISO-8601). It can be used alone (open-ended).</summary>
    /// <param name="toTime">ISO-8601 datetime string; memories whose validity period overlaps (-infinity, toTime] are returned.</param>
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
