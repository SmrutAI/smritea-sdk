package ai.smritea.sdk.errors;

/** Thrown when the API returns a 404 Not Found response. */
public class SmriteaNotFoundError extends SmriteaError {
  /**
   * Creates a new SmriteaNotFoundError.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 404)
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaNotFoundError(
      String message, int httpStatus, String code, boolean retryable, Object body) {
    super(message, httpStatus, code, retryable, body);
  }

  /**
   * Creates a new SmriteaNotFoundError carrying the server request ID.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 404)
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   * @param body the full parsed JSON response body, or null if not available
   * @param requestId the server request ID, or null if not available
   */
  public SmriteaNotFoundError(
      String message,
      int httpStatus,
      String code,
      boolean retryable,
      Object body,
      String requestId) {
    super(message, httpStatus, code, retryable, body, requestId);
  }

  /**
   * Creates a new SmriteaNotFoundError.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 404)
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   */
  public SmriteaNotFoundError(String message, int httpStatus, String code, boolean retryable) {
    this(message, httpStatus, code, retryable, null);
  }

  /**
   * Creates a new SmriteaNotFoundError. Not retryable.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 404)
   */
  public SmriteaNotFoundError(String message, int httpStatus) {
    this(message, httpStatus, null, false, null);
  }
}
