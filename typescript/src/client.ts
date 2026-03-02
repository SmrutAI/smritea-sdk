import { Configuration, ResponseError } from './_internal/autogen/runtime.js';
import { SDKMemoryApi } from './_internal/autogen/apis/SDKMemoryApi.js';
import type { AddMemoryOptions, Memory, SearchOptions, SearchResult, SmriteaClientConfig } from './types.js';
import {
  SmriteaAuthError,
  SmriteaError,
  SmriteaNotFoundError,
  SmriteaQuotaError,
  SmriteaRateLimitError,
  SmriteaValidationError,
} from './errors.js';

export class SmriteaClient {
  private readonly appId: string;
  private readonly api: SDKMemoryApi;

  constructor(config: SmriteaClientConfig) {
    this.appId = config.appId;
    const configuration = new Configuration({
      basePath: config.baseUrl?.replace(/\/$/, '') ?? 'https://api.smritea.ai',
      apiKey: config.apiKey,
    });
    this.api = new SDKMemoryApi(configuration);
  }

  async add(content: string, options?: AddMemoryOptions): Promise<Memory> {
    const actorId = options?.userId ?? options?.actorId;
    const actorType = options?.userId !== undefined ? 'user' : options?.actorType;

    try {
      return await this.api.createMemory({
        request: {
          appId: this.appId,
          content,
          actorId,
          actorType: actorType as never,
          actorName: options?.actorName,
          metadata: options?.metadata as never,
          conversationId: options?.conversationId,
        },
      });
    } catch (err) {
      return this.handleError(err);
    }
  }

  async search(query: string, options?: SearchOptions): Promise<SearchResult[]> {
    const actorId = options?.userId ?? options?.actorId;
    const actorType = options?.userId !== undefined ? 'user' : options?.actorType;

    try {
      const response = await this.api.searchMemories({
        request: {
          appId: this.appId,
          query,
          actorId,
          actorType: actorType as never,
          limit: options?.limit,
          method: options?.method as never,
          threshold: options?.threshold,
          graphDepth: options?.graphDepth,
          conversationId: options?.conversationId,
        },
      });
      return response.memories ?? [];
    } catch (err) {
      return this.handleError(err);
    }
  }

  async get(memoryId: string): Promise<Memory> {
    try {
      return await this.api.getMemory({ memoryId });
    } catch (err) {
      return this.handleError(err);
    }
  }

  async delete(memoryId: string): Promise<void> {
    try {
      await this.api.deleteMemory({ memoryId });
    } catch (err) {
      this.handleError(err);
    }
  }

  // ------------------------------------------------------------------
  // Private helpers
  // ------------------------------------------------------------------

  private handleError(err: unknown): never {
    if (err instanceof ResponseError) {
      const status = err.response.status;
      const message = err.message;
      switch (status) {
        case 400: throw new SmriteaValidationError(message, status);
        case 401: throw new SmriteaAuthError(message, status);
        case 402: throw new SmriteaQuotaError(message, status);
        case 404: throw new SmriteaNotFoundError(message, status);
        case 429: {
          const retryAfter = err.response.headers.get('Retry-After');
          const parsed = retryAfter !== null ? parseInt(retryAfter, 10) : undefined;
          throw new SmriteaRateLimitError(message, status, isNaN(parsed ?? NaN) ? undefined : parsed);
        }
        default: throw new SmriteaError(message, status);
      }
    }
    throw new SmriteaError(String(err));
  }
}
