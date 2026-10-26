import { afterEach, describe, expect, it, vi } from 'vitest';
import { SmriteaClient } from '../src/client.js';
import { SmriteaNotFoundError } from '../src/errors.js';

const BASE_CONFIG = { apiKey: 'test-key', appId: 'app_test', baseUrl: 'http://127.0.0.1:9', maxRetries: 0 };

function notFound(): Response {
  return new Response(JSON.stringify({ code: 'MEMORY_NOT_FOUND', message: 'Memory not found' }), {
    status: 404,
    headers: { 'Content-Type': 'application/json' },
  });
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('fetch option', () => {
  it.each([
    ['without traceId', undefined],
    ['with traceId', 'trace-abc12345'],
  ])('sends every request through the custom fetch (%s)', async (_label, traceId) => {
    const globalFetch = vi.fn(async () => notFound());
    vi.stubGlobal('fetch', globalFetch);
    const customFetch = vi.fn(async () => notFound());
    const client = new SmriteaClient({ ...BASE_CONFIG, traceId, fetch: customFetch });

    await expect(client.get('mem_1')).rejects.toBeInstanceOf(SmriteaNotFoundError);

    expect(customFetch).toHaveBeenCalledTimes(1);
    expect(globalFetch).not.toHaveBeenCalled();
    const [url, init] = customFetch.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('http://127.0.0.1:9/api/v1/sdk/memories/mem_1');
    const headers = init.headers as Record<string, string>;
    expect(headers['X-API-Key']).toBe('test-key');
    expect(headers['X-Trace-ID']).toBe(traceId);
  });

  it.each([
    ['option absent', {}],
    ['option undefined', { fetch: undefined }],
  ])('uses the global fetch when the %s', async (_label, extra) => {
    const globalFetch = vi.fn(async () => notFound());
    vi.stubGlobal('fetch', globalFetch);
    const client = new SmriteaClient({ ...BASE_CONFIG, ...extra });

    await expect(client.get('mem_1')).rejects.toBeInstanceOf(SmriteaNotFoundError);

    expect(globalFetch).toHaveBeenCalledTimes(1);
  });
});
