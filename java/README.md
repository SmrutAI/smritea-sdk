---
type: Overview
title: smritea SDK — Java
status: stable
tags:
- readme
stale_after: 2026-12-31
generated:
  by: Tushar Dwivedi
  at: 2026-08-25T00:00:00Z
---

# smritea SDK — Java


| Section | What it covers |
|---------|----------------|
| [Installation](#installation) | Maven |
| [Get your API key](#get-your-api-key) | Sign up at smritea.ai — free account, no credit card required |
| [Quickstart](#quickstart) | Quickstart |
| [Constructor](#constructor) | All methods are synchronous and return values directly. For async use, wrap calls in a |
| [Methods](#methods) | Methods |
| [Error handling](#error-handling) | Error handling |
| [`Memory` type reference](#memory-type-reference) | Memory type reference |

Java SDK for the [smritea](https://smritea.ai) AI memory system.

[![Maven Central](https://img.shields.io/maven-central/v/ai.smritea/smritea-sdk)](https://central.sonatype.com/artifact/ai.smritea/smritea-sdk)

**[Get a free API key →](https://smritea.ai)**

---

## Installation

**Maven**

```xml
<dependency>
    <groupId>ai.smritea</groupId>
    <artifactId>smritea-sdk</artifactId>
    <version>LATEST</version> <!-- latest release: see the Maven Central badge above -->
</dependency>
```

**Gradle**

```groovy
implementation 'ai.smritea:smritea-sdk:LATEST' // latest release: see the Maven Central badge above
```

Requires Java 11+.

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

```java
import ai.smritea.sdk.SmriteaClient;
import ai.smritea.sdk.model.AddOptions;
import ai.smritea.sdk.model.MemoryCreationResult;
import ai.smritea.sdk.model.MemoryScope;
import ai.smritea.sdk.model.SearchOptions;
import ai.smritea.sdk.model.SearchResult;
import java.util.List;

SmriteaClient client = new SmriteaClient(
    System.getenv("SMRITEA_API_KEY"),
    System.getenv("SMRITEA_APP_ID")
);

// Store something about a user
MemoryCreationResult result = client.add("Alice is a vegetarian and loves hiking",
    new AddOptions().withScope(
        MemoryScope.builder().actorId("alice").actorType("user").build()));

// Retrieve it later
List<SearchResult> results = client.search("What are Alice's food preferences?",
    new SearchOptions().withScope(
        MemoryScope.builder().actorId("alice").actorType("user").build()));
for (SearchResult r : results) {
    System.out.printf("%.2f  %s%n", r.getScore(), r.getContent());
}
```

---

## Constructor

```java
import ai.smritea.sdk.SmriteaClient;

// Minimal (uses default base URL and 2 retries)
SmriteaClient client = new SmriteaClient("smrt_<prefix>_<secret>", "app_...");

// Full
SmriteaClient client = new SmriteaClient(
    "smrt_<prefix>_<secret>",           // apiKey — required
    "app_...",                          // appId — required
    "https://api-us.smritea.ai",           // baseUrl — optional, default shown
    2                                   // maxRetries — optional, default 2; 0 disables retry
);

// With a Trace ID — the 5th argument
SmriteaClient client = new SmriteaClient(
    "smrt_<prefix>_<secret>",
    "app_...",
    "https://api-us.smritea.ai",
    2,
    "checkout-req-7f3a9c"               // traceId — optional; pass null to send no header
);
```

`traceId` is sent as the `X-Trace-ID` header on every request the client makes. It must be 8–100
characters long and use only letters A–Z, a–z, digits 0–9, dot (`.`), underscore (`_`), colon (`:`)
and hyphen (`-`). An invalid value throws `IllegalArgumentException` from the constructor. The
four-argument constructors send no trace header.

The sixth argument `httpClient` (a `java.net.http.HttpClient`, optional) sends every request: configure
your own proxy, TLS, timeouts or executor on it before you pass it. `null` builds a default client. The
`SmriteaClient` does not close it.

All methods are **synchronous** and return values directly. For async use, wrap calls in a
`CompletableFuture` or virtual thread (`Thread.ofVirtual()`).

---

## Methods

### `add` — Store a memory

```java
import ai.smritea.sdk.model.AddOptions;
import ai.smritea.sdk.model.MemoryCreationResult;
import ai.smritea.sdk.model.MemoryScope;
import java.util.Map;

MemoryCreationResult result = client.add("User prefers concise replies",
    new AddOptions()
        .withScope(MemoryScope.builder()
            .actorId("alice")                                   // explicit actor ID
            .actorType("user")                                  // "user" | "agent" | "system"
            .conversationId("conv_123")                         // optional
            .build())
        .withMetadata(Map.of("source", "chat")));               // optional
System.out.println(result.getMemories().get(0).getId()); // mem_...
```

`add` returns a `MemoryCreationResult`. One call can store several facts, so read each one from `getMemories()`.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `content` | `String` | required | Memory text |
| `withScope(v)` | `MemoryScope` | `null` | Actor and conversation context (see the scope builder below) |
| `withMetadata(v)` | `Map<String, Object>` | `null` | Key-value map; values must be string, integer or float |
| `withEventOccurredAt(v)` | `String` | `null` | RFC 3339 time the event in the text happened |
| `withRelativeStanding(v)` | `RelativeStanding` | `null` | Importance and decay configuration |

`MemoryScope.builder()` sets the scope fields. Every field is optional.

| Builder method | Type | Description |
|---|---|---|
| `actorId(v)` | `String` | Actor ID, at most 64 characters. Pair it with `actorType`. |
| `actorType(v)` | `String` | `"user"` \| `"agent"` \| `"system"` |
| `actorName(v)` | `String` | Display name, at most 255 characters |
| `conversationId(v)` | `String` | Conversation ID. Cannot be combined with `participantIds`. |
| `sourceType(v)` | `String` | `"conversation"` \| `"document"` \| `"api"` |
| `participantIds(v)` | `List<String>` | Search only: every listed actor took part. Cannot be combined with `conversationId`. |

---

### `search` — Semantic search

```java
import ai.smritea.sdk.model.MemoryScope;
import ai.smritea.sdk.model.SearchOptions;
import ai.smritea.sdk.model.SearchResult;
import java.util.List;

List<SearchResult> results = client.search("dietary restrictions",
    new SearchOptions()
        .withScope(MemoryScope.builder().actorId("alice").actorType("user").build())
        .withLimit(5));
for (SearchResult r : results) {
    System.out.println(r.getScore() + "  " + r.getContent());
}
```

Results are ordered by relevance (descending). Each result exposes `getScore()` (0.0–1.0) and all `Memory` fields via getter methods.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `String` | required | Search text |
| `withScope(v)` | `MemoryScope` | `null` | Filter by actor, conversation, source type or participants |
| `withLimit(v)` | `Integer` | app default | Max results to return; at most the server's `top_n_max` |
| `withGraphDepth(v)` | `Integer` | `null` | Graph traversal depth, 1 to 5 |
| `withFromTime(v)` | `String` | `null` | RFC 3339 start of a time range. Cannot be combined with `withValidAt`. |
| `withToTime(v)` | `String` | `null` | RFC 3339 end of a time range. Cannot be combined with `withValidAt`. |
| `withValidAt(v)` | `String` | `null` | RFC 3339 moment; returns memories valid at that moment |
| `withMethod(v)` | `String` | app default | `"quick_search"` \| `"deep_search"` \| `"context_aware_search"` |
| `withRerankerType(v)` | `String` | app default | Applies to `deep_search` only |
| `withMetadataFilter(v)` | `Map<String, Object>` | `null` | Operator filter over memory metadata |
| `withSpeakerActorId(v)` | `String` | `null` | Who is asking. Never filters results. |

---

### `get` — Retrieve a memory by ID

```java
Memory memory = client.get("mem_abc123");
System.out.println(memory.getContent() + " " + memory.getCreatedAt());
// Throws SmriteaNotFoundError if the ID does not exist
```

---

### `delete` — Delete a memory by ID

```java
client.delete("mem_abc123");
// Throws SmriteaNotFoundError if the ID does not exist
```

---

### `getAll` — List all memories

> **Not yet implemented.** Throws `UnsupportedOperationException`.
> Use `search()` with a broad query as a workaround:

```java
List<SearchResult> results = client.search("", new SearchOptions()
    .withScope(MemoryScope.builder().actorId("alice").actorType("user").build())
    .withLimit(20));
```

`limit` must not exceed the server's `top_n_max` (default 20); a larger value returns a 400 error. Omit it to use the app's `top_n`.

---

## Error handling

```java
import ai.smritea.sdk.errors.SmriteaUnauthorizedError;
import ai.smritea.sdk.errors.SmriteaTooManyRequestsError;
import ai.smritea.sdk.errors.SmriteaPaymentRequiredError;
import ai.smritea.sdk.errors.SmriteaError;

try {
    List<SearchResult> results = client.search("preferences",
        new SearchOptions().withScope(
            MemoryScope.builder().actorId("alice").actorType("user").build()));
} catch (SmriteaUnauthorizedError e) {
    System.out.println("Check your API key");
} catch (SmriteaTooManyRequestsError e) {
    System.out.printf("Rate limited — retry after %ds%n", e.getRetryAfter());
} catch (SmriteaPaymentRequiredError e) {
    System.out.println("Plan quota exceeded");
} catch (SmriteaError e) {
    System.out.printf("Unexpected error: %s (code=%s, retryable=%b)%n",
        e.getMessage(), e.getCode(), e.isRetryable());
    System.out.println("Request ID: " + e.getRequestId());
}
```

| Exception | HTTP | When |
|---|---|---|
| `SmriteaBadRequestError` | 400 | Invalid request parameters |
| `SmriteaUnauthorizedError` | 401 | Invalid or missing API key |
| `SmriteaPaymentRequiredError` | 402 | Organisation quota exceeded |
| `SmriteaForbiddenError` | 403 | Caller is authenticated but not permitted to perform the action |
| `SmriteaNotFoundError` | 404 | Memory ID does not exist |
| `SmriteaConflictError` | 409 | Request conflicts with existing state |
| `SmriteaUnprocessableError` | 422 | Request body is well-formed but semantically invalid |
| `SmriteaTooManyRequestsError` | 429 | Rate limit hit — check `.getRetryAfter()` |
| `SmriteaDeserializationError` | — | Server returned an unexpected response body |
| `SmriteaError` | other | Unexpected server error |

Every exception carries `getCode()` (the server's machine-readable wire code, e.g.
`MEMORY_NOT_FOUND`), `getHttpStatus()`, and `isRetryable()`, in addition to `getBody()` for the
full parsed response body.

`getRequestId()` returns the server's request ID for the failed call. It is taken from the response
body's `request_id` field, or from the `X-Request-ID` response header if the body has none. It
returns `null` when neither is present.

---

## `Memory` type reference

| Getter method | Type | Description |
|---|---|---|
| `getId()` | String | Memory ID (`mem_...`) |
| `getAppId()` | String | App this memory belongs to |
| `getContent()` | String | Memory text |
| `getActorId()` | String | Actor who owns this memory |
| `getActorType()` | String | `"user"` \| `"agent"` \| `"system"` |
| `getActorName()` | String | Display name (nullable) |
| `getMetadata()` | Map<String, Object> | Arbitrary key-value pairs (nullable) |
| `getConversationId()` | String | Conversation context (nullable) |
| `getActiveFrom()` | String | ISO 8601 — when memory becomes valid |
| `getActiveTo()` | String | ISO 8601 — when memory expires (nullable) |
| `getCreatedAt()` | String | ISO 8601 creation timestamp |
| `getUpdatedAt()` | String | ISO 8601 last update timestamp |
