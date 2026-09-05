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
	// Code holds the machine-readable error code from the server response (e.g. "MEMORY_NOT_FOUND").
	Code string
	// HTTPStatus is the HTTP status code from the server response.
	HTTPStatus int
	// Retryable reports whether the server marked this error safe to retry; parsed
	// from the response body's optional "retryable" field, and always true for 429.
	Retryable bool
	// Body holds the full parsed JSON response body from the API.
	Body map[string]any
}

// Error implements the error interface.
func (e *SmriteaError) Error() string {
	if e.Code != "" {
		return fmt.Sprintf("smritea: [%s] %s (HTTP %d)", e.Code, e.Message, e.HTTPStatus)
	}
	return fmt.Sprintf("smritea: %s (HTTP %d)", e.Message, e.HTTPStatus)
}

// SmriteaBadRequestError is returned when the server responds with HTTP 400 Bad Request.
// The Message field contains the server-provided validation detail.
type SmriteaBadRequestError struct {
	SmriteaError
}

// SmriteaUnauthorizedError is returned when the server responds with HTTP 401 Unauthorized.
// This typically means the API key is missing, expired, or invalid.
type SmriteaUnauthorizedError struct {
	SmriteaError
}

// SmriteaPaymentRequiredError is returned when the server responds with HTTP 402 Payment Required.
// This indicates the organization has exceeded its plan quota.
type SmriteaPaymentRequiredError struct {
	SmriteaError
}

// SmriteaForbiddenError is returned when the server responds with HTTP 403 Forbidden.
type SmriteaForbiddenError struct {
	SmriteaError
}

// SmriteaNotFoundError is returned when the server responds with HTTP 404 Not Found.
type SmriteaNotFoundError struct {
	SmriteaError
}

// SmriteaConflictError is returned when the server responds with HTTP 409 Conflict.
type SmriteaConflictError struct {
	SmriteaError
}

// SmriteaUnprocessableError is returned when the server responds with HTTP 422 Unprocessable Entity.
type SmriteaUnprocessableError struct {
	SmriteaError
}

// SmriteaTooManyRequestsError is returned when the server responds with HTTP 429 Too Many Requests.
// RetryAfter holds the number of seconds to wait before retrying, parsed from the
// Retry-After response header. It is nil if the header was absent or unparseable.
type SmriteaTooManyRequestsError struct {
	SmriteaError
	RetryAfter *int
}

// SmriteaDeserializationError is returned when the server returns a response that cannot
// be deserialized. This typically indicates an unexpected API response format or a
// server-side error that produced a malformed body.
type SmriteaDeserializationError struct {
	SmriteaError
}

// mapError converts an HTTP response into the appropriate typed SDK error.
// body is the already-read response body used as the error message.
// Attempts to extract the "message" field from JSON; falls back to raw body if parsing fails.
func mapError(resp *http.Response, body []byte) error {
	message, code, retryable, parsedBody := extractErrorFields(body)

	base := SmriteaError{
		Message:    message,
		Code:       code,
		HTTPStatus: resp.StatusCode,
		Retryable:  retryable || resp.StatusCode == http.StatusTooManyRequests,
		Body:       parsedBody,
	}

	switch resp.StatusCode {
	case http.StatusBadRequest:
		return &SmriteaBadRequestError{SmriteaError: base}
	case http.StatusUnauthorized:
		return &SmriteaUnauthorizedError{SmriteaError: base}
	case http.StatusPaymentRequired:
		return &SmriteaPaymentRequiredError{SmriteaError: base}
	case http.StatusForbidden:
		return &SmriteaForbiddenError{SmriteaError: base}
	case http.StatusNotFound:
		return &SmriteaNotFoundError{SmriteaError: base}
	case http.StatusConflict:
		return &SmriteaConflictError{SmriteaError: base}
	case http.StatusUnprocessableEntity:
		return &SmriteaUnprocessableError{SmriteaError: base}
	case http.StatusTooManyRequests:
		return &SmriteaTooManyRequestsError{
			SmriteaError: base,
			RetryAfter:   parseRetryAfter(resp),
		}
	default:
		return &base
	}
}

// extractErrorFields attempts to parse the response body as JSON and extract
// the "message", "code", and "retryable" fields. Falls back to ("Unknown error",
// "INTERNAL_ERROR", false) if the body is absent, unparseable, or missing the
// expected fields. Never returns the raw response body as the message.
// Returns (message, errorCode, retryable, parsedBody) where parsedBody is nil if parsing fails.
func extractErrorFields(body []byte) (message, errorCode string, retryable bool, parsedBody map[string]any) {
	var data map[string]any
	if err := json.Unmarshal(body, &data); err != nil {
		return "Unknown error", "INTERNAL_ERROR", false, nil
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

	if r, ok := data["retryable"].(bool); ok {
		retryable = r
	}

	return message, errorCode, retryable, data
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
