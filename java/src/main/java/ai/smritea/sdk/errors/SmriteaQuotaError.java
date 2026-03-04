package ai.smritea.sdk.errors;

/** Thrown when the API returns a 402 Payment Required response due to quota exhaustion. */
public class SmriteaQuotaError extends SmriteaError {
    /**
     * Creates a new SmriteaQuotaError.
     *
     * @param message the error message
     * @param statusCode the HTTP status code (typically 402)
     */
    public SmriteaQuotaError(String message, int statusCode) {
        super(message, statusCode);
    }
}
