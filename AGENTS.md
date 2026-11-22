# AGENTS.md — smritea SDK

Rules for AI agents and contributors working on any language implementation of the smritea SDK.
This file applies to every current and future SDK under this repository (`python/`, `typescript/`, etc.).

---

## Architecture

The SDK has these layers:

```
src/
  client.*        # SmriteaClient — public API, all business logic lives here
  types.*         # Public-facing types (Memory, SearchResult, config interfaces)
  errors.*        # Typed exception hierarchy
  _internal/      # Auto-generated API client — DO NOT edit manually
```

**Rule**: `_internal/autogen` is a derived artifact synced from the server's OpenAPI spec.
Never write or edit files inside `_internal/autogen` by hand. Run `make sync-autogen` to regenerate it.

---

## Language-Specific Directories

Each language lives in its own subdirectory and is an independently publishable package:

- `python/` — PyPI package `smritea-sdk`, import as `from smritea import SmriteaClient`
- `typescript/` — npm package `smritea-sdk`, import as `import { SmriteaClient } from 'smritea-sdk'`
- `go/` — Go module `github.com/SmrutAI/smritea-sdk/go`, import as `import smritea "github.com/SmrutAI/smritea-sdk/go"`
  **Note**: Go uses `internal/autogen` (compiler-enforced `internal/` package, no leading underscore),
  unlike Python/TypeScript which use `_internal/autogen` (convention-based).
- `java/` — Maven package `ai.smritea:smritea-sdk`, import as `import ai.smritea.sdk.SmriteaClient`
  **Note**: Java uses `_internal/autogen` (naming convention only). There is no `module-info.java`:
  the generated classes are `public` and can be imported by any code on the classpath.
- `csharp/` — NuGet package `Smritea.Sdk`, import as `using Smritea.Sdk`
  **Note**: C# uses `_internal/autogen` (naming convention only). The generator emits `public` classes
  for the models and the client, which the public result types alias.
  `<InternalsVisibleTo Include="Smritea.Sdk.Tests" />` in the `.csproj` exposes internal members to tests.

When adding a new language, follow the same structure: one `client` file, one `types` file, one `errors`
file, with `_internal/autogen` (or `internal/autogen` for Go) excluded from lint and type-checking.

---

## Public API Contract

Every SDK implementation MUST expose exactly this surface:

| Method   | Signature                                                                                                                                                |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `add`    | `(content, *, scope?, metadata?, event_occurred_at?, relative_standing?) → MemoryCreationResult`                                                         |
| `search` | `(query, *, speaker_actor_id?, scope?, limit?, graph_depth?, from_time?, to_time?, valid_at?, method?, reranker_type?, metadata_filter?) → list[SearchResult]` |
| `get`    | `(memory_id) → Memory`                                                                                                                                   |
| `delete` | `(memory_id) → void`                                                                                                                                     |
| `get_all` | `() → list[Memory]` — a stub that raises "not yet available" in every language                                                                          |

**`app_id` on constructor**: Set once, then sent as the `X-App-ID` request header on every call —
`add`, `search`, `get`, and `delete`. It is never placed in the request body and is never accepted as
a per-call argument. Callers must never pass it per-call.

**`scope` object**: Groups actor and conversation context fields (`actor_id`, `actor_type`,
`actor_name`, `conversation_id`, `source_type`, `participant_ids`). All fields are
optional. In `add()`, all 6 scope fields are mapped. In `search()`, `actor_id`,
`actor_type`, `conversation_id`, `source_type`, and `participant_ids` are sent as filters.

**Trace ID on constructor**: Optional. When set, it is sent as the `X-Trace-ID` request header on every
call — `add`, `search`, `get`, and `delete`. It is validated in the constructor, before any network call,
against: 8-100 characters of `A-Z`, `a-z`, `0-9`, `.`, `_`, `:`, `-`. Each language uses the name and
failure behaviour below (verified against the source; `None`/`undefined`/`null`/`nil` means unset):

