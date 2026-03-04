# smritea SDK

Give your AI apps a long-term memory. Store what users tell you, search it back when relevant, and build personalized
experiences — without managing vector databases yourself.

**[Get a free API key →](https://smritea.ai)**

---

## Packages

| Language             | Package                                               | Docs                                         |
|----------------------|-------------------------------------------------------|----------------------------------------------|
| Python               | `pip install smritea-sdk`                             | [python/README.md](python/README.md)         |
| TypeScript / Node.js | `npm install smritea-sdk`                             | [typescript/README.md](typescript/README.md) |
| Go                   | `go get github.com/SmrutAI/smritea-sdk/go`            | [go/README.md](go/README.md)                 |
| Java                 | `ai.smritea:smritea-sdk` (Maven Central)              | [java/README.md](java/README.md)             |
| C#                   | `dotnet add package Smritea.Sdk`                      | [csharp/README.md](csharp/README.md)         |

---

## Get your API key

1. Sign up at **[smritea.ai](https://smritea.ai)** — free tier available
2. Create an app in the dashboard and copy your API key (`sk-...`) and App ID (`app_...`)
3. Export them as environment variables:

```bash
export SMRITEA_API_KEY="sk-..."
export SMRITEA_APP_ID="app_..."
```

---

## Quickstart

**Python**

```python
import os
from smritea import SmriteaClient

client = SmriteaClient(
    api_key=os.environ["SMRITEA_API_KEY"],
    app_id=os.environ["SMRITEA_APP_ID"],
)

# Store something about a user
client.add("Alice is a vegetarian and loves hiking", user_id="alice")

# Retrieve it later in a different session
results = client.search("What are Alice's food preferences?", user_id="alice")
for r in results:
    print(f"{r.score:.2f}  {r.content}")
```

**TypeScript**

```typescript
import { SmriteaClient } from 'smritea-sdk';

const client = new SmriteaClient({
  apiKey: process.env.SMRITEA_API_KEY!,
  appId: process.env.SMRITEA_APP_ID!,
});

// Store something about a user
await client.add('Alice is a vegetarian and loves hiking', { userId: 'alice' });

// Retrieve it later
const results = await client.search("What are Alice's food preferences?", { userId: 'alice' });
for (const r of results) {
  console.log(r.score?.toFixed(2), r.content);
}
```

**Go**

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

**Java**

```java
import ai.smritea.sdk.SmriteaClient;
import ai.smritea.sdk.model.*;

SmriteaClient client = new SmriteaClient(
    System.getenv("SMRITEA_API_KEY"),
    System.getenv("SMRITEA_APP_ID")
);

// Store something about a user
Memory mem = client.add("Alice is a vegetarian and loves hiking",
    new AddOptions().withUserId("alice"));

// Retrieve it later
List<SearchResult> results = client.search("What are Alice's food preferences?",
    new SearchOptions().withUserId("alice"));
for (SearchResult r : results) {
    System.out.printf("%.2f  %s%n", r.getScore(), r.getContent());
}
```

**C#**

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

## Methods

### `add` — Store a memory

```python
# Python
memory = client.add(
    "User prefers concise replies",
    user_id="alice",  # shorthand for actor_id + actor_type="user"
    metadata={"source": "chat"},  # optional arbitrary key-value pairs
    conversation_id="conv_123",  # optional — links memory to a conversation
)
print(memory.id)  # mem_...
```

```typescript
// TypeScript
const memory = await client.add('User prefers concise replies', {
  userId: 'alice',
  metadata: { source: 'chat' },
  conversationId: 'conv_123',
});
console.log(memory.id); // mem_...
```

`user_id` / `userId` is a shorthand that sets `actor_id` and forces `actor_type="user"`. For agent or system memories,
use `actor_id` + `actor_type` directly.

**Full signature**

| Parameter (Python) | Parameter (TypeScript) | Parameter (Go) | Parameter (Java) | Parameter (C#) | Default  | Description                             |
|--------------------|------------------------|----------------|------------------|----------------|----------|-----------------------------------------|
| `content`          | `content`              | `content`      | `content`        | `content`      | required | Memory text                             |
| `user_id`          | `userId`               | `UserID`       | `userId`         | `UserId`       | `None`   | Shorthand: actor_id + actor_type="user" |
| `actor_id`         | `actorId`              | `ActorID`      | `actorId`        | `ActorId`      | `None`   | Explicit actor ID                       |
| `actor_type`       | `actorType`            | `ActorType`    | `actorType`      | `ActorType`    | `"user"` | `"user"` \| `"agent"` \| `"system"`     |
| `actor_name`       | `actorName`            | `ActorName`    | `actorName`      | `ActorName`    | `None`   | Display name                            |
| `metadata`         | `metadata`             | `Metadata`     | `metadata`       | `Metadata`     | `None`   | Arbitrary key-value dict / object       |
| `conversation_id`  | `conversationId`       | `ConversationID` | `conversationId` | `ConversationId` | `None` | Conversation context                    |

---

### `search` — Semantic search

```python
# Python
results = client.search(
    "dietary restrictions",
    user_id="alice",
    limit=5,
    method="deep_search",   # optional: "quick_search" | "deep_search" | "context_aware_search"
    threshold=0.7,           # optional: min relevance score (0.0–1.0)
)
for r in results:
    print(r.score, r.content)
```

```typescript
// TypeScript
const results = await client.search('dietary restrictions', {
  userId: 'alice',
  limit: 5,
  method: 'deep_search',
  threshold: 0.7,
});
results.forEach(r => console.log(r.score, r.content));
```

Results are ordered by relevance (descending). Each result exposes `score` (float 0–1) and all `Memory` fields directly.

**Search methods**

| Method                 | When to use                                |
|------------------------|--------------------------------------------|
| `quick_search`         | Low-latency, keyword + vector hybrid       |
| `deep_search`          | Higher recall, traverses the memory graph  |
| `context_aware_search` | Reranks results using conversation context |

**Full signature**

| Parameter (Python) | Parameter (TypeScript) | Parameter (Go) | Parameter (Java) | Parameter (C#) | Default     | Description                    |
|--------------------|------------------------|----------------|------------------|----------------|-------------|--------------------------------|
| `query`            | `query`                | `query`        | `query`          | `query`        | required    | Search text                    |
| `user_id`          | `userId`               | `UserID`       | `userId`         | `UserId`       | `None`      | Filter to this user's memories |
| `actor_id`         | `actorId`              | `ActorID`      | `actorId`        | `ActorId`      | `None`      | Filter by actor ID             |
| `actor_type`       | `actorType`            | `ActorType`    | `actorType`      | `ActorType`    | `None`      | Filter by actor type           |
| `limit`            | `limit`                | `Limit`        | `limit`          | `Limit`        | app default | Max results to return          |
| `method`           | `method`               | `Method`       | `method`         | `Method`       | app default | Search strategy                |
| `threshold`        | `threshold`            | `Threshold`    | `threshold`      | `Threshold`    | `None`      | Min relevance score 0.0–1.0    |
| `graph_depth`      | `graphDepth`           | `GraphDepth`   | `graphDepth`     | `GraphDepth`   | `None`      | Graph traversal depth override |
| `conversation_id`  | `conversationId`       | `ConversationID` | `conversationId` | `ConversationId` | `None`   | Conversation context           |

---

### `get` — Retrieve a memory by ID

```python
# Python
memory = client.get("mem_abc123")
print(memory.content, memory.created_at)
```

```typescript
// TypeScript
const memory = await client.get('mem_abc123');
console.log(memory.content, memory.createdAt);
```

Raises / throws `SmriteaNotFoundError` if the ID does not exist.

---

### `delete` — Delete a memory by ID

```python
# Python
client.delete("mem_abc123")
```

```typescript
// TypeScript
await client.delete('mem_abc123');
```

Raises / throws `SmriteaNotFoundError` if the ID does not exist.

---

### `get_all` — List all memories

> **Not yet implemented** across all SDKs.
>
> - Python: `get_all()` raises `NotImplementedError`
> - TypeScript: no equivalent method
> - Go: `GetAll()` returns an error
> - Java: `getAll()` throws `UnsupportedOperationException`
> - C#: `GetAllAsync()` throws `NotImplementedException`
>
> The list memories endpoint is pending. Use `search()` with a broad query as a workaround.

```python
# Workaround until get_all is available
results = client.search("", user_id="alice", limit=100)
```

---

## Error handling

All methods raise typed exceptions that map directly to HTTP status codes.

```python
# Python
from smritea import (
    SmriteaClient,
    SmriteaAuthError,
    SmriteaNotFoundError,
    SmriteaRateLimitError,
    SmriteaQuotaError,
    SmriteaValidationError,
    SmriteaError,
)

try:
    results = client.search("preferences", user_id="alice")
except SmriteaAuthError:
    print("Check your API key")
except SmriteaRateLimitError as e:
    print(f"Rate limited — retry after {e.retry_after}s")
except SmriteaQuotaError:
    print("Plan quota exceeded")
except SmriteaError as e:
    print(f"Unexpected error: {e}")
```

```typescript
// TypeScript
import {
  SmriteaAuthError,
  SmriteaNotFoundError,
  SmriteaRateLimitError,
  SmriteaQuotaError,
  SmriteaValidationError,
  SmriteaError,
} from 'smritea-sdk';

try {
  await client.delete('mem_unknown');
} catch (e) {
  if (e instanceof SmriteaNotFoundError) console.log('Memory not found');
  if (e instanceof SmriteaRateLimitError) console.log(`Retry after ${e.retryAfter}s`);
  if (e instanceof SmriteaAuthError) console.log('Invalid API key');
}
```

**Exception reference**

| Exception (Python/TS/Go/Java) | Exception (C#)              | HTTP  | When                                                  |
|-------------------------------|-----------------------------|-------|-------------------------------------------------------|
| `SmriteaAuthError`            | `SmriteaAuthException`      | 401   | Invalid or missing API key                            |
| `SmriteaValidationError`      | `SmriteaValidationException`| 400   | Invalid request parameters                            |
| `SmriteaNotFoundError`        | `SmriteaNotFoundException`  | 404   | Memory ID does not exist                              |
| `SmriteaQuotaError`           | `SmriteaQuotaException`     | 402   | Organisation quota exceeded                           |
| `SmriteaRateLimitError`       | `SmriteaRateLimitException` | 429   | Rate limit hit — check `.retry_after` / `.retryAfter` / `.RetryAfter` |
| `SmriteaError`                | `SmriteaException`          | other | Unexpected server error                               |

---

## `Memory` type reference

| Field (Python)    | Field (TypeScript) | Field (Go)     | Field (Java)         | Field (C#)     | Type    | Description                          |
|-------------------|--------------------|----------------|----------------------|----------------|---------|--------------------------------------|
| `id`              | `id`               | `Id`           | `getId()`            | `Id`           | string  | Memory ID (`mem_...`)                |
| `app_id`          | `appId`            | `AppId`        | `getAppId()`         | `AppId`        | string  | App this memory belongs to           |
| `content`         | `content`          | `Content`      | `getContent()`       | `Content`      | string  | Memory text                          |
| `actor_id`        | `actorId`          | `ActorId`      | `getActorId()`       | `ActorId`      | string  | Actor who owns this memory           |
| `actor_type`      | `actorType`        | `ActorType`    | `getActorType()`     | `ActorType`    | string  | `"user"` \| `"agent"` \| `"system"`  |
| `actor_name`      | `actorName`        | `ActorName`    | `getActorName()`     | `ActorName`    | string? | Display name                         |
| `metadata`        | `metadata`         | `Metadata`     | `getMetadata()`      | `Metadata`     | object? | Arbitrary key-value pairs            |
| `conversation_id` | `conversationId`   | `ConversationId` | `getConversationId()` | `ConversationId` | string? | Conversation context                 |
| `active_from`     | `activeFrom`       | `ActiveFrom`   | `getActiveFrom()`    | `ActiveFrom`   | string  | ISO 8601 — when memory becomes valid |
| `active_to`       | `activeTo`         | `ActiveTo`     | `getActiveTo()`      | `ActiveTo`     | string? | ISO 8601 — when memory expires       |
| `created_at`      | `createdAt`        | `CreatedAt`    | `getCreatedAt()`     | `CreatedAt`    | string  | ISO 8601 creation timestamp          |
| `updated_at`      | `updatedAt`        | `UpdatedAt`    | `getUpdatedAt()`     | `UpdatedAt`    | string  | ISO 8601 last update timestamp       |

> Python fields use `snake_case`; TypeScript fields use `camelCase`; Go and C# fields use `PascalCase`; Java uses `getFieldName()` getter methods.

---

## Constructor

**Python**

```python
from smritea import SmriteaClient

client = SmriteaClient(
    api_key="sk-...",  # required
    app_id="app_...",  # required
    base_url="https://api.smritea.ai",  # optional, default shown
)
```

**TypeScript**

```typescript
import { SmriteaClient } from 'smritea-sdk';

const client = new SmriteaClient({
  apiKey: 'sk-...',                       // required
  appId: 'app_...',                       // required
  baseUrl: 'https://api.smritea.ai',      // optional, default shown
});
```

---

## Development

```bash
# Regenerate auto-gen SDK layer (run after API changes in smritea-cloud)
make generate

# Tests
make test-python
make test-typescript
make test-go
make test-java
make test-csharp
make test             # all

# Build distribution artifacts
make build-python       # python/dist/smritea-*.whl
make build-typescript   # typescript/dist/
make build-java         # java/target/smritea-sdk-*.jar
make build-csharp       # csharp/bin/Release/Smritea.Sdk.*.nupkg

# Publish (tokens from environment variables)
PYPI_TOKEN=pypi-... make publish-python
NPM_TOKEN=npm_...  make publish-typescript
make publish-java     # requires SONATYPE_USERNAME + SONATYPE_PASSWORD
make publish-csharp   # requires NUGET_API_KEY
```
