import { Configuration, ResponseError } from './_internal/autogen/runtime.js';
import { SDKMemoryApi } from './_internal/autogen/apis/SDKMemoryApi.js';
import type { AddOptions, Memory, MemoryCreationResult, SearchOptions, SearchResult, SmriteaClientConfig } from './types.js';
import {
  SmriteaBadRequestError,
  SmriteaConflictError,
  SmriteaError,
  SmriteaForbiddenError,
  SmriteaNotFoundError,
  SmriteaPaymentRequiredError,
  SmriteaTooManyRequestsError,
  SmriteaUnauthorizedError,
  SmriteaUnprocessableError,
} from './errors.js';

const RETRY_CAP_MS = 30_000;

export class SmriteaClient {
  private readonly appId: string;
  private readonly api: SDKMemoryApi;
  private readonly maxRetries: number;

  constructor(config: SmriteaClientConfig) {
    this.appId = config.appId;
    this.maxRetries = config.maxRetries ?? 2;
    const configuration = new Configuration({
      basePath: config.baseUrl?.replace(/\/$/, '') ?? 'https://api-us.smritea.ai',
      apiKey: config.apiKey,
    });
    this.api = new SDKMemoryApi(configuration);
  }

  async add(content: string, options?: AddOptions): Promise<MemoryCreationResult> {
    if (options?.metadata !== undefined) {
      const m = options.metadata;
      if (typeof m !== 'object' || m === null || Array.isArray(m)) {
        throw new SmriteaBadRequestError('metadata must be a plain object (dictionary)', 400);
      }
    }

    return this.withRetry(() =>
      this.api.createMemory({
        xAppID: this.appId,
        request: {
          content,
          scope: options?.scope
            ? {
                actorId: options.scope.actorId,
                actorType: options.scope.actorType,
                actorName: options.scope.actorName,
                conversationId: options.scope.conversationId,
                sourceType: options.scope.sourceType,
                participantIds: options.scope.participantIds,
              }
            : {},
          metadata: options?.metadata as never,
          eventOccurredAt: options?.eventOccurredAt,
          relativeStanding: options?.relativeStanding
            ? {
                importance: options.relativeStanding.importance,
                decayFactor: options.relativeStanding.decayFactor,
                decayFunction: options.relativeStanding.decayFunction,
              }
            : undefined,
        },
      }),
    );
  }

  async search(query: string, options?: SearchOptions): Promise<SearchResult[]> {
    const response = await this.withRetry(() =>
      this.api.searchMemories({
        xAppID: this.appId,
        request: {
          query,
          speakerActorId: options?.speakerActorId,
          scope: options?.scope
            ? {
                actorId: options.scope.actorId,
                actorType: options.scope.actorType,
                conversationId: options.scope.conversationId,
                participantIds: options.scope.participantIds,
              }
            : undefined,
          limit: options?.limit,
          graphDepth: options?.graphDepth,
          fromTime: options?.fromTime,
          toTime: options?.toTime,
          validAt: options?.validAt,
          method: options?.method,
          rerankerType: options?.rerankerType,
          metadataFilter: options?.metadataFilter as never,
        },
      }),
    );
    return response.memories ?? [];
  }

  async get(memoryId: string): Promise<Memory> {
    return this.withRetry(() => this.api.getMemory({ xAppID: this.appId, memoryId }));
  }

  async delete(memoryId: string): Promise<void> {
    await this.withRetry(() => this.api.deleteMemory({ xAppID: this.appId, memoryId }));
  }

  async getAll(options?: { limit?: number; offset?: number }): Promise<Memory[]> {
    void options; // parameter reserved for future use
    throw new Error(
      'getAll() is not yet available. The list memories endpoint is pending ' +
        'dashboard testing. Use search() to find specific memories.',
    );
  }

  // ------------------------------------------------------------------
  // Private helpers
  // ------------------------------------------------------------------

