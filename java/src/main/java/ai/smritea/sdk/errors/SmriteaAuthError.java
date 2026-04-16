package ai.smritea.sdk.errors;

/** Thrown when the API returns a 401 Unauthorized response. */
public class SmriteaAuthError extends SmriteaError {
  /**
   * Creates a new SmriteaAuthError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 401)
   * @param errorCode the error code from the API response, or null if not provided
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaAuthError(String message, int statusCode, String errorCode, Object body) {
    super(message, statusCode, errorCode, body);
  }

  /**
   * Creates a new SmriteaAuthError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 401)
   * @param errorCode the error code from the API response, or null if not provided
   */
  public SmriteaAuthError(String message, int statusCode, String errorCode) {
    this(message, statusCode, errorCode, null);
  }

  /**
   * Creates a new SmriteaAuthError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 401)
   */
  public SmriteaAuthError(String message, int statusCode) {
    this(message, statusCode, null, null);
  }
}
