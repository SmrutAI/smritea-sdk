# smritea SDK — Go

Go SDK for the [smritea](https://smritea.ai) AI memory system.

**[Get a free API key →](https://smritea.ai)**

---

## Installation

```bash
go get github.com/SmrutAI/smritea-sdk/go
```

Requires Go 1.22+.

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

```go
package main

import (
    "context"
    "fmt"
    "os"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

func main() {
    client := smritea.NewClient(smritea.ClientConfig{
        APIKey: os.Getenv("SMRITEA_API_KEY"),
        AppID:  os.Getenv("SMRITEA_APP_ID"),
    })

    ctx := context.Background()

    // Store something about a user
    client.Add(ctx, "Alice is a vegetarian and loves hiking", &smritea.AddOptions{
        UserID: strPtr("alice"),
    })

    // Retrieve it later
    results, _ := client.Search(ctx, "What are Alice's food preferences?", &smritea.SearchOptions{
        UserID: strPtr("alice"),
    })
    for _, r := range results {
        fmt.Printf("%v  %v\n", r.Score, r.Memory.Content)
    }
}

func strPtr(s string) *string { return &s }
```

---

## Constructor

```go
import smritea "github.com/SmrutAI/smritea-sdk/go"

client := smritea.NewClient(smritea.ClientConfig{
    APIKey:     "sk-...",                   // required
    AppID:      "app_...",                  // required
    BaseURL:    "https://api.smritea.ai",   // optional, default shown
    MaxRetries: 2,                          // optional, default 2; 0 disables retry
})
```

---

## Methods

### `Add` — Store a memory

```go
memory, err := client.Add(ctx, "User prefers concise replies", &smritea.AddOptions{
    UserID:         strPtr("alice"),              // shorthand for ActorID + ActorType="user"
    Metadata:       map[string]interface{}{"source": "chat"}, // optional
    ConversationID: strPtr("conv_123"),           // optional
})
fmt.Println(memory.Id) // mem_...
```

`UserID` is a shorthand — sets `ActorID` and forces `ActorType="user"`. For agent or system memories use `ActorID` + `ActorType` directly.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `content` | `string` | required | Memory text |
| `UserID` | `*string` | `nil` | Shorthand: ActorID + ActorType="user" |
| `ActorID` | `*string` | `nil` | Explicit actor ID |
| `ActorType` | `*string` | `nil` | `"user"` \| `"agent"` \| `"system"` |
| `ActorName` | `*string` | `nil` | Display name |
| `Metadata` | `map[string]interface{}` | `nil` | Arbitrary key-value map |
| `ConversationID` | `*string` | `nil` | Conversation context |

---

### `Search` — Semantic search

```go
results, err := client.Search(ctx, "dietary restrictions", &smritea.SearchOptions{
    UserID:    strPtr("alice"),
    Limit:     int32Ptr(5),
    Method:    strPtr("deep_search"), // "quick_search" | "deep_search" | "context_aware_search"
    Threshold: float32Ptr(0.7),       // min relevance score 0.0–1.0
})
for _, r := range results {
    fmt.Println(r.Score, r.Memory.Content)
}
```

Results are ordered by relevance (descending). Each result exposes `Score` (0.0–1.0) and a `Memory` struct.

| Search method | When to use |
|---|---|
| `quick_search` | Low-latency, keyword + vector hybrid |
| `deep_search` | Higher recall, traverses the memory graph |
| `context_aware_search` | Reranks results using conversation context |

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `string` | required | Search text |
| `UserID` | `*string` | `nil` | Filter to this user's memories |
| `ActorID` | `*string` | `nil` | Filter by actor ID |
| `ActorType` | `*string` | `nil` | Filter by actor type |
| `Limit` | `*int32` | app default | Max results to return |
| `Method` | `*string` | app default | Search strategy |
| `Threshold` | `*float32` | `nil` | Min relevance score 0.0–1.0 |
| `GraphDepth` | `*int32` | `nil` | Graph traversal depth override |
| `ConversationID` | `*string` | `nil` | Conversation context |

---

### `Get` — Retrieve a memory by ID

```go
memory, err := client.Get(ctx, "mem_abc123")
fmt.Println(memory.Content, memory.CreatedAt)
// Returns SmriteaNotFoundError if the ID does not exist
```

---

### `Delete` — Delete a memory by ID

```go
err := client.Delete(ctx, "mem_abc123")
// Returns SmriteaNotFoundError if the ID does not exist
```

---

### `GetAll` — List all memories

> **Not yet implemented.** Returns an error.
> Use `Search()` with a broad query as a workaround:

```go
results, _ := client.Search(ctx, "", &smritea.SearchOptions{
    UserID: strPtr("alice"),
    Limit:  int32Ptr(100),
})
```

---

## Error handling

```go
import (
    "errors"
    "fmt"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

results, err := client.Search(ctx, "preferences", &smritea.SearchOptions{
    UserID: strPtr("alice"),
})
if err != nil {
    var authErr *smritea.SmriteaAuthError
    var rateLimitErr *smritea.SmriteaRateLimitError
    var quotaErr *smritea.SmriteaQuotaError
    var smriteaErr *smritea.SmriteaError

    switch {
    case errors.As(err, &authErr):
        fmt.Println("Check your API key")
    case errors.As(err, &rateLimitErr):
        fmt.Printf("Rate limited — retry after %ds\n", rateLimitErr.RetryAfter)
    case errors.As(err, &quotaErr):
        fmt.Println("Plan quota exceeded")
    case errors.As(err, &smriteaErr):
        fmt.Printf("Unexpected error: %s\n", smriteaErr.Message)
    default:
        fmt.Printf("Non-API error: %v\n", err)
    }
}
```

| Error type | HTTP | When |
|---|---|---|
| `SmriteaAuthError` | 401 | Invalid or missing API key |
| `SmriteaValidationError` | 400 | Invalid request parameters |
| `SmriteaNotFoundError` | 404 | Memory ID does not exist |
| `SmriteaQuotaError` | 402 | Organisation quota exceeded |
| `SmriteaRateLimitError` | 429 | Rate limit hit — check `.RetryAfter` |
| `SmriteaError` | other | Unexpected server error |

---

## `Memory` type reference

| Field | Type | Description |
|---|---|---|
| `Id` | string | Memory ID (`mem_...`) |
| `AppId` | string | App this memory belongs to |
| `Content` | string | Memory text |
| `ActorId` | string | Actor who owns this memory |
| `ActorType` | string | `"user"` \| `"agent"` \| `"system"` |
| `ActorName` | *string | Display name |
| `Metadata` | map[string]interface{} | Arbitrary key-value pairs |
| `ConversationId` | *string | Conversation context |
| `ActiveFrom` | string | ISO 8601 — when memory becomes valid |
| `ActiveTo` | *string | ISO 8601 — when memory expires |
| `CreatedAt` | string | ISO 8601 creation timestamp |
| `UpdatedAt` | string | ISO 8601 last update timestamp |
