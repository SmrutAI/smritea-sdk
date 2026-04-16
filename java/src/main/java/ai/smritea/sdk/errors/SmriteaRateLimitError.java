package ai.smritea.sdk.errors;

/** Thrown when the API returns a 429 Too Many Requests response. */
public class SmriteaRateLimitError extends SmriteaError {
  private final Integer retryAfter;

  /**
   * Creates a new SmriteaRateLimitError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 429)
   * @param retryAfter seconds to wait before retrying, or null if not provided
   * @param errorCode the error code from the API response, or null if not provided
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaRateLimitError(
      String message, int statusCode, Integer retryAfter, String errorCode, Object body) {
    super(message, statusCode, errorCode, body);
    this.retryAfter = retryAfter;
  }

  /**
   * Creates a new SmriteaRateLimitError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 429)
   * @param retryAfter seconds to wait before retrying, or null if not provided
   * @param errorCode the error code from the API response, or null if not provided
   */
  public SmriteaRateLimitError(
      String message, int statusCode, Integer retryAfter, String errorCode) {
    this(message, statusCode, retryAfter, errorCode, null);
  }

  /**
   * Creates a new SmriteaRateLimitError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 429)
   * @param retryAfter seconds to wait before retrying, or null if not provided
   */
  public SmriteaRateLimitError(String message, int statusCode, Integer retryAfter) {
    this(message, statusCode, retryAfter, null, null);
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
