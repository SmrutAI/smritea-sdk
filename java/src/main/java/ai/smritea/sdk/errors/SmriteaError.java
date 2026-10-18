package ai.smritea.sdk.errors;

/**
 * Base error type for all Smritea SDK errors.
 *
 * <p>Mirrors the server's HTTP error categories.
 */
public class SmriteaError extends RuntimeException {
  private final Integer httpStatus;
  private final String code;
  private final boolean retryable;
  private final Object body;
  private final String requestId;

  /**
   * Creates a new SmriteaError with a message, HTTP status code, wire code, retryable flag, body,
   * and server request ID.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code, or null if not applicable
   * @param code the machine-readable wire code from the API response (e.g. "MEMORY_NOT_FOUND"), or
   *     null if not provided (defaults to "INTERNAL_ERROR")
   * @param retryable whether the server marked this error as retryable
   * @param body the full parsed JSON response body, or null if not available
   * @param requestId the server request ID (body {@code request_id} or the {@code X-Request-ID}
   *     response header), or null if not available
   */
  public SmriteaError(
      String message,
      Integer httpStatus,
      String code,
      boolean retryable,
      Object body,
      String requestId) {
    super(message);
    this.httpStatus = httpStatus;
    this.code = code != null ? code : "INTERNAL_ERROR";
    this.retryable = retryable;
    this.body = body;
    this.requestId = requestId;
  }

  /**
   * Creates a new SmriteaError with a message, HTTP status code, wire code, retryable flag, and
   * body. The server request ID is not available.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code, or null if not applicable
   * @param code the machine-readable wire code from the API response (e.g. "MEMORY_NOT_FOUND"), or
   *     null if not provided (defaults to "INTERNAL_ERROR")
   * @param retryable whether the server marked this error as retryable
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaError(
      String message, Integer httpStatus, String code, boolean retryable, Object body) {
    this(message, httpStatus, code, retryable, body, null);
  }

  /**
   * Creates a new SmriteaError with a message, HTTP status code, wire code, and retryable flag.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code, or null if not applicable
   * @param code the machine-readable wire code from the API response, or null if not provided
   *     (defaults to "INTERNAL_ERROR")
   * @param retryable whether the server marked this error as retryable
   */
  public SmriteaError(String message, Integer httpStatus, String code, boolean retryable) {
    this(message, httpStatus, code, retryable, null);
  }

  /**
   * Creates a new SmriteaError with a message and HTTP status code. Not retryable.
   *
   * @param message the error message
   * @param httpStatus the HTTP status code, or null if not applicable
   */
  public SmriteaError(String message, Integer httpStatus) {
    this(message, httpStatus, null, false, null);
  }

  /**
   * Creates a new SmriteaError with a message and no status code. Not retryable.
   *
   * @param message the error message
   */
  public SmriteaError(String message) {
    this(message, null, null, false, null);
  }

  /**
   * Returns the HTTP status code associated with this error, or null if not applicable.
   *
   * @return the HTTP status code, or null
   */
  public Integer getHttpStatus() {
    return httpStatus;
  }

  /**
   * Returns the machine-readable wire code from the API response, or "INTERNAL_ERROR" if not
   * provided.
   *
   * @return the wire code
   */
  public String getCode() {
    return code;
  }

  /**
   * Returns whether the server marked this error as retryable.
   *
   * @return true if retryable, false otherwise
   */
  public boolean isRetryable() {
    return retryable;
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

  /**
   * Returns the server request ID for this error, taken from the response body {@code request_id}
   * field or the {@code X-Request-ID} response header, or null if neither was present.
   *
   * @return the request ID, or null
   */
  public String getRequestId() {
    return requestId;
  }
}