| Language   | Option name                                                                 | Invalid value                                                                                                                                  | Source                                                                                                 |
|------------|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| Python     | `trace_id` keyword on `SmriteaClient(...)`                                  | `ValueError` raised from the constructor                                                                                                       | `python/src/smritea/client.py#SmriteaClient.__init__`                                                  |
| TypeScript | `traceId` field on `SmriteaClientConfig`                                    | `TypeError` thrown from the constructor                                                                                                        | `typescript/src/client.ts#SmriteaClient`; `typescript/src/types.ts#SmriteaClientConfig`                |
| Go         | `ClientConfig.TraceID`                                                      | `NewClient` still returns a client; `Add`, `Search`, `Get`, `Delete` and `GetAll` each return a `*SmriteaBadRequestError` (HTTP 400, `Code` `INVALID_TRACE_ID`) and send no request | `go/client.go#NewClient`; `go/errors.go#ErrCodeInvalidTraceID`; `go/types.go#ClientConfig` |
| Java       | `traceId`, 5th argument of the 5-argument constructor `(apiKey, appId, baseUrl, maxRetries, traceId)` | `IllegalArgumentException` thrown from the constructor                                                                    | `java/src/main/java/ai/smritea/sdk/SmriteaClient.java#SmriteaClient`                                    |
| C#         | `traceId`, last parameter of the constructor                                | `ArgumentException` thrown from the constructor (parameter name `traceId`)                                                                     | `csharp/SmriteaClient.cs#SmriteaClient`                                                                 |

Language notes: Go treats an empty `TraceID` as unset (no error); every other language rejects `""` because
it fails the pattern. Go's validation counts bytes, not characters. Java's 4-argument constructors pass
`null` for the trace ID, which sends no header.

**HTTP client on constructor**: Optional. The given client sends every request exactly as given: the SDK
adds no timeout to it and never closes it, so the caller's own timeouts, proxy and TLS settings take
effect. When none is given, the SDK builds its own client with the default request timeout (see
"Request Timeout").

| Language   | Option name                                         | Null or absent                                                              | Source                                                                                                              |
|------------|-----------------------------------------------------|-----------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------|
| Python     | `http_client` keyword (`urllib3.PoolManager`)       | the generated client's PoolManager, wrapped so each request has a 10 s timeout | `python/src/smritea/client.py#SmriteaClient.__init__`, `python/src/smritea/_http_client.py#_default_pool_manager`  |
| TypeScript | `fetch` field (`typeof fetch`)                      | the global `fetch`, wrapped so each request has a 10 s timeout              | `typescript/src/types.ts#SmriteaClientConfig`, `typescript/src/default-fetch.ts#createDefaultFetch`                 |
| Go         | `ClientConfig.HTTPClient` (`*http.Client`)          | a new `http.Client` with `Timeout` 10 s (never `http.DefaultClient`)        | `go/types.go#ClientConfig`, `go/client.go#newDefaultHTTPClient`                                                     |
| Java       | `httpClient`, 6th constructor argument (`HttpClient`) | a new client wrapped in `TimeoutHttpClient`, 10 s per exchange            | `java/src/main/java/ai/smritea/sdk/SmriteaClient.java#SmriteaClient`, `java/src/main/java/ai/smritea/sdk/TimeoutHttpClient.java#TimeoutHttpClient` |
| C#         | `httpClient`, 6th constructor parameter (`HttpClient?`) | the SDK creates its own with a 10 s request timeout; `Dispose` never disposes a given one | `csharp/SmriteaClient.cs#SmriteaClient`, `csharp/SmriteaClient.cs#ResolveTimeout`           |

---

## Error Hierarchy

Every implementation must define this exact hierarchy, mapping HTTP status codes 1:1 onto the
server's nine HTTP error categories:

