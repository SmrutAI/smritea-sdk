package smritea

import (
	"errors"
	"io"
	"net/http"
	"strings"
	"testing"
)

// mockResponse constructs a minimal *http.Response for testing. The Body is
// backed by a strings.Reader so it can be read exactly once; callers must not
// close it inside the helper (mapError and parseRetryAfter handle that).
func mockResponse(statusCode int, body string, headers map[string]string) *http.Response {
	resp := &http.Response{
		StatusCode: statusCode,
		Body:       io.NopCloser(strings.NewReader(body)),
		Header:     http.Header{},
	}
	for k, v := range headers {
		resp.Header.Set(k, v)
	}
	return resp
}

func TestMapError_400_ReturnsValidationError(t *testing.T) {
	resp := mockResponse(http.StatusBadRequest, "field required", nil)
	err := mapError(resp, []byte("field required"))

	var target *SmriteaBadRequestError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaBadRequestError, got %T", err)
	}
	if target.HTTPStatus != 400 {
		t.Errorf("expected HTTPStatus 400, got %d", target.HTTPStatus)
	}
	// Plain-text (non-JSON) body: server did not send the expected JSON envelope,
	// so extractErrorFields falls back to "Unknown error" rather than surfacing raw body.
	if target.Message != "Unknown error" {
		t.Errorf("expected message %q, got %q", "Unknown error", target.Message)
	}
}

func TestMapError_400_ExtractsJSONMessage(t *testing.T) {
	body := []byte(`{"message":"field required","code":"validation_failed"}`)
	resp := mockResponse(http.StatusBadRequest, string(body), nil)
	err := mapError(resp, body)

	var target *SmriteaBadRequestError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaBadRequestError, got %T", err)
	}
	if target.Message != "field required" {
		t.Errorf("expected message %q, got %q", "field required", target.Message)
	}
}

func TestMapError_400_FallsBackOnInvalidJSON(t *testing.T) {
	body := []byte(`{"message":`)
	resp := mockResponse(http.StatusBadRequest, string(body), nil)
	err := mapError(resp, body)

	var target *SmriteaBadRequestError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaBadRequestError, got %T", err)
	}
	// Truncated/invalid JSON: raw body is never surfaced; fallback is "Unknown error".
	if target.Message != "Unknown error" {
		t.Errorf("expected fallback message %q, got %q", "Unknown error", target.Message)
	}
}

func TestMapError_400_FallsBackOnMissingMessageField(t *testing.T) {
	body := []byte(`{"error":"field required"}`)
	resp := mockResponse(http.StatusBadRequest, string(body), nil)
	err := mapError(resp, body)

	var target *SmriteaBadRequestError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaBadRequestError, got %T", err)
	}
	// JSON without "message" field: raw body is never surfaced; fallback is "Unknown error".
	if target.Message != "Unknown error" {
		t.Errorf("expected fallback message %q, got %q", "Unknown error", target.Message)
	}
}

func TestMapError_400_FallsBackOnEmptyMessageField(t *testing.T) {
	body := []byte(`{"message":""}`)
	resp := mockResponse(http.StatusBadRequest, string(body), nil)
	err := mapError(resp, body)

	var target *SmriteaBadRequestError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaBadRequestError, got %T", err)
	}
	// Empty "message" field: raw body is never surfaced; fallback is "Unknown error".
	if target.Message != "Unknown error" {
		t.Errorf("expected fallback message %q, got %q", "Unknown error", target.Message)
	}
}

func TestMapError_401_ReturnsAuthError(t *testing.T) {
	resp := mockResponse(http.StatusUnauthorized, "invalid api key", nil)
	err := mapError(resp, []byte("invalid api key"))

	var target *SmriteaUnauthorizedError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaUnauthorizedError, got %T", err)
	}
	if target.HTTPStatus != 401 {
		t.Errorf("expected HTTPStatus 401, got %d", target.HTTPStatus)
	}
}

func TestMapError_402_ReturnsQuotaError(t *testing.T) {
	resp := mockResponse(http.StatusPaymentRequired, "quota exceeded", nil)
	err := mapError(resp, []byte("quota exceeded"))

	var target *SmriteaPaymentRequiredError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaPaymentRequiredError, got %T", err)
	}
	if target.HTTPStatus != 402 {
		t.Errorf("expected HTTPStatus 402, got %d", target.HTTPStatus)
	}
}

func TestMapError_403_ReturnsForbiddenError(t *testing.T) {
	resp := mockResponse(http.StatusForbidden, "access denied", nil)
	err := mapError(resp, []byte("access denied"))

	var target *SmriteaForbiddenError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaForbiddenError, got %T", err)
	}
	if target.HTTPStatus != 403 {
		t.Errorf("expected HTTPStatus 403, got %d", target.HTTPStatus)
	}
}

func TestMapError_409_ReturnsConflictError(t *testing.T) {
	resp := mockResponse(http.StatusConflict, "conflicting state", nil)
	err := mapError(resp, []byte("conflicting state"))

	var target *SmriteaConflictError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaConflictError, got %T", err)
	}
	if target.HTTPStatus != 409 {
		t.Errorf("expected HTTPStatus 409, got %d", target.HTTPStatus)
	}
}

func TestMapError_422_ReturnsUnprocessableError(t *testing.T) {
	resp := mockResponse(http.StatusUnprocessableEntity, "cannot process", nil)
	err := mapError(resp, []byte("cannot process"))

	var target *SmriteaUnprocessableError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaUnprocessableError, got %T", err)
	}
	if target.HTTPStatus != 422 {
		t.Errorf("expected HTTPStatus 422, got %d", target.HTTPStatus)
	}
}

