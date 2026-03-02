/**
 * Error classes for the smritea TypeScript SDK.
 * Each maps to an HTTP status code returned by the API.
 */

export class SmriteaError extends Error {
  statusCode?: number;

  constructor(message: string, statusCode?: number) {
    super(message);
    this.name = 'SmriteaError';
    this.statusCode = statusCode;
    // Maintain proper prototype chain in TypeScript/transpiled environments
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 401 -- invalid or missing API key. */
export class SmriteaAuthError extends SmriteaError {
  constructor(message: string, statusCode?: number) {
    super(message, statusCode ?? 401);
    this.name = 'SmriteaAuthError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 404 -- memory not found. */
export class SmriteaNotFoundError extends SmriteaError {
  constructor(message: string, statusCode?: number) {
    super(message, statusCode ?? 404);
    this.name = 'SmriteaNotFoundError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 400 -- request validation failed. */
export class SmriteaValidationError extends SmriteaError {
  constructor(message: string, statusCode?: number) {
    super(message, statusCode ?? 400);
    this.name = 'SmriteaValidationError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 402 -- quota exceeded for this organization. */
export class SmriteaQuotaError extends SmriteaError {
  constructor(message: string, statusCode?: number) {
    super(message, statusCode ?? 402);
    this.name = 'SmriteaQuotaError';
    Object.setPrototypeOf(this, new.target.prototype);
  }
}

/** HTTP 429 -- rate limit exceeded. Check retryAfter (seconds) before retrying. */
export class SmriteaRateLimitError extends SmriteaError {
  retryAfter?: number;

  constructor(message: string, statusCode?: number, retryAfter?: number) {
    super(message, statusCode ?? 429);
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
 */
export function throwForStatus(status: number, message: string, retryAfter?: number): never {
  switch (status) {
    case 400:
      throw new SmriteaValidationError(message, status);
    case 401:
      throw new SmriteaAuthError(message, status);
    case 402:
      throw new SmriteaQuotaError(message, status);
    case 404:
      throw new SmriteaNotFoundError(message, status);
    case 429:
      throw new SmriteaRateLimitError(message, status, retryAfter);
    default:
      throw new SmriteaError(message, status);
  }
}