  /**
   * Execute fn, retrying on HTTP 429 up to maxRetries times.
   *
   * Sleep strategy per attempt:
   * 1. Retry-After header value (seconds), capped at 30 s.
   * 2. Exponential backoff (1 s, 2 s, 4 s, …) with ±25 % jitter, capped at 30 s.
   *
   * After all retries are exhausted the 429 is re-raised as SmriteaTooManyRequestsError
   * with retryAfter populated from the final response header if available.
   */
  private async withRetry<T>(fn: () => Promise<T>): Promise<T> {
    for (let attempt = 0; attempt <= this.maxRetries; attempt++) {
      try {
        return await fn();
      } catch (err) {
        if (err instanceof ResponseError && err.response.status === 429 && attempt < this.maxRetries) {
          await new Promise<void>((resolve) =>
            setTimeout(resolve, this.retryDelayMs(attempt, this.parseRetryAfter(err.response))),
          );
          continue;
        }
        await this.handleError(err);
      }
    }
    throw new Error('unreachable');
  }

  /** Calculate sleep duration in milliseconds for a retry attempt. */
  private retryDelayMs(attempt: number, retryAfterSeconds?: number): number {
    if (retryAfterSeconds !== undefined && retryAfterSeconds > 0) {
      // Sleep for 90% of the server-specified wait time. The 10% headroom
      // accounts for one-way network latency: by the time the request
      // arrives at the server the rate-limit window will have expired.
      return Math.min(retryAfterSeconds * 0.9 * 1000, RETRY_CAP_MS);
    }
    // Exponential backoff: 1 s, 2 s, 4 s, …
    const baseMs = Math.min(1000 * Math.pow(2, attempt), RETRY_CAP_MS);
    // ±25 % jitter
    const jitter = baseMs * (0.75 + 0.5 * Math.random());
    return Math.min(jitter, RETRY_CAP_MS);
  }

  /** Extract the Retry-After header value in seconds, or undefined. */
  private parseRetryAfter(response: Response): number | undefined {
    const header = response.headers.get('Retry-After');
    if (header === null) return undefined;
    const parsed = parseInt(header, 10);
    return isNaN(parsed) ? undefined : parsed;
  }

  private async handleError(err: unknown): Promise<never> {
    if (err instanceof ResponseError) {
      const status = err.response.status;
      const errorData = await this.extractErrorData(err.response);
      const message = errorData.message || err.message;
      const code = errorData.code;
      const body = errorData.body;
      const retryable = errorData.retryable;
      switch (status) {
        case 400: throw new SmriteaBadRequestError(message, status, code, body, retryable);
        case 401: throw new SmriteaUnauthorizedError(message, status, code, body, retryable);
        case 402: throw new SmriteaPaymentRequiredError(message, status, code, body, retryable);
        case 403: throw new SmriteaForbiddenError(message, status, code, body, retryable);
        case 404: throw new SmriteaNotFoundError(message, status, code, body, retryable);
        case 409: throw new SmriteaConflictError(message, status, code, body, retryable);
        case 422: throw new SmriteaUnprocessableError(message, status, code, body, retryable);
        case 429: throw new SmriteaTooManyRequestsError(message, status, this.parseRetryAfter(err.response), code, body, true);
        default: throw new SmriteaError(message, status, code, body, retryable);
      }
    }
    throw new SmriteaError(String(err));
  }

  /** Attempt to extract error data ("message", "code", and "retryable" fields) from the response body JSON. */
  private async extractErrorData(response: Response): Promise<{ message: string; code?: string; body?: unknown; retryable?: boolean }> {
    try {
      const body = await response.clone().json();
      if (body && typeof body === 'object') {
        const message = (body as Record<string, unknown>).message;
        const code = (body as Record<string, unknown>).code;
        const retryable = (body as Record<string, unknown>).retryable;
        if (typeof message === 'string' && message) {
          return {
            message,
            code: typeof code === 'string' ? code : undefined,
            retryable: typeof retryable === 'boolean' ? retryable : undefined,
            body,
          };
        }
      }
      return { message: '', body };
    } catch {
      // JSON parsing failed; fall through to caller-provided fallback.
    }
    return { message: '' };
  }
}
