namespace Smritea.Sdk;

/// <summary>
/// Public-facing Memory type. Wraps the auto-generated response type.
/// Fields match the server response with PascalCase naming.
/// </summary>
public sealed class Memory
{
    public string? Id { get; init; }
    public string? AppId { get; init; }
    public string? Content { get; init; }
    public string? ActorId { get; init; }
    public string? ActorType { get; init; }
    public string? ActorName { get; init; }
    public Dictionary<string, object>? Metadata { get; init; }
    public string? ConversationId { get; init; }
    public string? ConversationMessageId { get; init; }
    public string? ActiveFrom { get; init; }
    public string? ActiveTo { get; init; }
    public string? CreatedAt { get; init; }
    public string? UpdatedAt { get; init; }
}
