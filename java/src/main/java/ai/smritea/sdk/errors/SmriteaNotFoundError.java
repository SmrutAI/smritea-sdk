package ai.smritea.sdk.errors;

/** Thrown when the API returns a 404 Not Found response. */
public class SmriteaNotFoundError extends SmriteaError {
  /**
   * Creates a new SmriteaNotFoundError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 404)
   * @param errorCode the error code from the API response, or null if not provided
   */
  public SmriteaNotFoundError(String message, int statusCode, String errorCode) {
    super(message, statusCode, errorCode);
  }

  /**
   * Creates a new SmriteaNotFoundError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 404)
   */
  public SmriteaNotFoundError(String message, int statusCode) {
    this(message, statusCode, null);
  }
}
