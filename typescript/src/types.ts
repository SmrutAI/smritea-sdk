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

export interface AddOptions {
  /** Shorthand: sets actor_id and actor_type="user". Takes precedence if actorId also set. */
  userId?: string;
  /** Explicit actor ID. Use when actor_type is not "user". */
  actorId?: string;
  actorType?: 'user' | 'agent' | 'system';
  actorName?: string;
  metadata?: Record<string, unknown>;
  conversationId?: string;
}

export interface SearchOptions {
  /** Shorthand: sets actor_id and actor_type="user" filter. */
  userId?: string;
  actorId?: string;
  actorType?: 'user' | 'agent' | 'system';
  limit?: number;
  method?: string;
  threshold?: number;
  graphDepth?: number;
  conversationId?: string;
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
