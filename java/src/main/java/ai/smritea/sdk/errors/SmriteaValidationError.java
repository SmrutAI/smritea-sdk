package ai.smritea.sdk.errors;

/** Thrown when the API returns a 400 Bad Request response due to validation failure. */
public class SmriteaValidationError extends SmriteaError {
  /**
   * Creates a new SmriteaValidationError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 400)
   * @param errorCode the error code from the API response, or null if not provided
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaValidationError(String message, int statusCode, String errorCode, Object body) {
    super(message, statusCode, errorCode, body);
  }

  /**
   * Creates a new SmriteaValidationError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 400)
   * @param errorCode the error code from the API response, or null if not provided
   */
  public SmriteaValidationError(String message, int statusCode, String errorCode) {
    this(message, statusCode, errorCode, null);
  }

  /**
   * Creates a new SmriteaValidationError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 400)
   */
  public SmriteaValidationError(String message, int statusCode) {
    this(message, statusCode, null, null);
  }
}
