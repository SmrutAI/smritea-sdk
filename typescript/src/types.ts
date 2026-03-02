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

export interface AddMemoryOptions {
  /** Shorthand: sets actor_id and actor_type="user". Takes precedence if actorId also set. */
  userId?: string;
  /** Explicit actor ID. Use when actor_type is not "user". */
  actorId?: string;
  actorType?: string;
  actorName?: string;
  metadata?: Record<string, unknown>;
  conversationId?: string;
}

export interface SearchOptions {
  /** Shorthand: sets actor_id and actor_type="user" filter. */
  userId?: string;
  actorId?: string;
  actorType?: string;
  limit?: number;
  method?: string;
  threshold?: number;
  graphDepth?: number;
  conversationId?: string;
}

export interface SmriteaClientConfig {
  apiKey: string;
  appId: string;
  /** Override API base URL. Defaults to https://api.smritea.ai */
  baseUrl?: string;
}
