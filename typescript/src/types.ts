/**
 * Public-facing types for the smritea TypeScript SDK.
 *
 * Memory and SearchResult are re-exported directly from the auto-generated SDK
 * so that the type contract matches the API exactly. Field names are camelCase
 * as produced by the openapi-generator TypeScript mapper.
 */

export type {
  MemoryMemoryResponse as Memory,
  MemorySearchMemoryResponse as SearchResult,
} from './_internal/autogen/models/index.js';

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

export interface AddOptions {
  scope?: Scope;
  metadata?: Record<string, unknown>;
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
