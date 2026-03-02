# smritea SDK

Give your AI apps a long-term memory. Store what users tell you, search it back when relevant, and build personalized
experiences — without managing vector databases yourself.

**[Get a free API key →](https://smritea.ai)**

---

## Packages

| Language             | Package                   | Docs                                         |
|----------------------|---------------------------|----------------------------------------------|
| Python               | `pip install smritea-sdk` | [python/README.md](python/README.md)         |
| TypeScript / Node.js | `npm install smritea-sdk` | [typescript/README.md](typescript/README.md) |

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

| Parameter (Python) | Parameter (TypeScript) | Default  | Description                             |
|--------------------|------------------------|----------|-----------------------------------------|
| `content`          | `content`              | required | Memory text                             |
| `user_id`          | `userId`               | `None`   | Shorthand: actor_id + actor_type="user" |
| `actor_id`         | `actorId`              | `None`   | Explicit actor ID                       |
| `actor_type`       | `actorType`            | `"user"` | `"user"` \| `"agent"` \| `"system"`     |
| `actor_name`       | `actorName`            | `None`   | Display name                            |
| `metadata`         | `metadata`             | `None`   | Arbitrary key-value dict / object       |
| `conversation_id`  | `conversationId`       | `None`   | Conversation context                    |

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

| Parameter (Python) | Parameter (TypeScript) | Default     | Description                    |
|--------------------|------------------------|-------------|--------------------------------|
| `query`            | `query`                | required    | Search text                    |
| `user_id`          | `userId`               | `None`      | Filter to this user's memories |
| `actor_id`         | `actorId`              | `None`      | Filter by actor ID             |
| `actor_type`       | `actorType`            | `None`      | Filter by actor type           |
| `limit`            | `limit`                | app default | Max results to return          |
| `method`           | `method`               | app default | Search strategy                |
| `threshold`        | `threshold`            | `None`      | Min relevance score 0.0–1.0    |
| `graph_depth`      | `graphDepth`           | `None`      | Graph traversal depth override |
| `conversation_id`  | `conversationId`       | `None`      | Conversation context           |

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

> **Not yet implemented.**
>
> `get_all()` raises `NotImplementedError` in Python. There is no equivalent method in TypeScript.
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

| Exception                | HTTP  | When                                                  |
|--------------------------|-------|-------------------------------------------------------|
| `SmriteaAuthError`       | 401   | Invalid or missing API key                            |
| `SmriteaValidationError` | 400   | Invalid request parameters                            |
| `SmriteaNotFoundError`   | 404   | Memory ID does not exist                              |
| `SmriteaQuotaError`      | 402   | Organisation quota exceeded                           |
| `SmriteaRateLimitError`  | 429   | Rate limit hit — check `.retry_after` / `.retryAfter` |
| `SmriteaError`           | other | Unexpected server error                               |

---

## `Memory` type reference

| Field (Python)    | Field (TypeScript) | Type    | Description                          |
|-------------------|--------------------|---------|--------------------------------------|
| `id`              | `id`               | string  | Memory ID (`mem_...`)                |
| `app_id`          | `appId`            | string  | App this memory belongs to           |
| `content`         | `content`          | string  | Memory text                          |
| `actor_id`        | `actorId`          | string  | Actor who owns this memory           |
| `actor_type`      | `actorType`        | string  | `"user"` \| `"agent"` \| `"system"`  |
| `actor_name`      | `actorName`        | string? | Display name                         |
| `metadata`        | `metadata`         | object? | Arbitrary key-value pairs            |
| `conversation_id` | `conversationId`   | string? | Conversation context                 |
| `active_from`     | `activeFrom`       | string  | ISO 8601 — when memory becomes valid |
| `active_to`       | `activeTo`         | string? | ISO 8601 — when memory expires       |
| `created_at`      | `createdAt`        | string  | ISO 8601 creation timestamp          |
| `updated_at`      | `updatedAt`        | string  | ISO 8601 last update timestamp       |

> Python fields use `snake_case`; TypeScript fields use `camelCase`.

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
make test             # both

# Build distribution artifacts
make build-python       # python/dist/smritea-*.whl
make build-typescript   # typescript/dist/

# Publish (tokens from environment variables)
PYPI_TOKEN=pypi-... make publish-python
NPM_TOKEN=npm_...  make publish-typescript
```
