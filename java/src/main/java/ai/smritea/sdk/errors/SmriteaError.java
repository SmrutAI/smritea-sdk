package ai.smritea.sdk.errors;

/** Base error type for all Smritea SDK errors. */
public class SmriteaError extends RuntimeException {
  private final Integer statusCode;
  private final String errorCode;

  /**
   * Creates a new SmriteaError with a message, HTTP status code, and error code.
   *
   * @param message the error message
   * @param statusCode the HTTP status code, or null if not applicable
   * @param errorCode the error code from the API response, or null if not provided (defaults to
   *     "INTERNAL_ERROR")
   */
  public SmriteaError(String message, Integer statusCode, String errorCode) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode != null ? errorCode : "INTERNAL_ERROR";
  }

  /**
   * Creates a new SmriteaError with a message and HTTP status code.
   *
   * @param message the error message
   * @param statusCode the HTTP status code, or null if not applicable
   */
  public SmriteaError(String message, Integer statusCode) {
    this(message, statusCode, null);
  }

  /**
   * Creates a new SmriteaError with a message and no status code.
   *
   * @param message the error message
   */
  public SmriteaError(String message) {
    this(message, null, null);
  }

  /**
   * Returns the HTTP status code associated with this error, or null if not applicable.
   *
   * @return the HTTP status code, or null
   */
  public Integer getStatusCode() {
    return statusCode;
  }

  /**
   * Returns the error code from the API response, or null if not provided.
   *
   * @return the error code, or null
   */
  public String getErrorCode() {
    return errorCode;
  }
}
