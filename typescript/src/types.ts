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
  /** Actor ID for scoping. */
  actorId?: string;
  actorType?: 'user' | 'agent' | 'system';
  actorName?: string;
  conversationId?: string;
  conversationMessageId?: string;
  sourceType?: 'conversation' | 'document' | 'api';
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
