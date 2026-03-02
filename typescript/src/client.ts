import {
  AddMemoryOptions,
  Memory,
  SearchOptions,
  SearchResult,
  SmriteaClientConfig,
} from './types.js';
import { throwForStatus } from './errors.js';

export class SmriteaClient {
  private readonly apiKey: string;
  private readonly appId: string;
  private readonly baseUrl: string;

  constructor(config: SmriteaClientConfig) {
    this.apiKey = config.apiKey;
    this.appId = config.appId;
    this.baseUrl = config.baseUrl?.replace(/\/$/, '') ?? 'https://api.smritea.ai';
  }

  async add(content: string, options?: AddMemoryOptions): Promise<Memory> {
    const body: Record<string, unknown> = {
      app_id: this.appId,
      content,
    };
    if (options?.userId !== undefined) {
      body.actor_id = options.userId;
      body.actor_type = 'user';
    } else {
      if (options?.actorId !== undefined) body.actor_id = options.actorId;
      if (options?.actorType !== undefined) body.actor_type = options.actorType;
    }
    if (options?.actorName !== undefined) body.actor_name = options.actorName;
    if (options?.metadata !== undefined) body.metadata = options.metadata;
    if (options?.conversationId !== undefined) body.conversation_id = options.conversationId;

    const data = await this.request<Memory>('POST', '/api/v1/sdk/memories', body);
    return data;
  }

  async search(query: string, options?: SearchOptions): Promise<SearchResult[]> {
    const body: Record<string, unknown> = {
      app_id: this.appId,
      query,
    };
    if (options?.userId !== undefined) {
      body.actor_id = options.userId;
      body.actor_type = 'user';
    } else {
      if (options?.actorId !== undefined) body.actor_id = options.actorId;
      if (options?.actorType !== undefined) body.actor_type = options.actorType;
    }
    if (options?.limit !== undefined) body.limit = options.limit;
    if (options?.method !== undefined) body.method = options.method;
    if (options?.threshold !== undefined) body.threshold = options.threshold;
    if (options?.graphDepth !== undefined) body.graph_depth = options.graphDepth;
    if (options?.conversationId !== undefined) body.conversation_id = options.conversationId;

    const data = await this.request<{ memories: SearchResult[] }>(
      'POST',
      '/api/v1/sdk/memories/search',
      body,
    );
    return data.memories ?? [];
  }

  async get(memoryId: string): Promise<Memory> {
    return this.request<Memory>('GET', `/api/v1/sdk/memories/${encodeURIComponent(memoryId)}`);
  }

  async delete(memoryId: string): Promise<void> {
    await this.requestRaw('DELETE', `/api/v1/sdk/memories/${encodeURIComponent(memoryId)}`);
  }

  // ------------------------------------------------------------------
  // Private helpers
  // ------------------------------------------------------------------

  private async request<T>(method: string, path: string, body?: unknown): Promise<T> {
    const response = await this.requestRaw(method, path, body);
    return response.json() as Promise<T>;
  }

  private async requestRaw(method: string, path: string, body?: unknown): Promise<Response> {
    const url = `${this.baseUrl}${path}`;
    const headers: Record<string, string> = {
      'X-API-Key': this.apiKey,
      'Content-Type': 'application/json',
    };

    const response = await fetch(url, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });

    if (!response.ok) {
      let message = `HTTP ${response.status}`;
      let retryAfter: number | undefined;

      // Attempt to extract error message from JSON body
      try {
        const errBody = (await response.clone().json()) as Record<string, unknown>;
        if (typeof errBody.message === 'string') {
          message = errBody.message;
        } else if (typeof errBody.error === 'string') {
          message = errBody.error;
        }
      } catch {
        // Non-JSON error body -- keep default message
      }

      if (response.status === 429) {
        const retryHeader = response.headers.get('Retry-After');
        if (retryHeader !== null) {
          const parsed = parseInt(retryHeader, 10);
          if (!isNaN(parsed)) retryAfter = parsed;
        }
      }

      throwForStatus(response.status, message, retryAfter);
    }

    return response;
  }
}
