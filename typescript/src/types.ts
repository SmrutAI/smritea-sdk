/**
 * Public-facing types for the smritea TypeScript SDK.
 *
 * Memory and SearchResult are re-exported directly from the auto-generated SDK
 * so that the type contract matches the API exactly. Field names are camelCase
 * as produced by the openapi-generator TypeScript mapper.
 */

export type {
  CreateMemoryResponse as MemoryCreationResult,
  MemoryResponse as Memory,
  SearchMemoryResponse as SearchResult,
} from './_internal/autogen/models/index.js';
// MemoryCreationResult is the response from add(). Contains all memories
// created from the extracted facts (memories[]), plus extraction metadata:
// factsExtracted, skippedCount, updatedCount.

/**
 * Actor and conversation context for add/search, aligned with the OSS `MemoryScope` type
 * (maps to `commondto.MemoryScope` on the wire — not the internal OpenAPI struct name).
 */
export interface MemoryScope {
  /**
   * Identifier for the actor (user, agent, or system) associated with this memory.
   * Must be set together with `actorType`; max 64 characters.
   */
  actorId?: string;
  /**
   * Role of the actor. Accepted values: `"user"`, `"agent"`, `"system"`.
   * Must be set together with `actorId`.
   */
  actorType?: 'user' | 'agent' | 'system';
  /**
   * Display name of the actor. Optional — used for human-readable labels;
   * max 255 characters.
   */
  actorName?: string;
  /**
   * Conversation thread this memory belongs to or should be searched within;
   * max 64 characters. Cannot be combined with `participantIds`: a search that
   * sets both is rejected with HTTP 400.
   */
  conversationId?: string;
  /**
   * Origin of the memory. Accepted values: `"conversation"`, `"document"`, `"api"`.
   * Defaults to `"api"` when omitted.
   */
  sourceType?: 'conversation' | 'document' | 'api';
  /**
   * Search across conversations where **all** listed actors participated (AND semantics).
   * The service expands this list into the matching conversation IDs before querying.
   * One ID selects every conversation that actor took part in. Each ID must be 1–64 characters.
   * Cannot be combined with `conversationId`: a search that sets both is rejected with HTTP 400.
   * Only relevant for `search` — ignored on `add`.
   */
  participantIds?: string[];
}

export interface RelativeStanding {
  /** How important is this memory (0.0–1.0). Higher = ranks higher in search. */
  importance?: number;
  /** Rate of relevance decay over time (>=0). 0 = no decay (memory score is pinned
   * permanently). 0.2 = light decay (default). 1.0 = standard. 3.0+ = aggressive. */
  decayFactor?: number;
  /** Decay curve shape. Accepted values: `"exponential"`, `"gaussian"`, `"linear"`. */
  decayFunction?: 'exponential' | 'gaussian' | 'linear';
}

export interface AddOptions {
  scope?: MemoryScope;
  metadata?: Record<string, unknown>;
  /** RFC 3339 datetime string — when this content was created or occurred.
   * Used by the extraction LLM to resolve relative temporal expressions
   * like "last year" or "yesterday". Defaults to current time if omitted. */
  eventOccurredAt?: string;
  /** Importance and temporal decay configuration for this memory. */
  relativeStanding?: RelativeStanding;
}

export interface SearchOptions {
  /**
   * Identifies who is asking this search query. NEVER used for filtering — it
   * does not narrow or bias which memories are returned. The backend resolves
   * it to the speaker's entity and uses it for speaker-context query
   * augmentation and audit attribution. Independent of `scope`, which remains
   * the only filtering mechanism. Omit for anonymous or system-initiated
   * searches. Max 64 characters.
   */
  speakerActorId?: string;
  scope?: MemoryScope;
  /** Number of results. It must not exceed the server's top_n_max (default 20). Omit to use the app's top_n. */
  limit?: number;
  /** Graph traversal depth, 1-5; omit to use the system default. */
  graphDepth?: number;
  /**
   * RFC 3339 datetime string — start of a time range. Returns memories whose validity period overlaps
   * [fromTime, infinity). Can be used alone (open-ended). With toTime the range is [fromTime, toTime].
   * Cannot be combined with validAt.
   */
  fromTime?: string;
  /**
   * RFC 3339 datetime string — end of a time range (inclusive). Returns memories whose validity period
   * overlaps (-infinity, toTime]. Can be used alone (open-ended). With fromTime the range is
   * [fromTime, toTime]. Cannot be combined with validAt.
   */
  toTime?: string;
  /** RFC 3339 datetime string — return memories valid at exactly this moment. Cannot be combined with fromTime or toTime. */
  validAt?: string;
  /** Search method override. Accepted values: `"quick_search"`, `"deep_search"`,
   * `"context_aware_search"`. Defaults to app config if omitted. */
  method?: 'quick_search' | 'deep_search' | 'context_aware_search';
  /** Reranker override. Accepted values: `"rrf_temporal"`, `"rrf"`, `"temporal"`,
   * `"node_distance"`, `"mmr"`, `"cross_encoder"`. Only applies to deep_search.
   * Defaults to app config if omitted. */
  rerankerType?: 'rrf_temporal' | 'rrf' | 'temporal' | 'node_distance' | 'mmr' | 'cross_encoder';
  /**
   * MongoDB-style operator DSL for filtering search results by their metadata.
   * Supports `$eq`, `$ne`, `$gt`, `$gte`, `$lt`, `$lte`, `$in`, `$nin`,
   * `$and`, `$or`, `$not`, and wildcard `"*"`.
   * Values must be string, number (int or float). Booleans and nested objects
   * are rejected by the server.
   *
   * Simple equality: `{ department: "engineering" }`
   * Range: `{ level: { $gte: 4 } }`
   * Logical: `{ $and: [{ department: "eng" }, { level: { $gt: 3 } }] }`
   *
   * Note: `$contains` is not supported. A search that uses it is rejected with
   * HTTP 422 (code `CONTAINS_NOT_SUPPORTED`).
   */
  metadataFilter?: Record<string, unknown>;
}

export interface SmriteaClientConfig {
  apiKey: string;
  appId: string;
  /** Override API base URL. Defaults to https://api-us.smritea.ai */
  baseUrl?: string;
  /**
   * Automatic retries on HTTP 429. Uses Retry-After header when the server
   * provides one, otherwise exponential backoff with jitter (capped at 30 s).
   * Default: 2. Set to 0 to disable retries. A negative value is treated as 0.
   */
  maxRetries?: number;
  /**
   * Optional trace ID sent as the `X-Trace-ID` header on every request.
   * Must be 8-100 characters of A-Z, a-z, 0-9, dot, underscore, colon or hyphen.
   * An invalid value throws a `TypeError` from the constructor, before any network call.
   */
  traceId?: string;
  /**
   * Optional `fetch` function that sends every request. Use it to set your own HTTP agent,
   * proxy, timeout or instrumentation. Default: the global `fetch`.
   */
  fetch?: typeof fetch;
}
