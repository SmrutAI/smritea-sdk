namespace Smritea.Sdk;

/// <summary>Options for the AddAsync method.</summary>
public record AddOptions
{
    public string? UserId { get; init; }
    public string? ActorId { get; init; }
    public string? ActorType { get; init; }
    public string? ActorName { get; init; }
    public Dictionary<string, object>? Metadata { get; init; }
    public string? ConversationId { get; init; }
}

/// <summary>Options for the SearchAsync method.</summary>
public record SearchOptions
{
    public string? UserId { get; init; }
    public string? ActorId { get; init; }
    public string? ActorType { get; init; }
    public int? Limit { get; init; }
    public string? Method { get; init; }
    public float? Threshold { get; init; }
    public int? GraphDepth { get; init; }
    public string? ConversationId { get; init; }
}
