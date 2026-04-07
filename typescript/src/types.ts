/**
 * Public-facing types for the smritea TypeScript SDK.
 *
 * Memory and SearchResult are re-exported directly from the auto-generated SDK
 * so that the type contract matches the API exactly. Field names are camelCase
 * as produced by the openapi-generator TypeScript mapper.
 */

export type {
  MemoryCreateMemoryResponse as MemoryCreationResult,
  MemoryMemoryResponse as Memory,
  MemorySearchMemoryResponse as SearchResult,
} from './_internal/autogen/models/index.js';
// MemoryCreationResult is the response from add(). Contains all memories
// created from the extracted facts (memories[]), plus extraction metadata:
// factsExtracted, extractionConfidence, skippedCount, updatedCount.

export interface Scope {
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
   * max 64 characters. Mutually exclusive with `participantIds`; if both are
   * set, `conversationId` takes precedence.
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
   * Requires at least 2 IDs; each ID must be 1–64 characters.
   * Mutually exclusive with `conversationId`; if both are set, `conversationId` wins.
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
  scope?: Scope;
  metadata?: Record<string, unknown>;
  /** ISO-8601 datetime string — when this content was created or occurred.
   * Used by the extraction LLM to resolve relative temporal expressions
   * like "last year" or "yesterday". Defaults to current time if omitted. */
  eventOccurredAt?: string;
  /** Importance and temporal decay configuration for this memory. */
  relativeStanding?: RelativeStanding;
}

export interface SearchOptions {
  scope?: Scope;
  limit?: number;
  threshold?: number;
  graphDepth?: number;
  /** ISO-8601 datetime string — only return memories created at or after this time. */
  fromTime?: string;
  /** ISO-8601 datetime string — only return memories created at or before this time. */
  toTime?: string;
  /** ISO-8601 datetime string — return memories valid at exactly this point in time. */
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
   * `$contains`, `$and`, `$or`, `$not`, and wildcard `"*"`.
   * Values must be string, number (int or float). Booleans and nested objects
   * are rejected by the server.
   *
   * Simple equality: `{ department: "engineering" }`
   * Range: `{ level: { $gte: 4 } }`
   * Logical: `{ $and: [{ department: "eng" }, { level: { $gt: 3 } }] }`
   *
   * Note: `$contains` is applied as a post-filter and may return fewer results
   * than `limit`. `$contains` inside `$or` is rejected with HTTP 400.
   */
  metadataFilter?: Record<string, unknown>;
}

export interface SmriteaClientConfig {
  apiKey: string;
  appId: string;
  /** Override API base URL. Defaults to https://api.smritea.ai */
  baseUrl?: string;
  /**
   * Automatic retries on HTTP 429. Uses Retry-After header when the server
   * provides one, otherwise exponential backoff with jitter (capped at 30 s).
   * Set to 0 to disable. Default: 2.
   */
  maxRetries?: number;
}
