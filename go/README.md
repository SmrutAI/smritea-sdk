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
    TraceID:    "checkout-flow-7f3a",       // optional; 8-100 chars of A-Z, a-z, 0-9, '.', '_', ':', '-'
    MaxRetries: 2,                          // optional, default 2
})
```

`TraceID` (optional) is sent as the `X-Trace-ID` header on every request, including retries. It must be
8-100 bytes long and contain only `A-Z`, `a-z`, `0-9`, `.`, `_`, `:`, or `-`.

An invalid `TraceID` does not stop the constructor: `NewClient` still returns a client, but every method
returns a `*SmriteaError` with `Code` `"INVALID_TRACE_ID"` and sends no request.

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
    var badRequestErr *smritea.SmriteaBadRequestError
    var authErr *smritea.SmriteaUnauthorizedError
    var paymentErr *smritea.SmriteaPaymentRequiredError
    var forbiddenErr *smritea.SmriteaForbiddenError
    var notFoundErr *smritea.SmriteaNotFoundError
    var conflictErr *smritea.SmriteaConflictError
    var unprocessableErr *smritea.SmriteaUnprocessableError
    var tooManyErr *smritea.SmriteaTooManyRequestsError
    var smriteaErr *smritea.SmriteaError

    // Typed errors embed SmriteaError by value and do not unwrap to it, so each
    // typed error must be matched first. *smritea.SmriteaError only matches the
    // base error returned for statuses without a dedicated type (e.g. 5xx).
    switch {
    case errors.As(err, &badRequestErr):
        fmt.Printf("Invalid request: %s\n", badRequestErr.Message)
    case errors.As(err, &authErr):
        fmt.Println("Check your API key")
    case errors.As(err, &paymentErr):
        fmt.Println("Plan quota exceeded")
    case errors.As(err, &forbiddenErr):
        fmt.Println("Access denied")
    case errors.As(err, &notFoundErr):
        fmt.Printf("Not found: %s\n", notFoundErr.Message)
    case errors.As(err, &conflictErr):
        fmt.Println("Conflicting resource state")
    case errors.As(err, &unprocessableErr):
        fmt.Printf("Cannot process request: %s\n", unprocessableErr.Message)
    case errors.As(err, &tooManyErr):
        if tooManyErr.RetryAfter != nil {
            fmt.Printf("Rate limited — retry after %ds\n", *tooManyErr.RetryAfter)
        } else {
            fmt.Println("Rate limited — no Retry-After header")
        }
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

`RequestID` holds the server's request ID for the failed call: the response body's `request_id` when present,
otherwise the `X-Request-ID` response header. It is empty when neither is returned.

Typed errors such as `*SmriteaNotFoundError` embed the base `SmriteaError`, so `RequestID` is available on them
as a promoted field. `errors.As` into `*smritea.SmriteaError` matches only the base error (for example, a status
without a dedicated type), so check the typed errors first:

```go
_, err := client.Get(ctx, "mem_missing")

var notFound *smritea.SmriteaNotFoundError
var se *smritea.SmriteaError
switch {
case errors.As(err, &notFound):
    fmt.Println("request id:", notFound.RequestID)
case errors.As(err, &se):
    fmt.Println("request id:", se.RequestID)
}
```

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
