# SDK Cross-Language Reference

Naming conventions across all five SDK implementations.

---

## Table 1: Class Names

| Concept              | Python          | TypeScript            | Go              | Java             | C#               |
|----------------------|-----------------|-----------------------|-----------------|------------------|------------------|
| Client               | `SmriteaClient` | `SmriteaClient`       | `SmriteaClient` | `SmriteaClient`  | `SmriteaClient`  |
| Add options          | `AddOptions`    | `AddOptions`          | `AddOptions`    | `AddOptions`     | `AddOptions`     |
| Search options       | `SearchOptions` | `SearchOptions`       | `SearchOptions` | `SearchOptions`  | `SearchOptions`  |
| Memory return type   | `Memory`        | `Memory`              | `Memory`        | `Memory`         | `Memory`         |
| SearchResult type    | `SearchResult`  | `SearchResult`        | `SearchResult`  | `SearchResult`   | `SearchResult`   |
| Config / constructor | keyword args    | `SmriteaClientConfig` | `ClientConfig`  | constructor args | constructor args |

> **Go note**: `Memory` and `SearchResult` are type aliases over autogen structs. Field names are
> controlled by the autogen package (e.g. `ActorId`, not `ActorID`) — this is expected and cannot be
> changed without modifying the generator output.

---

## Table 2: Public Method Names

| Operation  | Python      | TypeScript | Go         | Java       | C#              |
|------------|-------------|------------|------------|------------|-----------------|
| Add memory | `add()`     | `add()`    | `Add()`    | `add()`    | `AddAsync()`    |
| Search     | `search()`  | `search()` | `Search()` | `search()` | `SearchAsync()` |
| Get by ID  | `get()`     | `get()`    | `Get()`    | `get()`    | `GetAsync()`    |
| Delete     | `delete()`  | `delete()` | `Delete()` | `delete()` | `DeleteAsync()` |
| List all   | `get_all()` | `getAll()` | `GetAll()` | `getAll()` | `GetAllAsync()` |

> **C# note**: all methods are `async Task<T>` — the `Async` suffix is mandatory per C# naming
> conventions. All other languages expose synchronous methods (Go uses `context.Context` but is still
> synchronous from the caller's perspective with blocking I/O).
>
> **Go note**: method names are `PascalCase` (exported). Python uses `snake_case`. TypeScript and
> Java use `camelCase`. These are language-idiomatic and intentional.
>
> **`get_all()` / `GetAll()` / `GetAllAsync()`**: all implementations raise
> `NotImplementedError` / `UnsupportedOperationException` / `NotImplementedException` — the
> list-memories endpoint is pending server-side implementation.

---

## Table 3: Exception / Error Class Names

| HTTP status     | Python                        | TypeScript                    | Go                            | Java                          | C#                                |
|-----------------|-------------------------------|-------------------------------|-------------------------------|-------------------------------|-----------------------------------|
| base            | `SmriteaError`                | `SmriteaError`                | `SmriteaError`                | `SmriteaError`                | `SmriteaException`                |
| 400             | `SmriteaValidationError`      | `SmriteaValidationError`      | `SmriteaValidationError`      | `SmriteaValidationError`      | `SmriteaValidationException`      |
| 401             | `SmriteaAuthError`            | `SmriteaAuthError`            | `SmriteaAuthError`            | `SmriteaAuthError`            | `SmriteaAuthException`            |
| 402             | `SmriteaQuotaError`           | `SmriteaQuotaError`           | `SmriteaQuotaError`           | `SmriteaQuotaError`           | `SmriteaQuotaException`           |
| 404             | `SmriteaNotFoundError`        | `SmriteaNotFoundError`        | `SmriteaNotFoundError`        | `SmriteaNotFoundError`        | `SmriteaNotFoundException`        |
| 429             | `SmriteaRateLimitError`       | `SmriteaRateLimitError`       | `SmriteaRateLimitError`       | `SmriteaRateLimitError`       | `SmriteaRateLimitException`       |
| deserialization | `SmriteaDeserializationError` | `SmriteaDeserializationError` | `SmriteaDeserializationError` | `SmriteaDeserializationError` | `SmriteaDeserializationException` |

> **C# naming convention**: C# uses `*Exception` suffix (e.g. `SmriteaException`) instead of
> `*Error` — this is intentional to follow C# idioms (`ArgumentException`, `HttpRequestException`).
> All other languages use `*Error`.
