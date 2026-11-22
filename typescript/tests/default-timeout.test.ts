import { createServer as createHttpServer } from 'node:http';
import { createServer as createTcpServer } from 'node:net';
import type { AddressInfo, Server, Socket } from 'node:net';
import { afterAll, afterEach, beforeAll, describe, expect, it, vi } from 'vitest';

// The client is built with a SHORT timeout in these tests: the mocked factory below replaces the
// timeout it receives with `timeoutOverride.ms`, while still recording the real argument.
const { timeoutOverride, createDefaultFetchSpy } = vi.hoisted(() => ({
  timeoutOverride: { ms: undefined as number | undefined },
  createDefaultFetchSpy: vi.fn(),
}));

vi.mock('../src/default-fetch.js', async (importOriginal) => {
  const actual = await importOriginal<typeof import('../src/default-fetch.js')>();
  createDefaultFetchSpy.mockImplementation((ms: number, baseFetch?: typeof fetch) =>
    actual.createDefaultFetch(timeoutOverride.ms ?? ms, baseFetch),
  );
  return { ...actual, createDefaultFetch: createDefaultFetchSpy };
});

import { SmriteaClient } from '../src/client.js';
import { DEFAULT_TIMEOUT_MS, createDefaultFetch } from '../src/default-fetch.js';
import { SmriteaError, SmriteaNotFoundError } from '../src/errors.js';

const SHORT_TIMEOUT_MS = 300;
// Upper bound for a call that must have failed because of SHORT_TIMEOUT_MS.
const MAX_ELAPSED_MS = 3_000;

type StallMode = 'never answers' | 'stalls the body';

/**
 * A raw TCP server that accepts every connection and stays silent (mode "never answers"), or
 * sends a status line, headers, a Content-Length of 100 and 10 body bytes, then stalls
 * (mode "stalls the body").
 */
class StallServer {
  private readonly server: Server;
  private readonly sockets = new Set<Socket>();

  constructor(mode: StallMode) {
    this.server = createTcpServer((socket) => {
      this.sockets.add(socket);
      socket.on('close', () => this.sockets.delete(socket));
      socket.on('error', () => undefined);
      if (mode === 'stalls the body') {
        socket.on('data', () => {
          socket.write(
            'HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 100\r\n\r\n{"memories"',
          );
        });
      }
    });
  }

  async start(): Promise<string> {
    await new Promise<void>((resolve) => this.server.listen(0, '127.0.0.1', resolve));
    return `http://127.0.0.1:${(this.server.address() as AddressInfo).port}`;
  }

  async stop(): Promise<void> {
    this.sockets.forEach((socket) => socket.destroy());
    await new Promise<void>((resolve) => this.server.close(() => resolve()));
  }
}

const STALL_MODES: StallMode[] = ['never answers', 'stalls the body'];
const stallServers = new Map<StallMode, StallServer>();
const stallUrls = new Map<StallMode, string>();

beforeAll(async () => {
  for (const mode of STALL_MODES) {
    const server = new StallServer(mode);
    stallServers.set(mode, server);
    stallUrls.set(mode, await server.start());
  }
});

afterAll(async () => {
  await Promise.all([...stallServers.values()].map((server) => server.stop()));
});

afterEach(() => {
  timeoutOverride.ms = undefined;
  createDefaultFetchSpy.mockClear();
  vi.unstubAllGlobals();
});

function clientFor(mode: StallMode, extra: Record<string, unknown> = {}): SmriteaClient {
  return new SmriteaClient({
    apiKey: 'test-key',
    appId: 'app_test',
    baseUrl: stallUrls.get(mode),
    maxRetries: 0,
    ...extra,
  });
}

describe('DEFAULT_TIMEOUT_MS', () => {
  it('is 10 seconds', () => {
    expect(DEFAULT_TIMEOUT_MS).toBe(10_000);
  });

  it('is the timeout the public constructor gives the default fetch', () => {
    new SmriteaClient({ apiKey: 'test-key', appId: 'app_test' });

    expect(createDefaultFetchSpy).toHaveBeenCalledTimes(1);
    expect(createDefaultFetchSpy.mock.calls[0][0]).toBe(10_000);
  });

  it('is not used when the caller supplies a fetch', () => {
    new SmriteaClient({ apiKey: 'test-key', appId: 'app_test', fetch: vi.fn() });

    expect(createDefaultFetchSpy).not.toHaveBeenCalled();
  });
});

