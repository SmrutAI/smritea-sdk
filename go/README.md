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
2. Create an app in the dashboard and copy your API key (`smrt_<prefix>_<secret>`) and App ID (`app_...`)
3. Export them as environment variables:

```bash
export SMRITEA_API_KEY="smrt_<prefix>_<secret>"
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

func newClient() *smritea.SmriteaClient {
    return smritea.NewClient(smritea.ClientConfig{
        APIKey:     "smrt_<prefix>_<secret>",   // required
        AppID:      "app_...",                  // required
        BaseURL:    "https://api-us.smritea.ai",   // optional, default shown
        TraceID:    "checkout-flow-7f3a",       // optional; 8-100 chars of A-Z, a-z, 0-9, '.', '_', ':', '-'
        MaxRetries: smritea.Int(2),             // optional; nil = default 2; smritea.Int(0) disables retry; a negative value counts as 0
    })
}
```

`TraceID` (optional) is sent as the `X-Trace-ID` header on every request, including retries. It must be
8-100 bytes long and contain only `A-Z`, `a-z`, `0-9`, `.`, `_`, `:`, or `-`.

An invalid `TraceID` does not stop the constructor: `NewClient` still returns a client, but every method
returns a `*SmriteaBadRequestError` (HTTP 400) with `Code` `"INVALID_TRACE_ID"` (constant
`smritea.ErrCodeInvalidTraceID`) and sends no request. `errors.As` to `*SmriteaError` does not match it; use
`*SmriteaBadRequestError` instead.

`HTTPClient` (optional, `*http.Client`) sends every request: set your own transport, proxy, TLS or timeout.
`nil` uses `http.DefaultClient`.

---

## Methods

### `Add` — Store a memory

```go
import (
    "context"
    "fmt"
    "log"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

func addMemory(ctx context.Context, client *smritea.SmriteaClient) {
    result, err := client.Add(ctx, "User prefers concise replies",
        smritea.NewAddOptions().
            WithScope(smritea.NewMemoryScope().
                WithActorID("alice").              // explicit actor ID
                WithActorType("user").             // "user" | "agent" | "system"
                WithConversationID("conv_123")).   // optional
            WithMetadata(map[string]any{"source": "chat"}))   // optional
    if err != nil {
        log.Fatal(err)
    }
    fmt.Println(result.Memories[0].Id) // mem_...
}
```

| `AddOptions` method | Type | Default | Description |
|---|---|---|---|
| `content` | `string` | required | Memory text |
| `WithScope(v)` | `*MemoryScope` | `nil` | Actor and conversation context; see the scope methods below |
| `WithMetadata(v)` | `map[string]any` | `nil` | Arbitrary key-value map |
| `WithEventOccurredAt(v)` | `string` | `nil` | RFC 3339 time the event in the text happened |
| `WithRelativeStanding(v)` | `*RelativeStanding` | `nil` | Importance and decay configuration |

`NewMemoryScope()` sets the scope fields through chained methods. Every field is optional.

| `MemoryScope` method | Type | Description |
|---|---|---|
| `WithActorID(v)` | `string` | Actor ID. Pair it with `WithActorType`. |
| `WithActorType(v)` | `string` | `"user"` \| `"agent"` \| `"system"` |
| `WithActorName(v)` | `string` | Display name |
| `WithConversationID(v)` | `string` | Conversation context. Cannot be combined with participant IDs. |
| `WithSourceType(v)` | `string` | `"conversation"` \| `"document"` \| `"api"` |
| `WithParticipantIDs(v)` | `[]string` | Search only: every listed actor took part. Cannot be combined with a conversation ID. |

---

### `Search` — Semantic search

