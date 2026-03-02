/**
 * Public-facing types for the smritea TypeScript SDK.
 * Field names use snake_case to match the API JSON response contract.
 */

export interface Memory {
  id: string;
  app_id: string;
  actor_id: string;
  content: string;
  metadata?: Record<string, unknown>;
  actor_type?: string;
  actor_name?: string;
  active_from: string;
  active_to?: string;
  conversation_id?: string;
  conversation_message_id?: string;
  created_at: string;
  updated_at: string;
}

export interface SearchResult {
  memory: Memory;
  score: number;
}

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
