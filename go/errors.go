package smritea

import (
	"fmt"
	"net/http"
	"strconv"
)

// SmriteaError is the base error type returned by all SDK operations.
// All other SDK error types embed SmriteaError.
type SmriteaError struct {
	Message    string
	StatusCode int
}

// Error implements the error interface.
func (e *SmriteaError) Error() string {
	return fmt.Sprintf("smritea: %s (HTTP %d)", e.Message, e.StatusCode)
}

// SmriteaAuthError is returned when the server responds with HTTP 401 Unauthorized.
// This typically means the API key is missing, expired, or invalid.
type SmriteaAuthError struct {
	SmriteaError
}

// SmriteaNotFoundError is returned when the server responds with HTTP 404 Not Found.
type SmriteaNotFoundError struct {
	SmriteaError
}

// SmriteaValidationError is returned when the server responds with HTTP 400 Bad Request.
// The Message field contains the server-provided validation detail.
type SmriteaValidationError struct {
	SmriteaError
}

// SmriteaQuotaError is returned when the server responds with HTTP 402 Payment Required.
// This indicates the organization has exceeded its plan quota.
type SmriteaQuotaError struct {
	SmriteaError
}

// SmriteaRateLimitError is returned when the server responds with HTTP 429 Too Many Requests.
// RetryAfter holds the number of seconds to wait before retrying, parsed from the
// Retry-After response header. It is nil if the header was absent or unparseable.
type SmriteaRateLimitError struct {
	SmriteaError
	RetryAfter *int
}

// mapError converts an HTTP response into the appropriate typed SDK error.
// body is the already-read response body used as the error message.
func mapError(resp *http.Response, body []byte) error {
	base := SmriteaError{
		Message:    string(body),
		StatusCode: resp.StatusCode,
	}

	switch resp.StatusCode {
	case http.StatusBadRequest:
		return &SmriteaValidationError{SmriteaError: base}
	case http.StatusUnauthorized:
		return &SmriteaAuthError{SmriteaError: base}
	case http.StatusPaymentRequired:
		return &SmriteaQuotaError{SmriteaError: base}
	case http.StatusNotFound:
		return &SmriteaNotFoundError{SmriteaError: base}
	case http.StatusTooManyRequests:
		return &SmriteaRateLimitError{
			SmriteaError: base,
			RetryAfter:   parseRetryAfter(resp),
		}
	default:
		return &SmriteaError{
			Message:    string(body),
			StatusCode: resp.StatusCode,
		}
	}
}

// parseRetryAfter reads the Retry-After header from the response and parses it as
// an integer number of seconds. Returns nil if the header is absent or not an integer.
func parseRetryAfter(resp *http.Response) *int {
	raw := resp.Header.Get("Retry-After")
	if raw == "" {
		return nil
	}

	val, err := strconv.Atoi(raw)
	if err != nil {
		return nil
	}

	return &val
}
