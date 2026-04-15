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

	var target *SmriteaValidationError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaValidationError, got %T", err)
	}
	if target.StatusCode != 400 {
		t.Errorf("expected StatusCode 400, got %d", target.StatusCode)
	}
	if target.Message != "field required" {
		t.Errorf("expected message %q, got %q", "field required", target.Message)
	}
}

func TestMapError_400_ExtractsJSONMessage(t *testing.T) {
	body := []byte(`{"message":"field required","code":"validation_failed"}`)
	resp := mockResponse(http.StatusBadRequest, string(body), nil)
	err := mapError(resp, body)

	var target *SmriteaValidationError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaValidationError, got %T", err)
	}
	if target.Message != "field required" {
		t.Errorf("expected message %q, got %q", "field required", target.Message)
	}
}

func TestMapError_400_FallsBackOnInvalidJSON(t *testing.T) {
	body := []byte(`{"message":`)
	resp := mockResponse(http.StatusBadRequest, string(body), nil)
	err := mapError(resp, body)

	var target *SmriteaValidationError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaValidationError, got %T", err)
	}
	if target.Message != string(body) {
		t.Errorf("expected fallback message %q, got %q", string(body), target.Message)
	}
}

func TestMapError_400_FallsBackOnMissingMessageField(t *testing.T) {
	body := []byte(`{"error":"field required"}`)
	resp := mockResponse(http.StatusBadRequest, string(body), nil)
	err := mapError(resp, body)

	var target *SmriteaValidationError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaValidationError, got %T", err)
	}
	if target.Message != string(body) {
		t.Errorf("expected fallback message %q, got %q", string(body), target.Message)
	}
}

func TestMapError_400_FallsBackOnEmptyMessageField(t *testing.T) {
	body := []byte(`{"message":""}`)
	resp := mockResponse(http.StatusBadRequest, string(body), nil)
	err := mapError(resp, body)

	var target *SmriteaValidationError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaValidationError, got %T", err)
	}
	if target.Message != string(body) {
		t.Errorf("expected fallback message %q, got %q", string(body), target.Message)
	}
}

func TestMapError_401_ReturnsAuthError(t *testing.T) {
	resp := mockResponse(http.StatusUnauthorized, "invalid api key", nil)
	err := mapError(resp, []byte("invalid api key"))

	var target *SmriteaAuthError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaAuthError, got %T", err)
	}
	if target.StatusCode != 401 {
		t.Errorf("expected StatusCode 401, got %d", target.StatusCode)
	}
}

func TestMapError_402_ReturnsQuotaError(t *testing.T) {
	resp := mockResponse(http.StatusPaymentRequired, "quota exceeded", nil)
	err := mapError(resp, []byte("quota exceeded"))

	var target *SmriteaQuotaError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaQuotaError, got %T", err)
	}
	if target.StatusCode != 402 {
		t.Errorf("expected StatusCode 402, got %d", target.StatusCode)
	}
}

func TestMapError_404_ReturnsNotFoundError(t *testing.T) {
	resp := mockResponse(http.StatusNotFound, "memory not found", nil)
	err := mapError(resp, []byte("memory not found"))

	var target *SmriteaNotFoundError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaNotFoundError, got %T", err)
	}
	if target.StatusCode != 404 {
		t.Errorf("expected StatusCode 404, got %d", target.StatusCode)
	}
}

func TestMapError_429_ReturnsRateLimitError(t *testing.T) {
	headers := map[string]string{"Retry-After": "30"}
	resp := mockResponse(http.StatusTooManyRequests, "rate limited", headers)
	err := mapError(resp, []byte("rate limited"))

	var target *SmriteaRateLimitError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaRateLimitError, got %T", err)
	}
	if target.StatusCode != 429 {
		t.Errorf("expected StatusCode 429, got %d", target.StatusCode)
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

	var target *SmriteaRateLimitError
	if !errors.As(err, &target) {
		t.Fatalf("expected *SmriteaRateLimitError, got %T", err)
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
	var ve *SmriteaValidationError
	var ae *SmriteaAuthError
	var qe *SmriteaQuotaError
	var nfe *SmriteaNotFoundError
	var rle *SmriteaRateLimitError

	if errors.As(err, &ve) {
		t.Error("500 should not map to *SmriteaValidationError")
	}
	if errors.As(err, &ae) {
		t.Error("500 should not map to *SmriteaAuthError")
	}
	if errors.As(err, &qe) {
		t.Error("500 should not map to *SmriteaQuotaError")
	}
	if errors.As(err, &nfe) {
		t.Error("500 should not map to *SmriteaNotFoundError")
	}
	if errors.As(err, &rle) {
		t.Error("500 should not map to *SmriteaRateLimitError")
	}

	if base.StatusCode != 500 {
		t.Errorf("expected StatusCode 500, got %d", base.StatusCode)
	}
	if base.Message != "internal error" {
		t.Errorf("expected message %q, got %q", "internal error", base.Message)
	}
}

func TestSmriteaError_ErrorMessage(t *testing.T) {
	e := &SmriteaError{Message: "something went wrong", StatusCode: 503}
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
