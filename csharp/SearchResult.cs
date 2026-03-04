namespace Smritea.Sdk;

public sealed class SearchResult
{
    public Memory? Memory { get; init; }
    public double? Score { get; init; }

    /// <summary>Convenience accessor for the memory content.</summary>
    public string? Content => Memory?.Content;

    /// <summary>Convenience accessor for the memory ID.</summary>
    public string? Id => Memory?.Id;
}
