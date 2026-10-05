---
type: Overview
title: smritea SDK — Go
status: stable
tags:
- readme
stale_after: 2026-12-31
generated:
  by: Tushar Dwivedi
  at: 2026-08-25T00:00:00Z
---

# smritea SDK — Go


| Section | What it covers |
|---------|----------------|
| [Installation](#installation) | Requires Go 1.22+. |
| [Get your API key](#get-your-api-key) | Sign up at smritea.ai — free account, no credit card required |
| [Quickstart](#quickstart) | Quickstart |
| [Constructor](#constructor) | Constructor |
| [Methods](#methods) | Methods |
| [Error handling](#error-handling) | Error handling |
| [`Memory` type reference](#memory-type-reference) | Memory type reference |

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
    client.Add(ctx, "Alice is a vegetarian and loves hiking",
        smritea.NewAddOptions().WithScope(
            smritea.NewMemoryScope().WithActorID("alice").WithActorType("user")))

    // Retrieve it later
    results, _ := client.Search(ctx, "What are Alice's food preferences?",
        smritea.NewSearchOptions().WithScope(
            smritea.NewMemoryScope().WithActorID("alice").WithActorType("user")))
    for _, r := range results {
        fmt.Printf("%v  %v\n", r.Score, r.Memory.Content)
    }
}
```

---

## Constructor

```go
import smritea "github.com/SmrutAI/smritea-sdk/go"

client := smritea.NewClient(smritea.ClientConfig{
    APIKey:     "sk-...",                   // required
    AppID:      "app_...",                  // required
    BaseURL:    "https://api-us.smritea.ai",   // optional, default shown
    MaxRetries: 2,                          // optional, default 2
})
```

---

## Methods

### `Add` — Store a memory

```go
memory, err := client.Add(ctx, "User prefers concise replies",
    smritea.NewAddOptions().
        WithScope(smritea.NewMemoryScope().
            WithActorID("alice").              // explicit actor ID
            WithActorType("user").             // "user" | "agent" | "system"
            WithConversationID("conv_123")).   // optional
        WithMetadata(map[string]any{"source": "chat"}))   // optional
fmt.Println(memory.Id) // mem_...
```

| Parameter | Type | Default | Description |
|---|---|---|---|
| `content` | `string` | required | Memory text |
| `ActorID` | `*string` | `nil` | Actor ID |
| `ActorType` | `*string` | `nil` | `"user"` \| `"agent"` \| `"system"` |
| `ActorName` | `*string` | `nil` | Display name |
| `Metadata` | `map[string]any` | `nil` | Arbitrary key-value map |
| `ConversationID` | `*string` | `nil` | Conversation context |

---

### `Search` — Semantic search

```go
results, err := client.Search(ctx, "dietary restrictions",
    smritea.NewSearchOptions().
        WithScope(smritea.NewMemoryScope().WithActorID("alice").WithActorType("user")).
        WithLimit(5))
for _, r := range results {
    fmt.Println(r.Score, r.Memory.Content)
}
```

Results are ordered by relevance (descending). Each result exposes `Score` (0.0–1.0) and a `Memory` struct.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `string` | required | Search text |
| `ActorID` | `*string` | `nil` | Filter by actor ID |
| `ActorType` | `*string` | `nil` | Filter by actor type |
| `Limit` | `*int32` | app default | Max results to return |
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
results, _ := client.Search(ctx, "",
    smritea.NewSearchOptions().WithScope(
        smritea.NewMemoryScope().WithActorID("alice").WithActorType("user")).WithLimit(20))
```

`limit` must not exceed the server's `top_n_max` (default 20); a larger value returns a 400 error. Omit it to use the app's `top_n`.

---

## Error handling

```go
import (
    "errors"
    "fmt"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

results, err := client.Search(ctx, "preferences",
    smritea.NewSearchOptions().WithScope(
        smritea.NewMemoryScope().WithActorID("alice").WithActorType("user")))
if err != nil {
    var authErr *smritea.SmriteaUnauthorizedError
    var tooManyErr *smritea.SmriteaTooManyRequestsError
    var paymentErr *smritea.SmriteaPaymentRequiredError
    var smriteaErr *smritea.SmriteaError

    switch {
    case errors.As(err, &authErr):
        fmt.Println("Check your API key")
    case errors.As(err, &tooManyErr):
        fmt.Printf("Rate limited — retry after %ds\n", *tooManyErr.RetryAfter)
    case errors.As(err, &paymentErr):
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
| `SmriteaBadRequestError` | 400 | Invalid request parameters |
| `SmriteaUnauthorizedError` | 401 | Invalid or missing API key |
| `SmriteaPaymentRequiredError` | 402 | Organisation quota exceeded |
| `SmriteaForbiddenError` | 403 | Access denied |
| `SmriteaNotFoundError` | 404 | Memory ID does not exist |
| `SmriteaConflictError` | 409 | Conflicting resource state |
| `SmriteaUnprocessableError` | 422 | Request understood but cannot be processed |
| `SmriteaTooManyRequestsError` | 429 | Rate limit hit — check `.RetryAfter` |
| `SmriteaError` | other | Unexpected server error |

Every error type also carries `.Code` (machine-readable wire code), `.HTTPStatus`, and `.Retryable`
(parsed from the response body's optional `retryable` field, always `true` for 429).

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
| `Metadata` | map[string]any | Arbitrary key-value pairs |
| `ConversationId` | *string | Conversation context |
| `ActiveFrom` | string | ISO 8601 — when memory becomes valid |
| `ActiveTo` | *string | ISO 8601 — when memory expires |
| `CreatedAt` | string | ISO 8601 creation timestamp |
| `UpdatedAt` | string | ISO 8601 last update timestamp |
