package ai.smritea.sdk.errors;

/** Thrown when the API returns a 429 Too Many Requests response. */
public class SmriteaTooManyRequestsError extends SmriteaError {
  private final Integer retryAfter;

  /**
   * Creates a new SmriteaTooManyRequestsError.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 429)
   * @param retryAfter seconds to wait before retrying, or null if not provided
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaTooManyRequestsError(
      String message,
      int httpStatus,
      Integer retryAfter,
      String code,
      boolean retryable,
      Object body) {
    super(message, httpStatus, code, retryable, body);
    this.retryAfter = retryAfter;
  }

  /**
   * Creates a new SmriteaTooManyRequestsError.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 429)
   * @param retryAfter seconds to wait before retrying, or null if not provided
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   */
  public SmriteaTooManyRequestsError(
      String message, int httpStatus, Integer retryAfter, String code, boolean retryable) {
    this(message, httpStatus, retryAfter, code, retryable, null);
  }

  /**
   * Creates a new SmriteaTooManyRequestsError. Not retryable.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 429)
   * @param retryAfter seconds to wait before retrying, or null if not provided
   */
  public SmriteaTooManyRequestsError(String message, int httpStatus, Integer retryAfter) {
    this(message, httpStatus, retryAfter, null, false, null);
  }

  /**
   * Returns the number of seconds to wait before retrying, or null if not provided by the server.
   *
   * @return retry-after seconds, or null
   */
  public Integer getRetryAfter() {
    return retryAfter;
  }
}
