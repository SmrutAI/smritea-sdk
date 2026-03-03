import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { SmriteaClient } from '../src/client.js';
import { ResponseError } from '../src/_internal/autogen/runtime.js';
import {
  SmriteaError,
  SmriteaAuthError,
  SmriteaValidationError,
  SmriteaNotFoundError,
  SmriteaQuotaError,
  SmriteaRateLimitError,
} from '../src/errors.js';

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

interface MockApi {
  createMemory: ReturnType<typeof vi.fn>;
  searchMemories: ReturnType<typeof vi.fn>;
  getMemory: ReturnType<typeof vi.fn>;
  deleteMemory: ReturnType<typeof vi.fn>;
}

function createClientWithMock(overrides?: { maxRetries?: number }): {
  client: SmriteaClient;
  mockApi: MockApi;
} {
  const client = new SmriteaClient({
    apiKey: 'test-key',
    appId: 'app-123',
    ...overrides,
  });

  const mockApi: MockApi = {
    createMemory: vi.fn(),
    searchMemories: vi.fn(),
    getMemory: vi.fn(),
    deleteMemory: vi.fn(),
  };

  // Replace the private api property with our mock
  (client as any).api = mockApi;

  return { client, mockApi };
}

function makeResponseError(
  status: number,
  retryAfter?: string,
  message = `HTTP ${status}`,
): ResponseError {
  const headers = new Map<string, string>();
  if (retryAfter !== undefined) {
    headers.set('Retry-After', retryAfter);
  }

  const mockResponse = {
    status,
    headers: { get: (h: string) => headers.get(h) ?? null },
  } as unknown as Response;

  return new ResponseError(mockResponse, message);
}

// ---------------------------------------------------------------------------
// 1. userId convenience mapping
// ---------------------------------------------------------------------------

describe('userId convenience mapping', () => {
  it('add() maps userId to actorId with actorType "user"', async () => {
    const { client, mockApi } = createClientWithMock();
    mockApi.createMemory.mockResolvedValue({ id: 'mem-1' });

    await client.add('hello world', { userId: 'alice' });

    expect(mockApi.createMemory).toHaveBeenCalledOnce();
    const arg = mockApi.createMemory.mock.calls[0][0];
    expect(arg.request.actorId).toBe('alice');
    expect(arg.request.actorType).toBe('user');
  });

  it('add() passes actorId and actorType through when userId is not set', async () => {
    const { client, mockApi } = createClientWithMock();
    mockApi.createMemory.mockResolvedValue({ id: 'mem-1' });

    await client.add('hello world', { actorId: 'bot-1', actorType: 'agent' });

    const arg = mockApi.createMemory.mock.calls[0][0];
    expect(arg.request.actorId).toBe('bot-1');
    expect(arg.request.actorType).toBe('agent');
  });

  it('search() maps userId to actorId with actorType "user"', async () => {
    const { client, mockApi } = createClientWithMock();
    mockApi.searchMemories.mockResolvedValue({ memories: [] });

    await client.search('find stuff', { userId: 'alice' });

    expect(mockApi.searchMemories).toHaveBeenCalledOnce();
    const arg = mockApi.searchMemories.mock.calls[0][0];
    expect(arg.request.actorId).toBe('alice');
    expect(arg.request.actorType).toBe('user');
  });

  it('search() passes actorId and actorType through when userId is not set', async () => {
    const { client, mockApi } = createClientWithMock();
    mockApi.searchMemories.mockResolvedValue({ memories: [] });

    await client.search('find stuff', { actorId: 'bot-1', actorType: 'agent' });

    const arg = mockApi.searchMemories.mock.calls[0][0];
    expect(arg.request.actorId).toBe('bot-1');
    expect(arg.request.actorType).toBe('agent');
  });
});

// ---------------------------------------------------------------------------
// 2. Error mapping
// ---------------------------------------------------------------------------

describe('error mapping', () => {
  const statusToError: Array<[number, new (...args: any[]) => SmriteaError]> = [
    [400, SmriteaValidationError],
    [401, SmriteaAuthError],
    [402, SmriteaQuotaError],
    [404, SmriteaNotFoundError],
    [429, SmriteaRateLimitError],
    [500, SmriteaError],
  ];

  it.each(statusToError)(
    'HTTP %i maps to %s',
    async (status, ExpectedErrorClass) => {
      const { client, mockApi } = createClientWithMock({ maxRetries: 0 });
      mockApi.createMemory.mockRejectedValue(makeResponseError(status));

      await expect(client.add('content')).rejects.toBeInstanceOf(ExpectedErrorClass);
    },
  );

  it('HTTP 429 populates retryAfter from Retry-After header', async () => {
    const { client, mockApi } = createClientWithMock({ maxRetries: 0 });
    mockApi.createMemory.mockRejectedValue(makeResponseError(429, '10'));

    try {
      await client.add('content');
      expect.unreachable('should have thrown');
    } catch (err) {
      expect(err).toBeInstanceOf(SmriteaRateLimitError);
      expect((err as SmriteaRateLimitError).retryAfter).toBe(10);
    }
  });

  it('non-ResponseError becomes base SmriteaError', async () => {
    const { client, mockApi } = createClientWithMock({ maxRetries: 0 });
    mockApi.createMemory.mockRejectedValue(new Error('network failure'));

    await expect(client.add('content')).rejects.toBeInstanceOf(SmriteaError);
    await expect(client.add('content')).rejects.not.toBeInstanceOf(SmriteaAuthError);
  });
});

