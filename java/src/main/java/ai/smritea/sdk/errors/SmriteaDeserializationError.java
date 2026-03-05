package ai.smritea.sdk.errors;

/**
 * Raised when the server response body cannot be deserialized into the expected type. This
 * indicates either a malformed response or an API contract mismatch between the SDK and the server.
 */
public class SmriteaDeserializationError extends SmriteaError {

  /**
   * Creates a new deserialization error.
   *
   * @param message description of what failed to deserialize
   */
  public SmriteaDeserializationError(String message) {
    super(message, null);
  }

  /**
   * Creates a new deserialization error wrapping a cause.
   *
   * @param message description of what failed to deserialize
   * @param cause the underlying exception (e.g. JsonProcessingException)
   */
  public SmriteaDeserializationError(String message, Throwable cause) {
    super(message);
    initCause(cause);
  }
}
