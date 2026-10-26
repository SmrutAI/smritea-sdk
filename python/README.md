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
| [MemoryScope](#memoryscope) | Actor and conversation context passed as `scope=` |
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
from smritea import MemoryScope, SmriteaClient

client = SmriteaClient(
    api_key=os.environ["SMRITEA_API_KEY"],
    app_id=os.environ["SMRITEA_APP_ID"],
)

alice = MemoryScope(actor_id="alice", actor_type="user")

# Store something about a user
client.add("Alice is a vegetarian and loves hiking", scope=alice)

# Retrieve it later in a different session
results = client.search("What are Alice's food preferences?", scope=alice)
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
    trace_id="checkout-flow-7f3a9c",     # optional, default None
)
```

| Parameter | Default | Description |
|---|---|---|
| `api_key` | required | Your API key (starts with `sk-`) |
| `app_id` | required | The app ID used for all memory operations |
| `base_url` | `https://api-us.smritea.ai` | Override the API base URL |
| `max_retries` | `2` | Automatic retries on HTTP 429. Uses the `Retry-After` header when present, otherwise exponential backoff with jitter (capped at 30 s). `0` disables retry. |
| `trace_id` | `None` | Trace ID sent as the `X-Trace-ID` header on every request. Must be 8-100 characters of `A-Z`, `a-z`, `0-9`, `.`, `_`, `:` or `-`. An invalid value raises `ValueError` at construction. When omitted, the server assigns one. |
| `http_client` | `None` | A `urllib3.PoolManager` (or a subclass such as `ProxyManager`) that sends every request. Use it to set your own proxy, TLS, timeout or pool settings. The client does not close it. When omitted, the SDK builds its own. |

Pass `trace_id` to tie the SDK's requests to a trace in your own system, for example one per user
session or workflow run:

```python
import os
from smritea import SmriteaClient

client = SmriteaClient(
    api_key=os.environ["SMRITEA_API_KEY"],
    app_id=os.environ["SMRITEA_APP_ID"],
    trace_id="checkout-flow-7f3a9c",
)
```

---

## Methods

### `add` — Store a memory

```python
from smritea import MemoryScope

result = client.add(
    "User prefers concise replies",
    scope=MemoryScope(
        actor_id="alice",           # explicit actor ID
        actor_type="user",          # "user" | "agent" | "system"
        conversation_id="conv_123", # optional
    ),
    metadata={"source": "chat"},    # optional
)
for memory in result.memories:
    print(memory.id)  # mem_...
```

| Parameter | Default | Description |
|---|---|---|
| `content` | required | Memory text |
| `scope` | `None` | `MemoryScope` object carrying the actor and conversation context. Its fields are listed in [MemoryScope](#memoryscope). `add()` sends all of them. |
| `metadata` | `None` | Arbitrary string-to-string key-value dict |
| `event_occurred_at` | `None` | ISO-8601 datetime of when the content occurred. Used to resolve relative expressions such as "yesterday". Defaults to now. |
| `relative_standing` | `None` | `RelativeStanding` object with `importance`, `decay_factor` and `decay_function` |

---

### `search` — Semantic search

```python
results = client.search(
    "dietary restrictions",
    scope=MemoryScope(actor_id="alice", actor_type="user"),
    limit=5,
)
for r in results:
    print(r.score, r.content)
```

Results are ordered by relevance (descending). Each result exposes `score` (0.0–1.0) and all `Memory` fields directly.

| Parameter | Default | Description |
|---|---|---|
| `query` | required | Search text |
| `scope` | `None` | `MemoryScope` object. `search()` uses `actor_id`, `actor_type`, `conversation_id` and `participant_ids` as filters; `actor_name` and `source_type` are not sent. See [MemoryScope](#memoryscope). |
| `speaker_actor_id` | `None` | Identifies who is asking. It never filters results; it is used for query augmentation and audit. Max 64 characters. |
| `limit` | app default | Number of results. Must not exceed the server's `top_n_max` (default 20). Omit to use the app's `top_n`. |
| `graph_depth` | `None` | Graph traversal depth override, 1–5 |

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
results = client.search("", scope=MemoryScope(actor_id="alice", actor_type="user"), limit=20)
```

`limit` must not exceed the server's `top_n_max` (default 20); a larger value returns a 400 error. Omit it to use the app's `top_n`.

---

## MemoryScope

`MemoryScope` groups the actor and conversation context passed as `scope=` to `add()` and `search()`.
Every field is optional. Import it with `from smritea import MemoryScope`.

| Field | Type | Description |
|---|---|---|
| `actor_id` | str \| None | Actor identifier, max 64 characters. Set together with `actor_type`. |
| `actor_type` | str \| None | `"user"` \| `"agent"` \| `"system"`. Set together with `actor_id`. |
| `actor_name` | str \| None | Display name, max 255 characters. `add()` sends it; `search()` does not. |
| `conversation_id` | str \| None | Conversation thread, max 64 characters. Takes precedence over `participant_ids` if both are set. |
| `source_type` | str \| None | `"conversation"` \| `"document"` \| `"api"`. `add()` sends it; `search()` does not. Defaults to `"api"` when omitted. |
| `participant_ids` | list[str] \| None | Finds memories from conversations in which **all** listed actors took part. Requires at least 2 IDs, each 1–64 characters. Used by `search()`; `add()` also sends it. |

---

## Error handling

```python
from smritea import (
    MemoryScope,
    SmriteaClient,
    SmriteaUnauthorizedError,
    SmriteaNotFoundError,
    SmriteaTooManyRequestsError,
    SmriteaPaymentRequiredError,
    SmriteaBadRequestError,
    SmriteaError,
)

try:
    results = client.search("preferences", scope=MemoryScope(actor_id="alice", actor_type="user"))
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
code), `retryable` (`True` when the server marked the error safe to retry), the full parsed response
`body`, and `request_id`.

`request_id` is the server's request ID for the failed call, taken from the response body's
`request_id` field or, if that is absent, the `X-Request-ID` response header. It is `None` when the
server sent neither. Include it when you contact support about a failed request:

```python
from smritea import SmriteaError

try:
    client.get("mem_abc123")
except SmriteaError as e:
    print(e.request_id)
```

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
