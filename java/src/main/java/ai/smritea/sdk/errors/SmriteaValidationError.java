package ai.smritea.sdk.errors;

/** Thrown when the API returns a 400 Bad Request response due to validation failure. */
public class SmriteaValidationError extends SmriteaError {
  /**
   * Creates a new SmriteaValidationError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 400)
   */
  public SmriteaValidationError(String message, int statusCode) {
    super(message, statusCode);
  }
}
