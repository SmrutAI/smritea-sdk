# smritea SDK — TypeScript / Node.js

TypeScript SDK for the [smritea](https://smritea.ai) AI memory system.

**[Get a free API key →](https://smritea.ai)**

---

## Installation

```bash
npm install smritea-sdk
```

Requires Node.js 18+. Ships as ESM + CJS with bundled type declarations.

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

```typescript
import { SmriteaClient } from 'smritea-sdk';

const client = new SmriteaClient({
  apiKey: process.env.SMRITEA_API_KEY!,
  appId: process.env.SMRITEA_APP_ID!,
});

// Store something about a user
await client.add('Alice is a vegetarian and loves hiking', { actorId: 'alice', actorType: 'user' });

// Retrieve it later in a different session
const results = await client.search("What are Alice's food preferences?", { actorId: 'alice', actorType: 'user' });
for (const r of results) {
  console.log(r.score?.toFixed(2), r.content);
}
```

---

## Constructor

```typescript
import { SmriteaClient } from 'smritea-sdk';

const client = new SmriteaClient({
  apiKey: 'sk-...',                       // required
  appId: 'app_...',                       // required
  baseUrl: 'https://api.smritea.ai',      // optional, default shown
  maxRetries: 2,                          // optional, default 2; 0 disables retry
});
```

---

## Methods

All methods are async and return Promises.

### `add` — Store a memory

```typescript
const memory = await client.add('User prefers concise replies', {
  actorId: 'alice',
  actorType: 'user',
  metadata: { source: 'chat' },
  conversationId: 'conv_123',
});
console.log(memory.id); // mem_...
```

| Option | Default | Description |
|---|---|---|
| `actorId` | `undefined` | Actor ID |
| `actorType` | `undefined` | `"user"` \| `"agent"` \| `"system"` |
| `actorName` | `undefined` | Display name |
| `metadata` | `undefined` | Arbitrary key-value object |
| `conversationId` | `undefined` | Conversation context |

---

### `search` — Semantic search

```typescript
const results = await client.search('dietary restrictions', {
  actorId: 'alice',
  actorType: 'user',
  limit: 5,
  method: 'deep_search',  // "quick_search" | "deep_search" | "context_aware_search"
  threshold: 0.7,          // min relevance score 0.0–1.0
});
results.forEach(r => console.log(r.score, r.content));
```

Results are ordered by relevance (descending). Each result exposes `score` (0.0–1.0) and all `Memory` fields directly.

| Search method | When to use |
|---|---|
| `quick_search` | Low-latency, keyword + vector hybrid |
| `deep_search` | Higher recall, traverses the memory graph |
| `context_aware_search` | Reranks results using conversation context |

| Option | Default | Description |
|---|---|---|
| `actorId` | `undefined` | Filter by actor ID |
| `actorType` | `undefined` | Filter by actor type |
| `limit` | app default | Max results to return |
| `method` | app default | Search strategy |
| `threshold` | `undefined` | Min relevance score 0.0–1.0 |
| `graphDepth` | `undefined` | Graph traversal depth override |
| `conversationId` | `undefined` | Conversation context |

---

### `get` — Retrieve a memory by ID

```typescript
const memory = await client.get('mem_abc123');
console.log(memory.content, memory.createdAt);
// Throws SmriteaNotFoundError if the ID does not exist
```

---

### `delete` — Delete a memory by ID

```typescript
await client.delete('mem_abc123');
// Throws SmriteaNotFoundError if the ID does not exist
```

---

### `getAll` — List all memories

> **Not yet implemented.** `getAll()` throws an `Error` when called.
> Use `search()` with a broad query as a workaround:

```typescript
const results = await client.search('', { actorId: 'alice', actorType: 'user', limit: 100 });
```

---

## Error handling

```typescript
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
  if (e instanceof SmriteaQuotaError) console.log('Plan quota exceeded');
}
```

| Exception | HTTP | When |
|---|---|---|
| `SmriteaAuthError` | 401 | Invalid or missing API key |
| `SmriteaValidationError` | 400 | Invalid request parameters |
| `SmriteaNotFoundError` | 404 | Memory ID does not exist |
| `SmriteaQuotaError` | 402 | Organisation quota exceeded |
| `SmriteaRateLimitError` | 429 | Rate limit hit — check `.retryAfter` |
| `SmriteaError` | other | Unexpected server error |

---

## `Memory` type reference

All fields use `camelCase`.

| Field | Type | Description |
|---|---|---|
| `id` | `string` | Memory ID (`mem_...`) |
| `appId` | `string` | App this memory belongs to |
| `content` | `string` | Memory text |
| `actorId` | `string` | Actor who owns this memory |
| `actorType` | `string` | `"user"` \| `"agent"` \| `"system"` |
| `actorName` | `string?` | Display name |
| `metadata` | `object?` | Arbitrary key-value pairs |
| `conversationId` | `string?` | Conversation context |
| `conversationMessageId` | `string?` | Message within the conversation |
| `activeFrom` | `string` | ISO 8601 — when memory becomes valid |
| `activeTo` | `string?` | ISO 8601 — when memory expires |
| `createdAt` | `string` | ISO 8601 creation timestamp |
| `updatedAt` | `string` | ISO 8601 last update timestamp |
