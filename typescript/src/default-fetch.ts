/**
 * Total time budget, in milliseconds, for ONE HTTP exchange made by the default
 * `fetch` the SDK uses when the caller supplies none. The budget runs from sending
 * the request until the full response body has been read. A retry after HTTP 429
 * starts a new exchange with a fresh budget; the retry sleep is not counted.
 */
export const DEFAULT_TIMEOUT_MS = 10_000;

/**
 * Combine the timeout signal with the signal the caller or the generated runtime
 * already passed, so that whichever aborts first aborts the request.
 */
function combineSignals(timeoutSignal: AbortSignal, callerSignal: AbortSignal | null | undefined): AbortSignal {
  if (!callerSignal) {
    return timeoutSignal;
  }
  if (typeof AbortSignal.any === 'function') {
    return AbortSignal.any([timeoutSignal, callerSignal]);
  }
  // Fallback for runtimes without AbortSignal.any (Node 18 up to 20.2).
  const controller = new AbortController();
  if (callerSignal.aborted) {
    controller.abort(callerSignal.reason);
    return controller.signal;
  }
  const onCallerAbort = (): void => controller.abort(callerSignal.reason);
  callerSignal.addEventListener('abort', onCallerAbort, { once: true });
  // The timeout signal always fires, so the listener on a long-lived caller signal is removed then.
  timeoutSignal.addEventListener(
    'abort',
    () => {
      callerSignal.removeEventListener('abort', onCallerAbort);
      controller.abort(timeoutSignal.reason);
    },
    { once: true },
  );
  return controller.signal;
}

/**
 * Create a `fetch` that aborts every call after `timeoutMs`, counted from the call until the
 * response body has been fully read. A signal already present in `init` is honoured: the call
 * aborts when either signal aborts.
 *
 * `baseFetch` defaults to the global `fetch`, looked up at call time. The timeout timer does
 * not keep a Node process alive.
 */
export function createDefaultFetch(timeoutMs: number, baseFetch?: typeof fetch): typeof fetch {
  return (input, init) => {
    const signal = combineSignals(AbortSignal.timeout(timeoutMs), init?.signal);
    return (baseFetch ?? globalThis.fetch)(input, { ...init, signal });
  };
}
