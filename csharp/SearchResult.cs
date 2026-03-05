// <copyright file="SearchResult.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

using Smritea.Internal.Autogen.Model;

/// <summary>Wraps a memory with its search relevance score. Delegates to the auto-generated type.</summary>
public sealed class SearchResult
{
    /// <summary>
    /// Initializes a new instance of the <see cref="SearchResult"/> class.
    /// Internal constructor used by SmriteaClient after deserializing the server response
    /// into the autogen <see cref="MemorySearchMemoryResponse"/>.
    /// </summary>
    internal SearchResult(MemorySearchMemoryResponse inner)
    {
        this.Memory = inner.Memory is not null ? new Memory(inner.Memory) : null;
        this.Score = (double)inner.Score;
    }

    /// <summary>
    /// Gets the matched memory. May be null if the server returned a result without a memory object,
    /// though this is not expected under normal operation.
    /// </summary>
    public Memory? Memory { get; }

    /// <summary>Gets the relevance score (0.0-1.0).</summary>
    public double Score { get; }

    /// <summary>Gets convenience accessor for the memory content. Returns null if Memory is null.</summary>
    public string? Content => this.Memory?.Content;

    /// <summary>Gets convenience accessor for the memory ID. Returns null if Memory is null.</summary>
    public string? Id => this.Memory?.Id;
}
