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
   */
  public SmriteaRateLimitError(String message, int statusCode, Integer retryAfter) {
    super(message, statusCode);
    this.retryAfter = retryAfter;
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