describe('default fetch timeout through SmriteaClient', () => {
  type Operation = readonly [name: string, call: (client: SmriteaClient) => Promise<unknown>];
  const OPERATIONS: Operation[] = [
    ['add', (client) => client.add('likes tea')],
    ['search', (client) => client.search('tea')],
    ['get', (client) => client.get('mem_1')],
    ['delete', (client) => client.delete('mem_1')],
  ];
  const CASES = STALL_MODES.flatMap((mode) => OPERATIONS.map(([name, call]) => [name, mode, call] as const));

  it.each(CASES)('%s fails within the budget when the server %s', async (_name, mode, call) => {
    timeoutOverride.ms = SHORT_TIMEOUT_MS;
    const client = clientFor(mode);
    const started = Date.now();

    const error = await call(client).then(
      () => undefined,
      (err: unknown) => err,
    );

    const elapsed = Date.now() - started;
    expect(error).toBeInstanceOf(SmriteaError);
    expect(elapsed).toBeGreaterThanOrEqual(SHORT_TIMEOUT_MS - 50);
    expect(elapsed).toBeLessThan(MAX_ELAPSED_MS);
  });
});

describe('createDefaultFetch', () => {
  type SignalCase = readonly [label: string, withAnySupport: boolean];
  const SIGNAL_CASES: SignalCase[] = [
    ['with AbortSignal.any', true],
    ['without AbortSignal.any', false],
  ];

  async function withAnySupport<T>(supported: boolean, run: () => Promise<T>): Promise<T> {
    const original = Object.getOwnPropertyDescriptor(AbortSignal, 'any');
    if (!supported) {
      Object.defineProperty(AbortSignal, 'any', { value: undefined, configurable: true, writable: true });
    }
    try {
      return await run();
    } finally {
      if (original) {
        Object.defineProperty(AbortSignal, 'any', original);
      }
    }
  }

  it.each(STALL_MODES)('aborts when the server %s', async (mode) => {
    const timedFetch = createDefaultFetch(SHORT_TIMEOUT_MS);
    const started = Date.now();

    const error = await timedFetch(stallUrls.get(mode) as string)
      .then((res) => res.text())
      .then(
        () => undefined,
        (err: unknown) => err,
      );

    expect((error as Error).name).toBe('TimeoutError');
    expect(Date.now() - started).toBeLessThan(MAX_ELAPSED_MS);
  });

  it.each(SIGNAL_CASES)('adds a signal and keeps the other init fields (%s)', async (_label, supported) => {
    const baseFetch = vi.fn(async (..._args: Parameters<typeof fetch>) => new Response('ok'));

    await withAnySupport(supported, () =>
      createDefaultFetch(SHORT_TIMEOUT_MS, baseFetch)('http://127.0.0.1:9/x', {
        method: 'POST',
        headers: { 'X-Test': '1' },
      }),
    );

    const [url, init] = baseFetch.mock.calls[0];
    expect(url).toBe('http://127.0.0.1:9/x');
    expect(init?.method).toBe('POST');
    expect(init?.headers).toEqual({ 'X-Test': '1' });
    expect(init?.signal).toBeInstanceOf(AbortSignal);
    expect(init?.signal?.aborted).toBe(false);
  });

  it.each(SIGNAL_CASES)('honours a caller signal that aborts first (%s)', async (_label, supported) => {
    const controller = new AbortController();
    const reason = new Error('caller gave up');
    setTimeout(() => controller.abort(reason), 100);
    const started = Date.now();

    const error = await withAnySupport(supported, () =>
      createDefaultFetch(DEFAULT_TIMEOUT_MS)(stallUrls.get('never answers') as string, {
        signal: controller.signal,
      }).then(
        () => undefined,
        (err: unknown) => err,
      ),
    );

    expect(error).toBe(reason);
    expect(Date.now() - started).toBeLessThan(MAX_ELAPSED_MS);
  });

  it.each(SIGNAL_CASES)('honours a caller signal that is already aborted (%s)', async (_label, supported) => {
    const baseFetch = vi.fn(async (_input: Parameters<typeof fetch>[0], init?: RequestInit) => {
      init?.signal?.throwIfAborted();
      return new Response('ok');
    });
    const reason = new Error('already aborted');

    const error = await withAnySupport(supported, () =>
      createDefaultFetch(DEFAULT_TIMEOUT_MS, baseFetch)('http://127.0.0.1:9/x', {
        signal: AbortSignal.abort(reason),
      }).then(
        () => undefined,
        (err: unknown) => err,
      ),
    );

    expect(error).toBe(reason);
  });

  it.each(SIGNAL_CASES)('times out when the caller signal never aborts (%s)', async (_label, supported) => {
    const controller = new AbortController();

    const error = await withAnySupport(supported, () =>
      createDefaultFetch(SHORT_TIMEOUT_MS)(stallUrls.get('never answers') as string, {
        signal: controller.signal,
      }).then(
        () => undefined,
        (err: unknown) => err,
      ),
    );

    expect((error as Error).name).toBe('TimeoutError');
    expect(controller.signal.aborted).toBe(false);
  });

  it('resolves the global fetch at call time', async () => {
    const timedFetch = createDefaultFetch(SHORT_TIMEOUT_MS);
    const globalFetch = vi.fn(async () => new Response('ok'));
    vi.stubGlobal('fetch', globalFetch);

    await timedFetch('http://127.0.0.1:9/x');

    expect(globalFetch).toHaveBeenCalledTimes(1);
  });
});

