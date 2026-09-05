package ai.smritea.sdk.errors;

/** Thrown when the API returns a 403 Forbidden response. */
public class SmriteaForbiddenError extends SmriteaError {
  /**
   * Creates a new SmriteaForbiddenError.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 403)
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaForbiddenError(
      String message, int httpStatus, String code, boolean retryable, Object body) {
    super(message, httpStatus, code, retryable, body);
  }

  /**
   * Creates a new SmriteaForbiddenError.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 403)
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   */
  public SmriteaForbiddenError(String message, int httpStatus, String code, boolean retryable) {
    this(message, httpStatus, code, retryable, null);
  }

  /**
   * Creates a new SmriteaForbiddenError. Not retryable.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 403)
   */
  public SmriteaForbiddenError(String message, int httpStatus) {
    this(message, httpStatus, null, false, null);
  }
}
