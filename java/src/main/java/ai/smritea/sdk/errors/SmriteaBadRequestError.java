package ai.smritea.sdk.errors;

/** Thrown when the API returns a 400 Bad Request response due to validation failure. */
public class SmriteaBadRequestError extends SmriteaError {
  /**
   * Creates a new SmriteaBadRequestError.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 400)
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaBadRequestError(
      String message, int httpStatus, String code, boolean retryable, Object body) {
    super(message, httpStatus, code, retryable, body);
  }

  /**
   * Creates a new SmriteaBadRequestError.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 400)
   * @param code the wire code from the API response, or null if not provided
   * @param retryable whether the server marked this error as retryable
   */
  public SmriteaBadRequestError(String message, int httpStatus, String code, boolean retryable) {
    this(message, httpStatus, code, retryable, null);
  }

  /**
   * Creates a new SmriteaBadRequestError. Not retryable.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code (typically 400)
   */
  public SmriteaBadRequestError(String message, int httpStatus) {
    this(message, httpStatus, null, false, null);
  }
}
