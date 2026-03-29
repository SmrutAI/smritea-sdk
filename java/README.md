# smritea SDK — Java

Java SDK for the [smritea](https://smritea.ai) AI memory system.

**[Get a free API key →](https://smritea.ai)**

---

## Installation

**Maven**

```xml
<dependency>
    <groupId>ai.smritea</groupId>
    <artifactId>smritea-sdk</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle**

```groovy
implementation 'ai.smritea:smritea-sdk:0.1.0'
```

Requires Java 11+.

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

```java
import ai.smritea.sdk.SmriteaClient;
import ai.smritea.sdk.model.AddOptions;
import ai.smritea.sdk.model.Memory;
import ai.smritea.sdk.model.SearchOptions;
import ai.smritea.sdk.model.SearchResult;
import java.util.List;

SmriteaClient client = new SmriteaClient(
    System.getenv("SMRITEA_API_KEY"),
    System.getenv("SMRITEA_APP_ID")
);

// Store something about a user
Memory mem = client.add("Alice is a vegetarian and loves hiking",
    new AddOptions().withActorId("alice").withActorType("user"));

// Retrieve it later
List<SearchResult> results = client.search("What are Alice's food preferences?",
    new SearchOptions().withActorId("alice").withActorType("user"));
for (SearchResult r : results) {
    System.out.printf("%.2f  %s%n", r.getScore(), r.getContent());
}
```

---

## Constructor

```java
import ai.smritea.sdk.SmriteaClient;

// Minimal (uses default base URL and 2 retries)
SmriteaClient client = new SmriteaClient("sk-...", "app_...");

// Full
SmriteaClient client = new SmriteaClient(
    "sk-...",                           // apiKey — required
    "app_...",                          // appId — required
    "https://api.smritea.ai",           // baseUrl — optional, default shown
    2                                   // maxRetries — optional, default 2; 0 disables retry
);
```

All methods are **synchronous** and return values directly. For async use, wrap calls in a
`CompletableFuture` or virtual thread (`Thread.ofVirtual()`).

---

## Methods

### `add` — Store a memory

```java
Memory memory = client.add("User prefers concise replies",
    new AddOptions()
        .withActorId("alice")                                   // explicit actor ID
        .withActorType("user")                                  // "user" | "agent" | "system"
        .withMetadata(Map.of("source", "chat"))                 // optional
        .withConversationId("conv_123"));                       // optional
System.out.println(memory.getId()); // mem_...
```

| Parameter | Type | Default | Description |
|---|---|---|---|
| `content` | `String` | required | Memory text |
| `withActorId(v)` | `String` | `null` | Actor ID |
| `withActorType(v)` | `String` | `null` | `"user"` \| `"agent"` \| `"system"` |
| `withActorName(v)` | `String` | `null` | Display name |
| `withMetadata(v)` | `Map<String, Object>` | `null` | Arbitrary key-value map |
| `withConversationId(v)` | `String` | `null` | Conversation context |

---

### `search` — Semantic search

```java
List<SearchResult> results = client.search("dietary restrictions",
    new SearchOptions()
        .withActorId("alice")
        .withActorType("user")
        .withLimit(5)
        .withThreshold(0.7f));           // min relevance score 0.0–1.0
for (SearchResult r : results) {
    System.out.println(r.getScore() + "  " + r.getContent());
}
```

Results are ordered by relevance (descending). Each result exposes `getScore()` (0.0–1.0) and all `Memory` fields via getter methods.

| Parameter | Type | Default | Description |
|---|---|---|---|
| `query` | `String` | required | Search text |
| `withActorId(v)` | `String` | `null` | Filter by actor ID |
| `withActorType(v)` | `String` | `null` | Filter by actor type |
| `withLimit(v)` | `Integer` | app default | Max results to return |
| `withThreshold(v)` | `Float` | `null` | Min relevance score 0.0–1.0 |
| `withGraphDepth(v)` | `Integer` | `null` | Graph traversal depth override |
| `withConversationId(v)` | `String` | `null` | Conversation context |

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
    .withActorId("alice")
    .withActorType("user")
    .withLimit(100));
```

---

## Error handling

```java
import ai.smritea.sdk.errors.SmriteaAuthError;
import ai.smritea.sdk.errors.SmriteaRateLimitError;
import ai.smritea.sdk.errors.SmriteaQuotaError;
import ai.smritea.sdk.errors.SmriteaError;

try {
    List<SearchResult> results = client.search("preferences",
        new SearchOptions().withActorId("alice").withActorType("user"));
} catch (SmriteaAuthError e) {
    System.out.println("Check your API key");
} catch (SmriteaRateLimitError e) {
    System.out.printf("Rate limited — retry after %ds%n", e.getRetryAfter());
} catch (SmriteaQuotaError e) {
    System.out.println("Plan quota exceeded");
} catch (SmriteaError e) {
    System.out.printf("Unexpected error: %s%n", e.getMessage());
}
```

| Exception | HTTP | When |
|---|---|---|
| `SmriteaAuthError` | 401 | Invalid or missing API key |
| `SmriteaValidationError` | 400 | Invalid request parameters |
| `SmriteaNotFoundError` | 404 | Memory ID does not exist |
| `SmriteaQuotaError` | 402 | Organisation quota exceeded |
| `SmriteaRateLimitError` | 429 | Rate limit hit — check `.getRetryAfter()` |
| `SmriteaDeserializationError` | — | Server returned an unexpected response body |
| `SmriteaError` | other | Unexpected server error |

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
| `getConversationMessageId()` | String | Message within the conversation (nullable) |
| `getActiveFrom()` | String | ISO 8601 — when memory becomes valid |
| `getActiveTo()` | String | ISO 8601 — when memory expires (nullable) |
| `getCreatedAt()` | String | ISO 8601 creation timestamp |
| `getUpdatedAt()` | String | ISO 8601 last update timestamp |
