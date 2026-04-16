package smritea

import (
	"encoding/json"
	"fmt"
	"net/http"
	"strconv"
)

// SmriteaError is the base error type returned by all SDK operations.
// All other SDK error types embed SmriteaError.
type SmriteaError struct {
	Message string
	// ErrorCode holds the machine-readable error code from the server response (e.g. "MEMORY_NOT_FOUND").
	ErrorCode  string
	StatusCode int
}

// Error implements the error interface.
func (e *SmriteaError) Error() string {
	if e.ErrorCode != "" {
		return fmt.Sprintf("smritea: [%s] %s (HTTP %d)", e.ErrorCode, e.Message, e.StatusCode)
	}
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

// SmriteaDeserializationError is returned when the server returns a response that cannot
// be deserialized. This typically indicates an unexpected API response format or a
// server-side error that produced a malformed body.
type SmriteaDeserializationError struct {
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
// Attempts to extract the "message" field from JSON; falls back to raw body if parsing fails.
func mapError(resp *http.Response, body []byte) error {
	message, errorCode := extractErrorFields(body)

	base := SmriteaError{
		Message:    message,
		ErrorCode:  errorCode,
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
			Message:    message,
			ErrorCode:  errorCode,
			StatusCode: resp.StatusCode,
		}
	}
}

// extractErrorFields attempts to parse the response body as JSON and extract
// the "message" and "code" fields. Falls back to ("Unknown error", "INTERNAL_ERROR")
// if the body is absent, unparseable, or missing the expected fields.
// Never returns the raw response body as the message.
func extractErrorFields(body []byte) (message, errorCode string) {
	var data map[string]interface{}
	if err := json.Unmarshal(body, &data); err != nil {
		return "Unknown error", "INTERNAL_ERROR"
	}

	if msg, ok := data["message"].(string); ok && msg != "" {
		message = msg
	} else {
		message = "Unknown error"
	}

	if code, ok := data["code"].(string); ok && code != "" {
		errorCode = code
	} else {
		errorCode = "INTERNAL_ERROR"
	}

	return message, errorCode
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
