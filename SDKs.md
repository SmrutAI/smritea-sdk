---
type: Overview
title: SDK Cross-Language Reference
status: draft
tags: []
---

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

> **Status (errkit alignment, docs/plans/175-errkit-unified-error-handling.md in smritea-cloud)**: C#
> has been converted to the full 9-category errkit hierarchy (`Code`/`HTTPStatus`/`Retryable` base
> fields, category-exact subclass names). Python, TypeScript, Go, and Java still use the pre-errkit
> hierarchy shown below and are converted in separate tasks of the same wave.

| HTTP status     | Python                        | TypeScript                    | Go                            | Java                          | C#                                |
|-----------------|-------------------------------|-------------------------------|-------------------------------|-------------------------------|-----------------------------------|
| base (5xx/unknown) | `SmriteaError`             | `SmriteaError`                | `SmriteaError`                | `SmriteaError`                | `SmriteaException`                |
| 400 Bad Request | `SmriteaBadRequestError`      | `SmriteaBadRequestError`      | `SmriteaBadRequestError`      | `SmriteaBadRequestError`      | `SmriteaBadRequestException`      |
| 401 Unauthorized | `SmriteaUnauthorizedError`   | `SmriteaUnauthorizedError`    | `SmriteaUnauthorizedError`    | `SmriteaUnauthorizedError`    | `SmriteaUnauthorizedException`    |
| 402 Payment Required | `SmriteaPaymentRequiredError` | `SmriteaPaymentRequiredError` | `SmriteaPaymentRequiredError` | `SmriteaPaymentRequiredError` | `SmriteaPaymentRequiredException` |
| 403 Forbidden   | `SmriteaForbiddenError`       | `SmriteaForbiddenError`       | `SmriteaForbiddenError`       | `SmriteaForbiddenError`       | `SmriteaForbiddenException`       |
| 404 Not Found   | `SmriteaNotFoundError`        | `SmriteaNotFoundError`        | `SmriteaNotFoundError`        | `SmriteaNotFoundError`        | `SmriteaNotFoundException`        |
| 409 Conflict    | `SmriteaConflictError`        | `SmriteaConflictError`        | `SmriteaConflictError`        | `SmriteaConflictError`        | `SmriteaConflictException`        |
| 422 Unprocessable | `SmriteaUnprocessableError` | `SmriteaUnprocessableError`   | `SmriteaUnprocessableError`   | `SmriteaUnprocessableError`   | `SmriteaUnprocessableException`   |
| 429 Too Many Requests | `SmriteaTooManyRequestsError` | `SmriteaTooManyRequestsError` | `SmriteaTooManyRequestsError` | `SmriteaTooManyRequestsError` | `SmriteaTooManyRequestsException` |
| deserialization | `SmriteaDeserializationError` | `SmriteaDeserializationError` | `SmriteaDeserializationError` | `SmriteaDeserializationError` | `SmriteaDeserializationException` |

> **C# naming convention**: C# uses `*Exception` suffix (e.g. `SmriteaException`) instead of
> `*Error` — this is intentional to follow C# idioms (`ArgumentException`, `HttpRequestException`).
> All other languages use `*Error`.
>
> **C# base fields (errkit alignment)**: every C# exception carries `Code` (server wire code),
> `HTTPStatus`, `Retryable`, and `Body` (full parsed response body). `SmriteaTooManyRequestsException`
> additionally carries `RetryAfter`.