```go
import (
    "context"
    "fmt"
    "log"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

func searchMemories(ctx context.Context, client *smritea.SmriteaClient) {
    results, err := client.Search(ctx, "dietary restrictions",
        smritea.NewSearchOptions().
            WithScope(smritea.NewMemoryScope().WithActorID("alice").WithActorType("user")).
            WithLimit(5))
    if err != nil {
        log.Fatal(err)
    }
    for _, r := range results {
        fmt.Println(r.Score, r.Memory.Content)
    }
}
```

Results are ordered by relevance (descending). Each result exposes `Score` (0.0–1.0) and a `Memory` struct.

| `SearchOptions` method | Type | Default | Description |
|---|---|---|---|
| `query` | `string` | required | Search text |
| `WithScope(v)` | `*MemoryScope` | `nil` | Filter by actor, conversation, source type or participants |
| `WithLimit(v)` | `int32` | app default | Max results to return; at most the server's `top_n_max` |
| `WithGraphDepth(v)` | `int32` | `nil` | Graph traversal depth, 1 to 5 |
| `WithFromTime(v)` | `string` | `nil` | RFC 3339 start of a time range. Cannot be combined with `WithValidAt`. |
| `WithToTime(v)` | `string` | `nil` | RFC 3339 end of a time range. Cannot be combined with `WithValidAt`. |
| `WithValidAt(v)` | `string` | `nil` | RFC 3339 moment; returns memories valid at that moment |
| `WithMethod(v)` | `string` | app default | `"quick_search"` \| `"deep_search"` \| `"context_aware_search"` |
| `WithRerankerType(v)` | `string` | app default | Applies to `deep_search` only |
| `WithMetadataFilter(v)` | `map[string]any` | `nil` | Operator filter over memory metadata |
| `WithSpeakerActorID(v)` | `string` | `nil` | Who is asking. Never filters results. |

---

### `Get` — Retrieve a memory by ID

```go
import (
    "context"
    "fmt"
    "log"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

func getMemory(ctx context.Context, client *smritea.SmriteaClient) {
    memory, err := client.Get(ctx, "mem_abc123")
    // Returns SmriteaNotFoundError if the ID does not exist
    if err != nil {
        log.Fatal(err)
    }
    fmt.Println(memory.Content, memory.CreatedAt)
}
```

---

### `Delete` — Delete a memory by ID

```go
import (
    "context"
    "log"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

func deleteMemory(ctx context.Context, client *smritea.SmriteaClient) {
    err := client.Delete(ctx, "mem_abc123")
    // Returns SmriteaNotFoundError if the ID does not exist
    if err != nil {
        log.Fatal(err)
    }
}
```

---

### `GetAll` — List all memories

> **Not yet implemented.** Returns an error.
> Use `Search()` with a broad query as a workaround:

```go
import (
    "context"
    "log"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

func listMemories(ctx context.Context, client *smritea.SmriteaClient) {
    results, err := client.Search(ctx, "",
        smritea.NewSearchOptions().WithScope(
            smritea.NewMemoryScope().WithActorID("alice").WithActorType("user")).WithLimit(20))
    if err != nil {
        log.Fatal(err)
    }
    log.Println(len(results))
}
```

`limit` must not exceed the server's `top_n_max` (default 20); a larger value returns a 400 error. Omit it to use the app's `top_n`.

---

## Error handling

```go
import (
    "context"
    "errors"
    "fmt"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

func handleSearchError(ctx context.Context, client *smritea.SmriteaClient) {
    _, err := client.Search(ctx, "preferences",
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
import (
    "context"
    "errors"
    "fmt"

    smritea "github.com/SmrutAI/smritea-sdk/go"
)

func printRequestID(ctx context.Context, client *smritea.SmriteaClient) {
    _, err := client.Get(ctx, "mem_missing")

    var notFound *smritea.SmriteaNotFoundError
    var se *smritea.SmriteaError
    switch {
    case errors.As(err, &notFound):
        fmt.Println("request id:", notFound.RequestID)
    case errors.As(err, &se):
        fmt.Println("request id:", se.RequestID)
    }
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