// ---------------------------------------------------------------------------
// 3. Retry logic
// ---------------------------------------------------------------------------

describe('retry logic', () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('retries on 429 twice then succeeds (default maxRetries=2)', async () => {
    const { client, mockApi } = createClientWithMock();

    mockApi.createMemory
      .mockRejectedValueOnce(makeResponseError(429, '1'))
      .mockRejectedValueOnce(makeResponseError(429, '1'))
      .mockResolvedValueOnce({ id: 'mem-1' });

    const promise = client.add('content');

    // Attach handler before advancing timers to avoid unhandled rejection
    const resultPromise = promise.then((r) => r);

    await vi.runAllTimersAsync();

    const result = await resultPromise;
    expect(result).toEqual({ id: 'mem-1' });
    expect(mockApi.createMemory).toHaveBeenCalledTimes(3);
  });

  it('maxRetries=0 raises SmriteaRateLimitError immediately without retrying', async () => {
    const { client, mockApi } = createClientWithMock({ maxRetries: 0 });
    mockApi.createMemory.mockRejectedValue(makeResponseError(429, '5'));

    await expect(client.add('content')).rejects.toBeInstanceOf(SmriteaRateLimitError);
    expect(mockApi.createMemory).toHaveBeenCalledTimes(1);
  });

  it('three 429s with maxRetries=2 exhausts retries and throws', async () => {
    const { client, mockApi } = createClientWithMock({ maxRetries: 2 });

    mockApi.createMemory
      .mockRejectedValueOnce(makeResponseError(429, '1'))
      .mockRejectedValueOnce(makeResponseError(429, '1'))
      .mockRejectedValueOnce(makeResponseError(429, '1'));

    const promise = client.add('content');

    // Attach rejection handler before advancing timers to avoid unhandled rejection
    const assertion = expect(promise).rejects.toBeInstanceOf(SmriteaRateLimitError);

    await vi.runAllTimersAsync();

    await assertion;
    // 1 initial + 2 retries = 3 total calls
    expect(mockApi.createMemory).toHaveBeenCalledTimes(3);
  });

  it('non-429 error (404) is not retried', async () => {
    const { client, mockApi } = createClientWithMock();
    mockApi.createMemory.mockRejectedValue(makeResponseError(404));

    await expect(client.add('content')).rejects.toBeInstanceOf(SmriteaNotFoundError);
    expect(mockApi.createMemory).toHaveBeenCalledTimes(1);
  });
});

// ---------------------------------------------------------------------------
// 4. retryDelayMs calculation
// ---------------------------------------------------------------------------

describe('retryDelayMs calculation', () => {
  it('uses 90% of Retry-After in milliseconds', () => {
    const { client } = createClientWithMock();
    const delay = (client as any).retryDelayMs(0, 10);
    // 10 * 0.9 * 1000 = 9000
    expect(delay).toBe(9000);
  });

  it('caps at 30s (RETRY_CAP_MS) even with large Retry-After', () => {
    const { client } = createClientWithMock();
    const delay = (client as any).retryDelayMs(0, 40);
    // 40 * 0.9 * 1000 = 36000, capped at 30000
    expect(delay).toBe(30_000);
  });

  it('falls back to exponential backoff with jitter when no Retry-After', () => {
    const { client } = createClientWithMock();
    const delay = (client as any).retryDelayMs(0, undefined);
    // Exponential base for attempt 0: min(1000 * 2^0, 30000) = 1000
    // Jitter: 1000 * (0.75 + 0.5 * random) => [750, 1250], capped at 30000
    expect(delay).toBeGreaterThanOrEqual(750);
    expect(delay).toBeLessThanOrEqual(30_000);
  });

  it('treats retryAfter=0 as missing and falls back to exponential', () => {
    const { client } = createClientWithMock();
    const delay = (client as any).retryDelayMs(0, 0);
    // Same jitter range as undefined case
    expect(delay).toBeGreaterThanOrEqual(750);
    expect(delay).toBeLessThanOrEqual(30_000);
  });
});

// ---------------------------------------------------------------------------
// 5. getAll throws not-implemented
// ---------------------------------------------------------------------------

describe('getAll', () => {
  it('throws an error containing "not yet available"', async () => {
    const { client } = createClientWithMock();
    await expect(client.getAll()).rejects.toThrow(/not yet available/);
  });
});
