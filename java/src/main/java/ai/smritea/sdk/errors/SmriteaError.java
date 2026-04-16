package ai.smritea.sdk.errors;

/** Base error type for all Smritea SDK errors. */
public class SmriteaError extends RuntimeException {
  private final Integer statusCode;
  private final String errorCode;
  private final Object body;

  /**
   * Creates a new SmriteaError with a message, HTTP status code, error code, and body.
   *
   * @param message the error message
   * @param statusCode the HTTP status code, or null if not applicable
   * @param errorCode the error code from the API response, or null if not provided (defaults to
   *     "INTERNAL_ERROR")
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaError(String message, Integer statusCode, String errorCode, Object body) {
    super(message);
    this.statusCode = statusCode;
    this.errorCode = errorCode != null ? errorCode : "INTERNAL_ERROR";
    this.body = body;
  }

  /**
   * Creates a new SmriteaError with a message, HTTP status code, and error code.
   *
   * @param message the error message
   * @param statusCode the HTTP status code, or null if not applicable
   * @param errorCode the error code from the API response, or null if not provided (defaults to
   *     "INTERNAL_ERROR")
   */
  public SmriteaError(String message, Integer statusCode, String errorCode) {
    this(message, statusCode, errorCode, null);
  }

  /**
   * Creates a new SmriteaError with a message and HTTP status code.
   *
   * @param message the error message
   * @param statusCode the HTTP status code, or null if not applicable
   */
  public SmriteaError(String message, Integer statusCode) {
    this(message, statusCode, null, null);
  }

  /**
   * Creates a new SmriteaError with a message and no status code.
   *
   * @param message the error message
   */
  public SmriteaError(String message) {
    this(message, null, null, null);
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

  /**
   * Returns the full parsed JSON response body associated with this error, or null if not
   * available.
   *
   * @return the response body, or null
   */
  public Object getBody() {
    return body;
  }
}
