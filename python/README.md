---
type: Overview
title: smritea SDK — Python
status: stable
tags:
- readme
stale_after: 2026-12-31
generated:
  by: Tushar Dwivedi
  at: 2026-08-25T00:00:00Z
---

# smritea SDK — Python


| Section | What it covers |
|---------|----------------|
| [Installation](#installation) | Requires Python 3.9+. |
| [Get your API key](#get-your-api-key) | Sign up at smritea.ai — free account, no credit card required |
| [Quickstart](#quickstart) | Quickstart |
| [Constructor](#constructor) | Constructor |
| [Methods](#methods) | Methods |
| [Error handling](#error-handling) | Error handling |
| [`Memory` type reference](#memory-type-reference) | Memory type reference |

Python SDK for the [smritea](https://smritea.ai) AI memory system.

**[Get a free API key →](https://smritea.ai)**

---

## Installation

```bash
pip install smritea-sdk
```

Requires Python 3.9+.

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

```python
import os
from smritea import SmriteaClient

client = SmriteaClient(
    api_key=os.environ["SMRITEA_API_KEY"],
    app_id=os.environ["SMRITEA_APP_ID"],
)

# Store something about a user
client.add("Alice is a vegetarian and loves hiking", actor_id="alice", actor_type="user")

# Retrieve it later in a different session
results = client.search("What are Alice's food preferences?", actor_id="alice", actor_type="user")
for r in results:
    print(f"{r.score:.2f}  {r.content}")
```

---

## Constructor

```python
from smritea import SmriteaClient

client = SmriteaClient(
    api_key="sk-...",                    # required
    app_id="app_...",                    # required
    base_url="https://api-us.smritea.ai",  # optional, default shown
    max_retries=2,                       # optional, default 2; 0 disables retry
)
```

---

## Methods

### `add` — Store a memory

```python
memory = client.add(
    "User prefers concise replies",
    actor_id="alice",              # explicit actor ID
    actor_type="user",             # "user" | "agent" | "system"
    metadata={"source": "chat"},   # optional
    conversation_id="conv_123",    # optional
)
print(memory.id)  # mem_...
```

| Parameter | Default | Description |
|---|---|---|
| `content` | required | Memory text |
| `actor_id` | `None` | Actor ID |
| `actor_type` | `None` | `"user"` \| `"agent"` \| `"system"` |
| `actor_name` | `None` | Display name |
| `metadata` | `None` | Arbitrary key-value dict |
| `conversation_id` | `None` | Conversation context |

---

### `search` — Semantic search

```python
results = client.search(
    "dietary restrictions",
    actor_id="alice",
    actor_type="user",
    limit=5,
)
for r in results:
    print(r.score, r.content)
```

Results are ordered by relevance (descending). Each result exposes `score` (0.0–1.0) and all `Memory` fields directly.

| Parameter | Default | Description |
|---|---|---|
| `query` | required | Search text |
| `actor_id` | `None` | Filter by actor ID |
| `actor_type` | `None` | Filter by actor type |
| `limit` | app default | Max results to return |
| `graph_depth` | `None` | Graph traversal depth override |
| `conversation_id` | `None` | Conversation context |

---

### `get` — Retrieve a memory by ID

```python
memory = client.get("mem_abc123")
print(memory.content, memory.created_at)
# Raises SmriteaNotFoundError if the ID does not exist
```

---

### `delete` — Delete a memory by ID

```python
client.delete("mem_abc123")
# Raises SmriteaNotFoundError if the ID does not exist
```

---

### `get_all` — List all memories

> **Not yet implemented.** Raises `NotImplementedError`.
> Use `search()` with a broad query as a workaround:

```python
results = client.search("", actor_id="alice", actor_type="user", limit=20)
```

`limit` must not exceed the server's `top_n_max` (default 20); a larger value returns a 400 error. Omit it to use the app's `top_n`.

---

## Error handling

```python
from smritea import (
    SmriteaClient,
    SmriteaUnauthorizedError,
    SmriteaNotFoundError,
    SmriteaTooManyRequestsError,
    SmriteaPaymentRequiredError,
    SmriteaBadRequestError,
    SmriteaError,
)

try:
    results = client.search("preferences", actor_id="alice", actor_type="user")
except SmriteaUnauthorizedError:
    print("Check your API key")
except SmriteaTooManyRequestsError as e:
    print(f"Rate limited — retry after {e.retry_after}s")
except SmriteaPaymentRequiredError:
    print("Plan quota exceeded")
except SmriteaError as e:
    print(f"Unexpected error: {e}")
```

| Exception | HTTP | When |
|---|---|---|
| `SmriteaBadRequestError` | 400 | Invalid request parameters |
| `SmriteaUnauthorizedError` | 401 | Invalid or missing API key |
| `SmriteaPaymentRequiredError` | 402 | Organisation quota exceeded |
| `SmriteaForbiddenError` | 403 | Caller is not permitted to perform this action |
| `SmriteaNotFoundError` | 404 | Memory ID does not exist |
| `SmriteaConflictError` | 409 | Request conflicts with the current state of the resource |
| `SmriteaUnprocessableError` | 422 | Request was well-formed but semantically invalid |
| `SmriteaTooManyRequestsError` | 429 | Rate limit hit — check `.retry_after` |
| `SmriteaError` | other | Unexpected server error |

Every exception carries `message`, `http_status`, `code` (the server's stable machine-readable wire
code), `retryable` (`True` when the server marked the error safe to retry), and the full parsed response
`body`.

---

## `Memory` type reference

| Field | Type | Description |
|---|---|---|
| `id` | str | Memory ID (`mem_...`) |
| `app_id` | str | App this memory belongs to |
| `content` | str | Memory text |
| `actor_id` | str | Actor who owns this memory |
| `actor_type` | str | `"user"` \| `"agent"` \| `"system"` |
| `actor_name` | str \| None | Display name |
| `metadata` | dict \| None | Arbitrary key-value pairs |
| `conversation_id` | str \| None | Conversation context |
| `active_from` | str | ISO 8601 — when memory becomes valid |
| `active_to` | str \| None | ISO 8601 — when memory expires |
| `created_at` | str | ISO 8601 creation timestamp |
| `updated_at` | str | ISO 8601 last update timestamp |