func TestMapError_RetryableFromBody(t *testing.T) {
	resp := mockResponse(http.StatusServiceUnavailable, `{"message":"try later","code":"UNAVAILABLE","retryable":true}`, nil)
	err := mapError(resp, []byte(`{"message":"try later","code":"UNAVAILABLE","retryable":true}`))

	var target *SmriteaError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaError, got %T", err)
	}
	if !target.Retryable {
		t.Error("expected Retryable to be true when the body's \"retryable\" field is true")
	}
}

func TestMapError_RetryableFalseWhenBodyOmitsField(t *testing.T) {
	resp := mockResponse(http.StatusBadRequest, `{"message":"bad input","code":"BAD_INPUT"}`, nil)
	err := mapError(resp, []byte(`{"message":"bad input","code":"BAD_INPUT"}`))

	var target *SmriteaBadRequestError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaBadRequestError, got %T", err)
	}
	if target.Retryable {
		t.Error("expected Retryable to be false when the body omits the \"retryable\" field")
	}
}

func TestMapError_404_ReturnsNotFoundError(t *testing.T) {
	resp := mockResponse(http.StatusNotFound, "memory not found", nil)
	err := mapError(resp, []byte("memory not found"))

	var target *SmriteaNotFoundError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaNotFoundError, got %T", err)
	}
	if target.HTTPStatus != 404 {
		t.Errorf("expected HTTPStatus 404, got %d", target.HTTPStatus)
	}
}

func TestMapError_429_ReturnsRateLimitError(t *testing.T) {
	headers := map[string]string{"Retry-After": "30"}
	resp := mockResponse(http.StatusTooManyRequests, "rate limited", headers)
	err := mapError(resp, []byte("rate limited"))

	var target *SmriteaTooManyRequestsError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaTooManyRequestsError, got %T", err)
	}
	if target.HTTPStatus != 429 {
		t.Errorf("expected HTTPStatus 429, got %d", target.HTTPStatus)
	}
	if target.RetryAfter == nil {
		t.Fatal("expected RetryAfter to be non-nil")
	}
	if *target.RetryAfter != 30 {
		t.Errorf("expected RetryAfter 30, got %d", *target.RetryAfter)
	}
}

func TestMapError_429_NoRetryAfter(t *testing.T) {
	resp := mockResponse(http.StatusTooManyRequests, "rate limited", nil)
	err := mapError(resp, []byte("rate limited"))

	var target *SmriteaTooManyRequestsError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaTooManyRequestsError, got %T", err)
	}
	if target.RetryAfter != nil {
		t.Errorf("expected RetryAfter to be nil, got %d", *target.RetryAfter)
	}
}

func TestMapError_500_ReturnsBaseError(t *testing.T) {
	resp := mockResponse(http.StatusInternalServerError, "internal error", nil)
	err := mapError(resp, []byte("internal error"))

	// Must be *SmriteaError at the base level.
	var base *SmriteaError
	if !errors.As(err, &base) {
		t.Fatalf("expected *SmriteaError, got %T", err)
	}

	// Must NOT be any of the named subtypes.
	var ve *SmriteaBadRequestError
	var ae *SmriteaUnauthorizedError
	var qe *SmriteaPaymentRequiredError
	var nfe *SmriteaNotFoundError
	var rle *SmriteaTooManyRequestsError

	if errors.As(err, &ve) {
		t.Error("500 should not map to *SmriteaBadRequestError")
	}
	if errors.As(err, &ae) {
		t.Error("500 should not map to *SmriteaUnauthorizedError")
	}
	if errors.As(err, &qe) {
		t.Error("500 should not map to *SmriteaPaymentRequiredError")
	}
	if errors.As(err, &nfe) {
		t.Error("500 should not map to *SmriteaNotFoundError")
	}
	if errors.As(err, &rle) {
		t.Error("500 should not map to *SmriteaTooManyRequestsError")
	}

	if base.HTTPStatus != 500 {
		t.Errorf("expected HTTPStatus 500, got %d", base.HTTPStatus)
	}
	// Plain-text body: raw body is never surfaced; fallback is "Unknown error".
	if base.Message != "Unknown error" {
		t.Errorf("expected message %q, got %q", "Unknown error", base.Message)
	}
}

func TestSmriteaError_ErrorMessage(t *testing.T) {
	e := &SmriteaError{Message: "something went wrong", HTTPStatus: 503}
	want := "smritea: something went wrong (HTTP 503)"
	if got := e.Error(); got != want {
		t.Errorf("Error() = %q, want %q", got, want)
	}
}

func TestParseRetryAfter_ValidHeader(t *testing.T) {
	resp := mockResponse(http.StatusTooManyRequests, "", map[string]string{
		"Retry-After": "10",
	})
	got := parseRetryAfter(resp)
	if got == nil {
		t.Fatal("expected non-nil *int, got nil")
	}
	if *got != 10 {
		t.Errorf("expected 10, got %d", *got)
	}
}

func TestParseRetryAfter_MissingHeader(t *testing.T) {
	resp := mockResponse(http.StatusTooManyRequests, "", nil)
	got := parseRetryAfter(resp)
	if got != nil {
		t.Errorf("expected nil, got %d", *got)
	}
}

func TestParseRetryAfter_InvalidHeader(t *testing.T) {
	resp := mockResponse(http.StatusTooManyRequests, "", map[string]string{
		"Retry-After": "not-a-number",
	})
	got := parseRetryAfter(resp)
	if got != nil {
		t.Errorf("expected nil for non-integer header, got %d", *got)
	}
}
