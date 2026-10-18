import { afterEach, describe, expect, it, vi } from 'vitest';
import { SmriteaClient } from '../src/client.js';
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
} from '../src/errors.js';

const BASE_CONFIG = { apiKey: 'test-key', appId: 'app_test', baseUrl: 'https://api.test.local' };

function stubFetch(makeResponse: () => Response): ReturnType<typeof vi.fn> {
  const fetchMock = vi.fn(async () => makeResponse());
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function sentHeaders(fetchMock: ReturnType<typeof vi.fn>, callIndex = 0): Record<string, string> {
  const init = fetchMock.mock.calls[callIndex][1] as RequestInit;
  return init.headers as Record<string, string>;
}

afterEach(() => {
  vi.unstubAllGlobals();
});

type ErrorClass = new (...args: never[]) => SmriteaError;
type RequestIdRow = readonly [
  label: string,
  status: number,
  errorClass: ErrorClass,
  body: Record<string, string>,
  headers: Record<string, string>,
  expected: string | undefined,
];

const REQUEST_ID_STATUS_CLASSES: ReadonlyArray<readonly [number, ErrorClass]> = [
  [400, SmriteaBadRequestError],
  [401, SmriteaUnauthorizedError],
  [402, SmriteaPaymentRequiredError],
  [403, SmriteaForbiddenError],
  [409, SmriteaConflictError],
  [422, SmriteaUnprocessableError],
  [500, SmriteaError],
];

const REQUEST_ID_ROWS: RequestIdRow[] = [
  ...REQUEST_ID_STATUS_CLASSES.flatMap(([status, errorClass]): RequestIdRow[] => [
    ['body request_id', status, errorClass, { message: 'request failed', request_id: 'req_body' }, {}, 'req_body'],
    ['header only', status, errorClass, { message: 'request failed' }, { 'X-Request-ID': 'req_header' }, 'req_header'],
    ['neither', status, errorClass, { message: 'request failed' }, {}, undefined],
  ]),
  ['header only', 429, SmriteaTooManyRequestsError, { message: 'rate limited' }, { 'X-Request-ID': 'req_header' }, 'req_header'],
  ['neither', 429, SmriteaTooManyRequestsError, { message: 'rate limited' }, {}, undefined],
];

describe('traceId option', () => {
  it('sends X-Trace-ID on every call when set', async () => {
    const fetchMock = stubFetch(() => new Response(JSON.stringify({ id: 'mem_1' }), { status: 200 }));
    const client = new SmriteaClient({ ...BASE_CONFIG, traceId: 'trace-abc12345' });

    await client.get('mem_1');
    await client.delete('mem_1');

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(sentHeaders(fetchMock, 0)['X-Trace-ID']).toBe('trace-abc12345');
    expect(sentHeaders(fetchMock, 1)['X-Trace-ID']).toBe('trace-abc12345');
  });

  it('does not send X-Trace-ID when unset', async () => {
    const fetchMock = stubFetch(() => new Response(JSON.stringify({ id: 'mem_1' }), { status: 200 }));
    const client = new SmriteaClient(BASE_CONFIG);

    await client.get('mem_1');

    expect(sentHeaders(fetchMock)['X-Trace-ID']).toBeUndefined();
  });

  it.each([
    ['7 characters', 'abcdefg'],
    ['101 characters', 'a'.repeat(101)],
    ['a space', 'trace id 1234'],
    ['a slash', 'trace/id12345'],
    ['non-ASCII', 'tracé12345'],
    ['empty string', ''],
    ['a trailing newline', 'abcdefgh\n'],
    ['a trailing NUL', 'abcdefgh\u0000'],
  ])('throws TypeError in the constructor for an invalid traceId (%s) and makes no request', (_label, traceId) => {
    const fetchMock = stubFetch(() => new Response('{}', { status: 200 }));

    expect(() => new SmriteaClient({ ...BASE_CONFIG, traceId })).toThrow(TypeError);
    expect(() => new SmriteaClient({ ...BASE_CONFIG, traceId })).toThrow(
      'trace_id must be 8-100 characters of A-Z, a-z, 0-9, dot, underscore, colon or hyphen',
    );
    expect(fetchMock).not.toHaveBeenCalled();
  });

  it.each([
    ['8 characters', 'abcdefgh'],
    ['100 characters', 'a'.repeat(100)],
    ['a.b_c:d-e', 'a.b_c:d-e'],
    ['all allowed symbols', 'A.z_9:-0Q'],
  ])('accepts a traceId of %s and sends it as X-Trace-ID on every fetch call', async (_label, traceId) => {
    const fetchMock = stubFetch(() => new Response(JSON.stringify({ id: 'mem_1' }), { status: 200 }));
    const client = new SmriteaClient({ ...BASE_CONFIG, traceId });

    await client.get('mem_1');
    await client.delete('mem_1');

    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(sentHeaders(fetchMock, 0)['X-Trace-ID']).toBe(traceId);
    expect(sentHeaders(fetchMock, 1)['X-Trace-ID']).toBe(traceId);
  });
});

describe('requestId on errors', () => {
  it('reads request_id from the response body', async () => {
    stubFetch(
      () =>
        new Response(JSON.stringify({ message: 'Memory not found', code: 'MEMORY_NOT_FOUND', request_id: 'req_body_1' }), {
          status: 404,
        }),
    );
    const client = new SmriteaClient(BASE_CONFIG);

    const error = await client.get('mem_missing').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(SmriteaNotFoundError);
    expect((error as SmriteaNotFoundError).requestId).toBe('req_body_1');
  });

  it('falls back to the X-Request-ID header when the body has no request_id', async () => {
    stubFetch(
      () =>
        new Response(JSON.stringify({ message: 'Memory not found', code: 'MEMORY_NOT_FOUND' }), {
          status: 404,
          headers: { 'X-Request-Id': 'req_header_1' },
        }),
    );
    const client = new SmriteaClient(BASE_CONFIG);

    const error = await client.get('mem_missing').catch((e: unknown) => e);

    expect((error as SmriteaNotFoundError).requestId).toBe('req_header_1');
  });

  it('reads the X-Request-ID header when the body is not JSON', async () => {
    stubFetch(() => new Response('upstream exploded', { status: 404, headers: { 'x-request-id': 'req_raw_1' } }));
    const client = new SmriteaClient(BASE_CONFIG);

    const error = await client.get('mem_missing').catch((e: unknown) => e);

    expect((error as SmriteaNotFoundError).requestId).toBe('req_raw_1');
  });

  it('is undefined when neither the body nor the header carries a request ID', async () => {
    stubFetch(() => new Response(JSON.stringify({ message: 'Memory not found' }), { status: 404 }));
    const client = new SmriteaClient(BASE_CONFIG);

    const error = await client.get('mem_missing').catch((e: unknown) => e);

    expect((error as SmriteaNotFoundError).requestId).toBeUndefined();
  });

  it('exposes requestId on SmriteaTooManyRequestsError', async () => {
    stubFetch(
      () =>
        new Response(JSON.stringify({ message: 'Rate limited', request_id: 'req_429' }), {
          status: 429,
          headers: { 'Retry-After': '1' },
        }),
    );
    const client = new SmriteaClient({ ...BASE_CONFIG, maxRetries: 0 });

    const error = await client.get('mem_1').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(SmriteaTooManyRequestsError);
    expect((error as SmriteaTooManyRequestsError).requestId).toBe('req_429');
  });

  // Rows for status/case pairs not already covered above (404 has all three cases and
  // 429 has the body case in the tests above). Source: typescript/src/client.ts:184-194, 201-211.
  it.each(REQUEST_ID_ROWS)(
    '%s on HTTP %i maps to %s with the expected requestId',
    async (_label, status, errorClass, body, headers, expected) => {
      stubFetch(() => new Response(JSON.stringify(body), { status, headers }));
      const client = new SmriteaClient({ ...BASE_CONFIG, maxRetries: 0 });

      const error = await client.get('mem_1').catch((e: unknown) => e);

      expect(error).toBeInstanceOf(errorClass);
      expect((error as SmriteaError).requestId).toBe(expected);
    },
  );
});
