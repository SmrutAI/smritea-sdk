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
  **Note**: Java uses `_internal/autogen` (naming convention) + JPMS `module-info.java` to prevent
  external use (unexported package).
- `csharp/` — NuGet package `Smritea.Sdk`, import as `using Smritea.Sdk`
  **Note**: C# uses `internal class` (compiler-enforced, assembly-scoped) for autogen types.
  `<InternalsVisibleTo Include="Smritea.Sdk.Tests" />` in the `.csproj` exposes internals to tests.

When adding a new language, follow the same structure: one `client` file, one `types` file, one `errors`
file, with `_internal/autogen` (or `internal/autogen` for Go) excluded from lint and type-checking.

---

## Public API Contract

Every SDK implementation MUST expose exactly this surface:

| Method   | Signature                                                                                                                                                |
|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------|
| `add`    | `(content, *, scope?, metadata?, event_occurred_at?, relative_standing?) → MemoryCreationResult`                                                         |
| `search` | `(query, *, scope?, limit?, graph_depth?, from_time?, to_time?, valid_at?, method?, reranker_type?) → list[SearchResult]`                                |
| `get`    | `(memory_id) → Memory`                                                                                                                                   |
| `delete` | `(memory_id) → void`                                                                                                                                     |

**`app_id` on constructor**: Set once, then sent as the `X-App-ID` request header on every call —
`add`, `search`, `get`, and `delete`. It is never placed in the request body and is never accepted as
a per-call argument. Callers must never pass it per-call.

**`scope` object**: Groups actor and conversation context fields (`actor_id`, `actor_type`,
`actor_name`, `conversation_id`, `source_type`, `participant_ids`). All fields are
optional. In `add()`, all 5 scope fields are mapped. In `search()`, `actor_id`,
`actor_type`, `conversation_id`, and `participant_ids` are sent as filters.

**Trace ID on constructor**: Optional. When set, it is sent as the `X-Trace-ID` request header on every
call — `add`, `search`, `get`, and `delete`. It is validated in the constructor, before any network call,
against: 8-100 characters of `A-Z`, `a-z`, `0-9`, `.`, `_`, `:`, `-`. Each language uses the name and
failure behaviour below (verified against the source; `None`/`undefined`/`null`/`nil` means unset):

| Language   | Option name                                                                 | Invalid value                                                                                                                                  | Source                                                                                                 |
|------------|-----------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| Python     | `trace_id` keyword on `SmriteaClient(...)`                                  | `ValueError` raised from the constructor                                                                                                       | `python/src/smritea/client.py:61,79-80,86-87`                                                          |
| TypeScript | `traceId` field on `SmriteaClientConfig`                                    | `TypeError` thrown from the constructor                                                                                                        | `typescript/src/client.ts:29-31,35`; `typescript/src/types.ts:145-149`                                 |
| Go         | `ClientConfig.TraceID`                                                      | `NewClient` still returns a client; `Add`, `Search`, `Get`, `Delete`, and `GetAll` each return a `*SmriteaError` with `Code` `INVALID_TRACE_ID` (`ErrCodeInvalidTraceID`) and send no request | `go/client.go:58-64,98,145,204,220,237`; `go/errors.go:12`; `go/types.go:25`                           |
| Java       | `traceId`, 5th argument of the 5-argument constructor `(apiKey, appId, baseUrl, maxRetries, traceId)` | `IllegalArgumentException` thrown from the constructor                                                                    | `java/.../SmriteaClient.java:93-98,114-116`                                                            |
| C#         | `traceId`, last parameter of the constructor                                | `ArgumentException` thrown from the constructor (parameter name `traceId`)                                                                     | `csharp/SmriteaClient.cs:40,44-49,61-64`                                                               |

Language notes: Go treats an empty `TraceID` as unset (no error); every other language rejects `""` because
it fails the pattern. Go's validation counts bytes, not characters. Java's 4-argument constructors pass
`null` for the trace ID, which sends no header.

---

## Error Hierarchy

Every implementation must define this exact hierarchy, mapping HTTP status codes 1:1 onto the
server's errkit HTTP categories (see `docs/plans/175-errkit-unified-error-handling.md` in the
`smritea-cloud` repo for the server-side category contract this mirrors):

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
   minimum 0 to disable).
2. **Sleep strategy** (in priority order):
    - If the server returns a `Retry-After` header: sleep for that many seconds.
    - Otherwise: exponential backoff — `1s × 2^attempt` with ±25 % random jitter.
3. **Hard cap**: sleep duration must never exceed **30 seconds**, regardless of what the server says.
4. **Jitter**: always apply jitter to exponential backoff to avoid thundering herd from concurrent clients.
5. **Final raise**: after all retries are exhausted, raise `SmriteaTooManyRequestsError` with `retry_after`
   populated from the last response header (so callers can inspect it if they implement their own logic).

Reference implementation in Python (`client.py`):

```
_retry_delay(attempt, retry_after):
    if retry_after > 0: return min(retry_after, 30)
    base = min(1.0 * 2^attempt, 30)
    jitter = base * (0.75 + 0.5 * random())
    return min(jitter, 30)
```

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

After adding the language-specific targets, add them to the top-level wrappers:

```makefile
format: format-python format-typescript format-<lang>  ## Auto-format all SDKs
lint:   lint-python   lint-typescript   lint-<lang>    ## Lint all SDKs
test:   test-python   test-typescript   test-<lang>    ## Run all SDK tests
```

Also add all new target names to the `.PHONY` declaration at the top of the Makefile.

### Required Pre-commit Hooks

Add four hooks in `.pre-commit-config.yaml` under the `local` repo, in this exact order:

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

Note: there is no separate `build` hook — build is covered implicitly by the type-check step
inside `lint-<lang>`. Add a dedicated build hook only if the language has a separate compile
step that is not part of lint (e.g. a compiled language where type errors only surface at
build time).

### Checklist for a New Language

- [ ] `<lang>/` directory created with `client`, `types`, `errors`, `_internal/autogen` structure
- [ ] `_internal/autogen/` excluded from linter and type-checker (in the language's own config file — not in the
  Makefile)
- [ ] `format-<lang>` target added and wired into `make format`
- [ ] `lint-<lang>` target added and wired into `make lint`
- [ ] `test-<lang>` target added and wired into `make test`
- [ ] `build-<lang>` and `publish-<lang>` targets added
- [ ] `.PHONY` updated
- [ ] `files` pattern in all three pre-commit hooks updated to include the new extension
- [ ] Hook names updated to list the new language

---

## What NOT to do

- Do not add language-specific business logic that differs between implementations. If behaviour
  diverges, it is a bug in the diverging implementation.
- Do not add new public methods without updating ALL language implementations simultaneously.
- Do not make HTTP calls outside of `_internal/`. All networking goes through the auto-gen client.
- Do not catch and swallow errors from `_internal/` without re-raising as a typed `SmriteaError`.
