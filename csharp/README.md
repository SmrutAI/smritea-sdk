# smritea SDK — C#

C# SDK for the [smritea](https://smritea.ai) AI memory system.

**[Get a free API key →](https://smritea.ai)**

---

## Installation

```bash
dotnet add package Smritea.Sdk
```

Requires .NET 8+.

---

## Get your API key

1. Sign up at **[smritea.ai](https://smritea.ai)** — free account, no credit card required
2. Create an app in the dashboard and copy your API key (`sk-...`) and App ID (`app_...`)
3. Export them as environment variables:

```bash
export SMRITEA_API_KEY="sk-..."
export SMRITEA_APP_ID="app_..."
```

---

## Quickstart

```csharp
using Smritea.Sdk;

var client = new SmriteaClient(
    apiKey: Environment.GetEnvironmentVariable("SMRITEA_API_KEY")!,
    appId: Environment.GetEnvironmentVariable("SMRITEA_APP_ID")!
);

// Store something about a user
var mem = await client.AddAsync("Alice is a vegetarian and loves hiking",
    new AddOptions { UserId = "alice" });

// Retrieve it later
var results = await client.SearchAsync("What are Alice's food preferences?",
    new SearchOptions { UserId = "alice" });
foreach (var r in results)
    Console.WriteLine($"{r.Score:F2}  {r.Content}");
```

---

## Constructor

```csharp
using Smritea.Sdk;

var client = new SmriteaClient(
    apiKey: "sk-...",                           // required
    appId: "app_...",                           // required
    baseUrl: "https://api.smritea.ai",         // optional, default shown
    maxRetries: 2                               // optional, default 2; 0 disables retry
);
```

---

## Methods

### `AddAsync` — Store a memory

```csharp
var memory = await client.AddAsync("User prefers concise replies", new AddOptions
{
    UserId = "alice",                                           // shorthand for ActorId + ActorType="user"
    Metadata = new Dictionary<string, object> { ["source"] = "chat" },  // optional
    ConversationId = "conv_123",                                // optional
});
Console.WriteLine(memory.Id); // mem_...
```

`UserId` is a shorthand — sets `ActorId` and forces `ActorType="user"`. For agent or system memories use `ActorId` + `ActorType` directly.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `content` | `string` | required | Memory text |
| `UserId` | `string?` | `null` | Shorthand: ActorId + ActorType="user" |
| `ActorId` | `string?` | `null` | Explicit actor ID |
| `ActorType` | `string?` | `null` | `"user"` \| `"agent"` \| `"system"` |
| `ActorName` | `string?` | `null` | Display name |
| `Metadata` | `Dictionary<string, object>?` | `null` | Arbitrary key-value dictionary |
| `ConversationId` | `string?` | `null` | Conversation context |

All methods accept an optional `CancellationToken` as the last parameter.

---

### `SearchAsync` — Semantic search

```csharp
var results = await client.SearchAsync("dietary restrictions", new SearchOptions
{
    UserId = "alice",
    Limit = 5,
    Method = "deep_search",     // "quick_search" | "deep_search" | "context_aware_search"
    Threshold = 0.7f,           // min relevance score 0.0–1.0
});
foreach (var r in results)
    Console.WriteLine($"{r.Score}  {r.Content}");
```

Results are ordered by relevance (descending). Each result exposes `Score` (0.0–1.0) and all `Memory` properties directly.

| Search method | When to use |
|---|---|
| `quick_search` | Low-latency, keyword + vector hybrid |
| `deep_search` | Higher recall, traverses the memory graph |
| `context_aware_search` | Reranks results using conversation context |

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `string` | required | Search text |
| `UserId` | `string?` | `null` | Filter to this user's memories |
| `ActorId` | `string?` | `null` | Filter by actor ID |
| `ActorType` | `string?` | `null` | Filter by actor type |
| `Limit` | `int?` | app default | Max results to return |
| `Method` | `string?` | app default | Search strategy |
| `Threshold` | `float?` | `null` | Min relevance score 0.0–1.0 |
| `GraphDepth` | `int?` | `null` | Graph traversal depth override |
| `ConversationId` | `string?` | `null` | Conversation context |

---

### `GetAsync` — Retrieve a memory by ID

```csharp
var memory = await client.GetAsync("mem_abc123");
Console.WriteLine($"{memory.Content} {memory.CreatedAt}");
// Throws SmriteaNotFoundException if the ID does not exist
```

---

### `DeleteAsync` — Delete a memory by ID

```csharp
await client.DeleteAsync("mem_abc123");
// Throws SmriteaNotFoundException if the ID does not exist
```

---

### `GetAllAsync` — List all memories

> **Not yet implemented.** Throws `NotImplementedException`.
> Use `SearchAsync()` with a broad query as a workaround:

```csharp
var results = await client.SearchAsync("", new SearchOptions
{
    UserId = "alice",
    Limit = 100,
});
```

---

## Error handling

```csharp
using Smritea.Sdk;

try
{
    var results = await client.SearchAsync("preferences", new SearchOptions
    {
        UserId = "alice",
    });
}
catch (SmriteaAuthException)
{
    Console.WriteLine("Check your API key");
}
catch (SmriteaRateLimitException ex)
{
    Console.WriteLine($"Rate limited — retry after {ex.RetryAfter}s");
}
catch (SmriteaQuotaException)
{
    Console.WriteLine("Plan quota exceeded");
}
catch (SmriteaException ex)
{
    Console.WriteLine($"Unexpected error: {ex.Message}");
}
```

| Exception | HTTP | When |
|---|---|---|
| `SmriteaAuthException` | 401 | Invalid or missing API key |
| `SmriteaValidationException` | 400 | Invalid request parameters |
| `SmriteaNotFoundException` | 404 | Memory ID does not exist |
| `SmriteaQuotaException` | 402 | Organisation quota exceeded |
| `SmriteaRateLimitException` | 429 | Rate limit hit — check `.RetryAfter` |
| `SmriteaException` | other | Unexpected server error |

---

## `Memory` type reference

| Property | Type | Description |
|---|---|---|
| `Id` | string | Memory ID (`mem_...`) |
| `AppId` | string | App this memory belongs to |
| `Content` | string | Memory text |
| `ActorId` | string | Actor who owns this memory |
| `ActorType` | string | `"user"` \| `"agent"` \| `"system"` |
| `ActorName` | string? | Display name |
| `Metadata` | Dictionary<string, object>? | Arbitrary key-value pairs |
| `ConversationId` | string? | Conversation context |
| `ActiveFrom` | string | ISO 8601 — when memory becomes valid |
| `ActiveTo` | string? | ISO 8601 — when memory expires |
| `CreatedAt` | string | ISO 8601 creation timestamp |
| `UpdatedAt` | string | ISO 8601 last update timestamp |
