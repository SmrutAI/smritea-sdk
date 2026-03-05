package ai.smritea.sdk.errors;

/** Thrown when the API returns a 404 Not Found response. */
public class SmriteaNotFoundError extends SmriteaError {
  /**
   * Creates a new SmriteaNotFoundError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 404)
   */
  public SmriteaNotFoundError(String message, int statusCode) {
    super(message, statusCode);
  }
}
