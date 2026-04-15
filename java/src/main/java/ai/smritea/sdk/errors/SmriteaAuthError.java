package ai.smritea.sdk.errors;

/** Thrown when the API returns a 401 Unauthorized response. */
public class SmriteaAuthError extends SmriteaError {
  /**
   * Creates a new SmriteaAuthError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 401)
   * @param errorCode the error code from the API response, or null if not provided
   */
  public SmriteaAuthError(String message, int statusCode, String errorCode) {
    super(message, statusCode, errorCode);
  }

  /**
   * Creates a new SmriteaAuthError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 401)
   */
  public SmriteaAuthError(String message, int statusCode) {
    this(message, statusCode, null);
  }
}
