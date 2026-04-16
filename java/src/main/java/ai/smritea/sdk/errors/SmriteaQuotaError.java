package ai.smritea.sdk.errors;

/** Thrown when the API returns a 402 Payment Required response due to quota exhaustion. */
public class SmriteaQuotaError extends SmriteaError {
  /**
   * Creates a new SmriteaQuotaError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 402)
   * @param errorCode the error code from the API response, or null if not provided
   * @param body the full parsed JSON response body, or null if not available
   */
  public SmriteaQuotaError(String message, int statusCode, String errorCode, Object body) {
    super(message, statusCode, errorCode, body);
  }

  /**
   * Creates a new SmriteaQuotaError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 402)
   * @param errorCode the error code from the API response, or null if not provided
   */
  public SmriteaQuotaError(String message, int statusCode, String errorCode) {
    this(message, statusCode, errorCode, null);
  }

  /**
   * Creates a new SmriteaQuotaError.
   *
   * @param message the error message
   * @param statusCode the HTTP status code (typically 402)
   */
  public SmriteaQuotaError(String message, int statusCode) {
    this(message, statusCode, null, null);
  }
}
