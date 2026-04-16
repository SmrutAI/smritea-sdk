/**
 * Error classes for the smritea TypeScript SDK.
 * Each maps to an HTTP status code returned by the API.
 */

export class SmriteaError extends Error {
  statusCode?: number;
  errorCode: string;
  body?: unknown;

  constructor(message: string, statusCode?: number, errorCode?: string, body?: unknown) {
    super(message);
    this.name = 'SmriteaError';
    this.statusCode = statusCode;
    this.errorCode = errorCode ?? 'INTERNAL_ERROR';
    this.body = body;
    // Maintain proper prototype chain in TypeScript/transpiled environments
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 401 -- invalid or missing API key. */
export class SmriteaAuthError extends SmriteaError {
  constructor(message: string, statusCode?: number, errorCode?: string, body?: unknown) {
    super(message, statusCode ?? 401, errorCode, body);
    this.name = 'SmriteaAuthError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 404 -- memory not found. */
export class SmriteaNotFoundError extends SmriteaError {
  constructor(message: string, statusCode?: number, errorCode?: string, body?: unknown) {
    super(message, statusCode ?? 404, errorCode, body);
    this.name = 'SmriteaNotFoundError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 400 -- request validation failed. */
export class SmriteaValidationError extends SmriteaError {
  constructor(message: string, statusCode?: number, errorCode?: string, body?: unknown) {
    super(message, statusCode ?? 400, errorCode, body);
    this.name = 'SmriteaValidationError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 402 -- quota exceeded for this organization. */
export class SmriteaQuotaError extends SmriteaError {
  constructor(message: string, statusCode?: number, errorCode?: string, body?: unknown) {
    super(message, statusCode ?? 402, errorCode, body);
    this.name = 'SmriteaQuotaError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/**
 * Raised when the server returns a response that cannot be deserialized.
 * This typically indicates an unexpected API response format or a server-side
 * error that produced a malformed body.
 */
export class SmriteaDeserializationError extends SmriteaError {
  constructor(message: string, statusCode?: number, errorCode?: string, body?: unknown) {
    super(message, statusCode, errorCode, body);
    this.name = 'SmriteaDeserializationError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/**
 * HTTP 429 -- rate limit exceeded after all retries are exhausted.
 *
 * `retryAfter` is the value from the server's Retry-After header (seconds),
 * if provided. This is informational — the SDK already waited this long
 * during its automatic retry attempts.
 */
export class SmriteaRateLimitError extends SmriteaError {
  retryAfter?: number;

  constructor(message: string, statusCode?: number, retryAfter?: number, errorCode?: string, body?: unknown) {
    super(message, statusCode ?? 429, errorCode, body);
    this.name = 'SmriteaRateLimitError';
    this.retryAfter = retryAfter;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/**
 * Throw the appropriate SmriteaError subclass for a given HTTP status code.
 * @param status - HTTP response status code
 * @param message - Error message from the response body
 * @param retryAfter - Value of the Retry-After header (for 429 responses)
 * @param errorCode - Error code from the response body
 * @param body - Full parsed HTTP response body
 */
export function throwForStatus(status: number, message: string, retryAfter?: number, errorCode?: string, body?: unknown): never {
  switch (status) {
    case 400:
      throw new SmriteaValidationError(message, status, errorCode, body);
    case 401:
      throw new SmriteaAuthError(message, status, errorCode, body);
    case 402:
      throw new SmriteaQuotaError(message, status, errorCode, body);
    case 404:
      throw new SmriteaNotFoundError(message, status, errorCode, body);
    case 429:
      throw new SmriteaRateLimitError(message, status, retryAfter, errorCode, body);
    default:
      throw new SmriteaError(message, status, errorCode, body);
  }
}