describe('caller-supplied fetch', () => {
  it('receives the request without a signal added by the SDK', async () => {
    const customFetch = vi.fn(
      async (..._args: Parameters<typeof fetch>) =>
        new Response(JSON.stringify({ code: 'MEMORY_NOT_FOUND', message: 'Memory not found' }), {
          status: 404,
          headers: { 'Content-Type': 'application/json' },
        }),
    );
    const client = new SmriteaClient({
      apiKey: 'test-key',
      appId: 'app_test',
      baseUrl: 'http://127.0.0.1:9',
      maxRetries: 0,
      fetch: customFetch,
    });

    await expect(client.get('mem_1')).rejects.toBeInstanceOf(SmriteaNotFoundError);

    const init = customFetch.mock.calls[0][1] as RequestInit;
    expect(init.signal).toBeUndefined();
    expect('signal' in init).toBe(false);
  });
});

describe('429 retry', () => {
  it('gives the retried exchange a fresh budget and does not count the retry sleep', async () => {
    // Exchange 1 answers 429 at once with Retry-After: 1 (SDK sleeps 0.9 s, longer than the
    // 400 ms budget). Exchange 2 takes 250 ms. Total time exceeds the budget; each exchange does not.
    const budgetMs = 400;
    let requests = 0;
    const server = createHttpServer((_req, res) => {
      requests += 1;
      if (requests === 1) {
        res.writeHead(429, { 'Retry-After': '1', 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ code: 'RATE_LIMITED', message: 'slow down' }));
        return;
      }
      setTimeout(() => {
        res.writeHead(404, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ code: 'MEMORY_NOT_FOUND', message: 'Memory not found' }));
      }, 250);
    });
    await new Promise<void>((resolve) => server.listen(0, '127.0.0.1', resolve));
    timeoutOverride.ms = budgetMs;
    const client = new SmriteaClient({
      apiKey: 'test-key',
      appId: 'app_test',
      baseUrl: `http://127.0.0.1:${(server.address() as AddressInfo).port}`,
      maxRetries: 1,
    });
    const started = Date.now();

    try {
      await expect(client.get('mem_1')).rejects.toBeInstanceOf(SmriteaNotFoundError);
    } finally {
      await new Promise<void>((resolve) => {
        server.closeAllConnections();
        server.close(() => resolve());
      });
    }

    expect(requests).toBe(2);
    expect(Date.now() - started).toBeGreaterThan(budgetMs);
  });
});
