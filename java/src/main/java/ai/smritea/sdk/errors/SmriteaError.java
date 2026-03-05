package ai.smritea.sdk.errors;

/** Base error type for all Smritea SDK errors. */
public class SmriteaError extends RuntimeException {
  private final Integer statusCode;

  /**
   * Creates a new SmriteaError with a message and HTTP status code.
   *
   * @param message the error message
   * @param statusCode the HTTP status code, or null if not applicable
   */
  public SmriteaError(String message, Integer statusCode) {
    super(message);
    this.statusCode = statusCode;
  }

  /**
   * Creates a new SmriteaError with a message and no status code.
   *
   * @param message the error message
   */
  public SmriteaError(String message) {
    this(message, null);
  }

  /**
   * Returns the HTTP status code associated with this error, or null if not applicable.
   *
   * @return the HTTP status code, or null
   */
  public Integer getStatusCode() {
    return statusCode;
  }
}