| HTTP        | Exception class            |
|-------------|-----------------------------|
| 400         | `SmriteaBadRequestError`    |
| 401         | `SmriteaUnauthorizedError`  |
| 402         | `SmriteaPaymentRequiredError` |
| 403         | `SmriteaForbiddenError`     |
| 404         | `SmriteaNotFoundError`      |
| 409         | `SmriteaConflictError`      |
| 422         | `SmriteaUnprocessableError` |
| 429         | `SmriteaTooManyRequestsError` |
| 5xx / other | `SmriteaError` (base)       |

Coverage is complete for all 9 errkit HTTP categories: every status class above plus the `SmriteaError`
base for 5xx/unknown. A separate deserialization-error class (not tied to any HTTP status) is raised
when a response body cannot be parsed as the expected error envelope at all.

All exception classes inherit from `SmriteaError` and carry at minimum:

- `message` — human-readable error message
- `Code` — the server's stable machine-readable wire code (e.g. `MEMORY_NOT_FOUND`); field name uses
  language-idiomatic casing per SDK (e.g. `code` in Python, `Code` in Go/C#)
- `HTTPStatus` — the HTTP status code the error was raised for; same per-language casing rule as `Code`
- `Retryable` — boolean, `true` when the server marked the error retryable; same per-language casing rule
- `request_id` — the server request ID of the failed call; same per-language casing rule as `Code`, but
  the names are fixed per language: Python `request_id`, TypeScript `requestId`, Go `RequestID`, Java
  `getRequestId()`, C# `RequestId`. Source: the body's `request_id` field when it is a non-empty string;
  otherwise the `X-Request-ID` response header (name matched case-insensitively). When neither source has a
  non-empty value it is absent: `None` in Python, `undefined` in TypeScript, the empty string in Go, and
  `null` in Java and C#. Verified at
  `python/src/smritea/exceptions.py:9-23,32-34,50` (`extract_request_id`),
  `typescript/src/client.ts:204-212`, `typescript/src/errors.ts:11-12,21`,
  `go/errors.go:16-27,104,163-171`,
  `java/.../errors/SmriteaError.java:15,137` and `java/.../SmriteaClient.java:432-437,560-569`,
  `csharp/SmriteaException.cs:89-94` and `csharp/SmriteaClient.cs:486-563`.
- the full parsed response body, for callers that need fields beyond the five above

`SmriteaTooManyRequestsError` MUST also carry a `retry_after` field (nullable/optional integer, seconds).
This is populated from the server's `Retry-After` response header when present.

**Each language SDK stays standalone**: this hierarchy is hand-written and hand-mapped independently
in every language, per the SDK contract above — the polyglot SDK does not depend on or consume
`errkit-ts` (or any other errkit language binding) at runtime. Only the category *shape* and status-code
mapping are kept in lockstep with the server; the implementation in each language is self-contained.

---

## Retry Behaviour (MANDATORY for all languages)

Every SDK implementation MUST implement automatic retry on HTTP 429 with the following behaviour:

1. **Default retries**: 2 (configurable via `max_retries` on the client constructor, default 2,
   minimum 0 to disable). A negative value is treated as 0. Go uses `*int`: nil means the default 2; use `smritea.Int(n)` to set a value.
2. **Sleep strategy** (in priority order):
    - If the server returns a `Retry-After` header: sleep for 90 % of that many seconds.
    - Otherwise: exponential backoff — `1s × 2^attempt` with ±25 % random jitter.
3. **Hard cap**: sleep duration must never exceed **30 seconds**, regardless of what the server says.
4. **Jitter**: always apply jitter to exponential backoff to avoid thundering herd from concurrent clients.
5. **Final raise**: after all retries are exhausted, raise `SmriteaTooManyRequestsError` with `retry_after`
   populated from the last response header (so callers can inspect it if they implement their own logic).

Reference implementation in Python (`client.py`):

```
_retry_delay(attempt, retry_after):
    if retry_after > 0: return min(retry_after * 0.9, 30)
    base = min(1.0 * 2^attempt, 30)
    jitter = base * (0.75 + 0.5 * random())
    return min(jitter, 30)
```

---

## Request Timeout (MANDATORY for all languages)

Every SDK implementation MUST bound every HTTP exchange it sends through its own HTTP client:

1. **Default**: when the caller passes no HTTP client, the SDK builds its own client with a timeout of
   **10 seconds**. The number lives in one named constant per language (table below); no other file repeats it.
2. **Budget**: the 10 seconds are the total time of ONE HTTP exchange, from sending the request until the whole
   response body has been read. A server that never answers, or that sends the headers and then stalls the
   body, must make the call fail within 10 seconds. Python is the one exception (see "Python" below).
3. **Retries**: a 429 retry starts a new exchange with a new budget, and the retry sleep is not counted. A
   timeout is never retried.
4. **Supplied client**: a client the caller passes is used exactly as given. The SDK adds no timeout to it and
   never closes it, so the caller's own timeouts take effect.
5. **No timeout option**: the constructors have no timeout parameter. A caller who wants another value passes
   an HTTP client configured with it.

| Language   | Constant                                           | Mechanism of the default client                                                       |
|------------|----------------------------------------------------|---------------------------------------------------------------------------------------|
| Python     | `python/src/smritea/client.py#DEFAULT_TIMEOUT_SECONDS` (`10.0`) | `_PoolManagerAdapter` puts `urllib3.Timeout(total=10)` on every request and `_NoTimeoutRetry` stops urllib3 from retrying a timeout (`python/src/smritea/_http_client.py`) |
| TypeScript | `typescript/src/default-fetch.ts#DEFAULT_TIMEOUT_MS` (`10_000`) | `createDefaultFetch` adds `AbortSignal.timeout` to every `fetch` call; `client.ts` uses it when `config.fetch` is absent |
| Go         | `go/client.go#DefaultTimeout` (`10 * time.Second`) | `newDefaultHTTPClient` returns `&http.Client{Timeout: DefaultTimeout}`                |
| Java       | `java/src/main/java/ai/smritea/sdk/SmriteaClient.java#DEFAULT_TIMEOUT` (`Duration.ofSeconds(10)`) | `TimeoutHttpClient` wraps a JDK client: request timeout for the header wait, plus a body stream that a timer closes at the deadline |
| C#         | `csharp/SmriteaClient.cs#DefaultTimeout` (`TimeSpan.FromSeconds(10)`) | the generated `Configuration.Timeout` cancels `SendAsync`; `ResolveTimeout` sets it to `Timeout.InfiniteTimeSpan` for a supplied client |

**Why the mechanism differs per language** (each point was reproduced with a local server that stalls, not
assumed):

- **Python**: the generated REST layer passes `timeout=None` on every call, and an explicit `None` overrides a
  timeout set on the `PoolManager` itself. A `PoolManager(timeout=...)` passed as `http_client` was therefore
  ignored. `_PoolManagerAdapter` replaces the `None` with the SDK default, or drops it for a supplied manager so
  that the manager's own timeout applies. urllib3 limits the connect, the wait for headers and each read, not
  the whole body: a body that stalls fails within 10 seconds, but a body that keeps sending data is not cut off.
- **Java**: `HttpRequest.Builder.timeout` covers only the wait for the response headers. A server that sends
  the headers and stalls the body blocks a reader of the body stream forever (reproduced on JDK 21).
  `TimeoutHttpClient` closes the body stream at the deadline. This is verified on JDK 21 only, although the
  module targets release 11.
- **TypeScript**: `AbortSignal.timeout` stays attached to the response body, so a stalled body is covered. The
  generated `deleteMemory` never reads its response body, so `delete()` in `typescript/src/client.ts` reads it
  explicitly; without that a stalled body made a delete succeed.
- **Go**: `http.Client.Timeout` includes reading the response body.
- **C#**: `SendAsync` buffers the response body inside the call, so the cancellation of the generated
  `Configuration.Timeout` covers a stalled body.

**What a caller sees on a timeout today** (asserted by the tests named in the last column). The surfaces are not
identical yet:

| Language   | Error                                                                                                  | Test |
|------------|--------------------------------------------------------------------------------------------------------|------|
| Python     | `SmriteaError`; `__cause__` is the urllib3 timeout error                                               | `python/tests/test_timeout.py#test_default_client_fails_within_timeout` |
| TypeScript | `SmriteaError`; message `FetchError: ...` when the server never answers, `TimeoutError: ...` when the body stalls | `typescript/tests/default-timeout.test.ts` |
| Go         | `*SmriteaError`; no cause is kept; a stalled body reads as `Code` `INTERNAL_ERROR`, `HTTPStatus` 200, message `Unknown error` | `go/timeout_test.go#TestDefaultHTTPClient_TimesOut` |
| Java       | `SmriteaError`, message `request failed: request timed out after 10000 ms`, cause `HttpTimeoutException`, `HTTPStatus` 0 | `java/src/test/java/ai/smritea/sdk/SmriteaTimeoutTest.java#defaultClientTimesOut` |
| C#         | `TaskCanceledException` from the generated client, not a `SmriteaException`; its inner exception is a `TimeoutException` | `csharp/tests/SmriteaTimeoutTests.cs#SdkDefaultClientFailsWithinTheBudget` |

Known gap: a timeout is not yet a typed, detectable SDK error in every language. Do not change one language's
surface alone; the five must move together (see "What NOT to do").

---

## Auto-Generated Code (`_internal/autogen/`)

- **Excluded from lint**: do not add `_internal/autogen/` to any linter, formatter, or type-checker pass.
- **Single source of truth for exclusions**: each language has ONE config file that specifies the
  `_internal/autogen` exclusion. Do not repeat it in Makefiles, CI scripts, or package manager configs.
    - Python: `pyproject.toml` `[tool.ruff] exclude`
    - TypeScript: `.eslintrc.json` `ignorePatterns` (for ESLint); `tsconfig.json` `exclude` (for tsc)
    - Go: `.golangci.yml` `run.exclude-dirs` (for golangci-lint)
    - Java: `checkstyle-suppressions.xml` `<suppress checks=".*" files="[\\/]_internal[\\/]autogen[\\/]"/>` (for Checkstyle)
    - C#: `.editorconfig` `[_internal/autogen/**/*.cs]` with `generated_code = true` (for Roslyn + dotnet-format)

---

## Lint Rules

- **Never disable a lint rule to work around a code smell.** Fix the code. If a rule genuinely does
  not apply to this project, add it to the ignore list only after explicit confirmation from the user.
- **Never suppress exceptions silently** unless there is an explicitly documented reason added after expicit approval
  from the user. Use narrow
  exception types and handle or re-raise.
- **Import order** is enforced by the linter. Do not manually reorder — let the linter fix it.

---

## Type Safety

- All public methods must have complete type annotations / JSDoc types.
- Do not use `any` / `object` as escape hatches in public API signatures.
  Internal `as never` casts in autogen delegation are acceptable with a comment.
- The `_internal/` layer may use untyped patterns — that is expected and acceptable.

---

## Testing

- Tests live in `tests/` inside each language directory.
- Tests must mock `_internal/` entirely — never make real network calls in unit tests.
- Test the retry logic explicitly: mock the API to return 429 N times then succeed; assert the
  correct number of sleeps occurred with approximately correct durations.
- Test the request timeout against a real loopback server (`127.0.0.1`, ephemeral port, never an external
  host), the only exception to the no-network rule. The server has two stall modes: it accepts and never
  answers, or it sends `HTTP/1.1 200 OK`, `Content-Length: 100` and 10 body bytes and then stalls. Cover
  `add`, `search`, `get` and `delete` in both modes. Keep the suite fast: build the default client through an
  internal factory that takes a short timeout, and separately assert that the public constructor wires the
  10-second constant. Also assert that a supplied client reaches the generated client unchanged with no
  SDK timeout added, and that a 429 retry gets a fresh budget.
- The `_internal/` directory must be excluded from test coverage collection.

---

## Adding a New Language SDK

When adding a new language (e.g. `go/`, `ruby/`, `java/`), the following Makefile targets and
pre-commit hooks are **mandatory**. They must be wired up before any code is merged.

### Required Makefile Targets

Add language-specific targets following the `<target>-<lang>` naming convention, then register
them in the top-level wrappers:

| Target           | What it does                                  | Example                              |
|------------------|-----------------------------------------------|--------------------------------------|
| `format-<lang>`  | Auto-format and auto-fix lint issues in place | `ruff format` + `ruff check --fix`   |
| `lint-<lang>`    | Check-only — fails if anything is wrong       | `ruff check` + `ruff format --check` |
| `build-<lang>`   | Compile / package the SDK                     | `uv build` / `tsc` / `go build`      |
| `test-<lang>`    | Run the full unit test suite                  | `uv run pytest tests/ -v`            |
| `publish-<lang>` | Build and publish to the package registry     | `uv publish` / `npm publish`         |
| `install-<lang>` | Download dependencies; wired into `make install` | `uv sync` / `npm install`         |
| `sync-autogen-<lang>` | Regenerate `_internal/autogen` from the spec | `openapi-generator-cli generate`  |

After adding the language-specific targets, add them to the top-level wrappers:

```makefile
format: format-python format-typescript format-<lang>  ## Auto-format all SDKs
lint:   lint-python   lint-typescript   lint-<lang>    ## Lint all SDKs
test:   test-python   test-typescript   test-<lang>    ## Run all SDK tests
```

Also add all new target names to the `.PHONY` declaration at the top of the Makefile.

### Required Pre-commit Hooks

Add these hooks in `.pre-commit-config.yaml` under the `local` repo, after `sdk-install`:

```yaml
- id: sdk-format
  name: Format (... + <Lang>)
  entry: make format
  language: system
  files: \.(py|ts|<ext>)$        # add the new file extension
  exclude: _internal/autogen/|internal/autogen/
  pass_filenames: false

- id: sdk-lint
  name: Lint (... + <Lang>)
  entry: make lint
  language: system
  files: \.(py|ts|<ext>)$
  exclude: _internal/autogen/|internal/autogen/
  pass_filenames: false

- id: sdk-test
  name: Test (... + <Lang>)
  entry: make test
  language: system
  files: \.(py|ts|<ext>)$
  exclude: _internal/autogen/|internal/autogen/
  pass_filenames: false
```

Also add the extension to the `sdk-build` hook `files` pattern, and the language to the
`sdk-install` and `sdk-build` hook names. Wire `install-<lang>` into `make install`.

### Checklist for a New Language

- [ ] `<lang>/` directory created with `client`, `types`, `errors`, `_internal/autogen` structure
- [ ] `_internal/autogen/` excluded from linter and type-checker (in the language's own config file — not in the
  Makefile)
- [ ] `format-<lang>` target added and wired into `make format`
- [ ] `lint-<lang>` target added and wired into `make lint`
- [ ] `test-<lang>` target added and wired into `make test`
- [ ] `build-<lang>`, `publish-<lang>`, `install-<lang>` and `sync-autogen-<lang>` targets added
- [ ] `.PHONY` updated
- [ ] `files` pattern in the format, lint, build and test hooks updated to include the new extension
- [ ] Hook names updated to list the new language
- [ ] Every contract section above is implemented and has a test: scope, trace ID, HTTP client, errors, retry, timeout
- [ ] `<lang>/README.md` written, and the language added to `README.md`, `SDKs.md` and the release script

---

## What NOT to do

- Do not add language-specific business logic that differs between implementations. If behaviour
  diverges, it is a bug in the diverging implementation.
- Do not add new public methods without updating ALL language implementations simultaneously.
- Do not rely on a per-request timeout alone to bound a call. In Java it ends at the response headers, and in
  Python a pool-level timeout is overridden by the generated client. Prove every timeout against a server that
  stalls the response body (see "Request Timeout").
- Do not make HTTP calls outside of `_internal/`. All networking goes through the auto-gen client.
- Do not catch and swallow errors from `_internal/` without re-raising as a typed `SmriteaError`.
