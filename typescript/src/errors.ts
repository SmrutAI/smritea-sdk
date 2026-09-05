/**
 * Error classes for the smritea TypeScript SDK.
 * Each maps to an HTTP status code returned by the API.
 */

export class SmriteaError extends Error {
  httpStatus?: number;
  code: string;
  body?: unknown;
  retryable: boolean;

  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message);
    this.name = 'SmriteaError';
    this.httpStatus = httpStatus;
    this.code = code ?? 'INTERNAL_ERROR';
    this.body = body;
    this.retryable = retryable;
    // Maintain proper prototype chain in TypeScript/transpiled environments
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 401 -- invalid or missing API key. */
export class SmriteaUnauthorizedError extends SmriteaError {
  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message, httpStatus ?? 401, code, body, retryable);
    this.name = 'SmriteaUnauthorizedError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 404 -- memory not found. */
export class SmriteaNotFoundError extends SmriteaError {
  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message, httpStatus ?? 404, code, body, retryable);
    this.name = 'SmriteaNotFoundError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 400 -- request validation failed. */
export class SmriteaBadRequestError extends SmriteaError {
  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message, httpStatus ?? 400, code, body, retryable);
    this.name = 'SmriteaBadRequestError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 402 -- quota exceeded for this organization. */
export class SmriteaPaymentRequiredError extends SmriteaError {
  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message, httpStatus ?? 402, code, body, retryable);
    this.name = 'SmriteaPaymentRequiredError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/**
 * Raised when the server returns a response that cannot be deserialized.
 * This typically indicates an unexpected API response format or a server-side
 * error that produced a malformed body.
 */
export class SmriteaDeserializationError extends SmriteaError {
  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message, httpStatus, code, body, retryable);
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
export class SmriteaTooManyRequestsError extends SmriteaError {
  retryAfter?: number;

  constructor(message: string, httpStatus?: number, retryAfter?: number, code?: string, body?: unknown, retryable = true) {
    super(message, httpStatus ?? 429, code, body, retryable);
    this.name = 'SmriteaTooManyRequestsError';
    this.retryAfter = retryAfter;
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 403 -- access denied. */
export class SmriteaForbiddenError extends SmriteaError {
  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message, httpStatus ?? 403, code, body, retryable);
    this.name = 'SmriteaForbiddenError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 409 -- resource conflict. */
export class SmriteaConflictError extends SmriteaError {
  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message, httpStatus ?? 409, code, body, retryable);
    this.name = 'SmriteaConflictError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 422 -- unprocessable entity. */
export class SmriteaUnprocessableError extends SmriteaError {
  constructor(message: string, httpStatus?: number, code?: string, body?: unknown, retryable = false) {
    super(message, httpStatus ?? 422, code, body, retryable);
    this.name = 'SmriteaUnprocessableError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/**
 * Throw the appropriate SmriteaError subclass for a given HTTP status code.
 * @param status - HTTP response status code
 * @param message - Error message from the response body
 * @param retryAfter - Value of the Retry-After header (for 429 responses)
 * @param code - Error code from the response body
 * @param body - Full parsed HTTP response body
 * @param retryable - Whether the error should be retried (used except for 429, which is always retryable)
 */
export function throwForStatus(status: number, message: string, retryAfter?: number, code?: string, body?: unknown, retryable?: boolean): never {
  switch (status) {
    case 400:
      throw new SmriteaBadRequestError(message, status, code, body, retryable);
    case 401:
      throw new SmriteaUnauthorizedError(message, status, code, body, retryable);
    case 402:
      throw new SmriteaPaymentRequiredError(message, status, code, body, retryable);
    case 403:
      throw new SmriteaForbiddenError(message, status, code, body, retryable);
    case 404:
      throw new SmriteaNotFoundError(message, status, code, body, retryable);
    case 409:
      throw new SmriteaConflictError(message, status, code, body, retryable);
    case 422:
      throw new SmriteaUnprocessableError(message, status, code, body, retryable);
    case 429:
      throw new SmriteaTooManyRequestsError(message, status, retryAfter, code, body, true);
    default:
      throw new SmriteaError(message, status, code, body, retryable);
  }
}
