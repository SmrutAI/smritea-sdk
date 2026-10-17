package smritea

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"slices"
	"strings"
	"sync/atomic"
	"testing"
)

// ---------------------------------------------------------------------------
// Test helpers
// ---------------------------------------------------------------------------

// newTraceTestClient mirrors newTestClient but also sets ClientConfig.TraceID,
// which newTestClient cannot express. t.Cleanup closes the server automatically.
func newTraceTestClient(t *testing.T, handler http.Handler, traceID string) *SmriteaClient {
	t.Helper()
	srv := httptest.NewServer(handler)
	t.Cleanup(srv.Close)
	return NewClient(ClientConfig{
		APIKey:     "test-key",
		AppID:      "app-test",
		BaseURL:    srv.URL,
		MaxRetries: 1,
		TraceID:    traceID,
	})
}

// baseSmriteaErrorOf returns the embedded *SmriteaError of whichever error type
// the chain holds. errors.As with a *SmriteaError target does not match the typed
// subtypes (they embed SmriteaError by value), so each subtype is tried explicitly.
func baseSmriteaErrorOf(err error) (*SmriteaError, bool) {
	var (
		badRequest    *SmriteaBadRequestError
		unauthorized  *SmriteaUnauthorizedError
		paymentReq    *SmriteaPaymentRequiredError
		forbidden     *SmriteaForbiddenError
		notFound      *SmriteaNotFoundError
		conflict      *SmriteaConflictError
		unprocessable *SmriteaUnprocessableError
		tooMany       *SmriteaTooManyRequestsError
	)
	switch {
	case errors.As(err, &badRequest):
		return &badRequest.SmriteaError, true
	case errors.As(err, &unauthorized):
		return &unauthorized.SmriteaError, true
	case errors.As(err, &paymentReq):
		return &paymentReq.SmriteaError, true
	case errors.As(err, &forbidden):
		return &forbidden.SmriteaError, true
	case errors.As(err, &notFound):
		return &notFound.SmriteaError, true
	case errors.As(err, &conflict):
		return &conflict.SmriteaError, true
	case errors.As(err, &unprocessable):
		return &unprocessable.SmriteaError, true
	case errors.As(err, &tooMany):
		return &tooMany.SmriteaError, true
	}
	var base *SmriteaError
	if errors.As(err, &base) {
		return base, true
	}
	return nil, false
}

// ---------------------------------------------------------------------------
// isValidTraceID (client.go)
// ---------------------------------------------------------------------------

func TestIsValidTraceID(t *testing.T) {
	tests := []struct {
		name string
		id   string
		want bool
	}{
		{name: "minimum length 8 bytes", id: "abcdefgh", want: true},
		{name: "maximum length 100 bytes", id: strings.Repeat("a", 100), want: true},
		{name: "dot underscore colon hyphen allowed", id: "a.b_c:d-e", want: true},
		{name: "7 bytes is below minimum", id: "abcdefg", want: false},
		{name: "101 bytes is above maximum", id: strings.Repeat("a", 101), want: false},
		{name: "space is not allowed", id: "abcd efgh", want: false},
		{name: "slash is not allowed", id: "abcd/efgh", want: false},
		{name: "empty string", id: "", want: false},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			if got := isValidTraceID(tc.id); got != tc.want {
				t.Errorf("isValidTraceID(%q) = %v, want %v", tc.id, got, tc.want)
			}
		})
	}
}

// ---------------------------------------------------------------------------
// X-Trace-ID header (client.go NewClient, default header)
// ---------------------------------------------------------------------------

func TestNewClient_TraceIDHeader(t *testing.T) {
	tests := []struct {
		name    string
		traceID string
		want    []string
	}{
		{name: "valid trace id is sent as X-Trace-ID", traceID: "trace-abc.123_x:y", want: []string{"trace-abc.123_x:y"}},
		{name: "unset trace id omits X-Trace-ID", traceID: "", want: nil},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			// Buffered so the handler never blocks; it sends before writing the response.
			seen := make(chan []string, 1)
			handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				seen <- r.Header.Values("X-Trace-ID")
				writeJSON(w, http.StatusOK, memSingleJSON("mem-1", "content"))
			})
			client := newTraceTestClient(t, handler, tc.traceID)

			if _, err := client.Get(context.Background(), "mem-1"); err != nil {
				t.Fatalf("Get: unexpected error: %v", err)
			}

			var got []string
			select {
			case got = <-seen:
			default:
				t.Fatal("handler was not called")
			}
			if !slices.Equal(got, tc.want) {
				t.Errorf("server received X-Trace-ID = %v, want %v", got, tc.want)
			}
		})
	}
}

// ---------------------------------------------------------------------------
// Invalid TraceID: every public method fails locally, no request is sent
// ---------------------------------------------------------------------------

func TestInvalidTraceID_EveryMethodRejectsWithoutRequest(t *testing.T) {
	const invalidTraceID = "bad trace/id"
	ctx := context.Background()

	tests := []struct {
		name string
		call func(c *SmriteaClient) error
	}{
		{name: "Add", call: func(c *SmriteaClient) error {
			_, err := c.Add(ctx, "content", nil)
			return err
		}},
		{name: "Search", call: func(c *SmriteaClient) error {
			_, err := c.Search(ctx, "query", nil)
			return err
		}},
		{name: "Get", call: func(c *SmriteaClient) error {
			_, err := c.Get(ctx, "mem-1")
			return err
		}},
		{name: "Delete", call: func(c *SmriteaClient) error {
			return c.Delete(ctx, "mem-1")
		}},
		{name: "GetAll", call: func(c *SmriteaClient) error {
			_, err := c.GetAll(ctx)
			return err
		}},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			var requests atomic.Int32
			handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
				requests.Add(1)
				writeJSON(w, http.StatusOK, memSingleJSON("mem-1", "content"))
			})
			client := newTraceTestClient(t, handler, invalidTraceID)

			err := tc.call(client)
			var sdkErr *SmriteaError
			if !errors.As(err, &sdkErr) {
				t.Fatalf("expected *SmriteaError, got %T: %v", err, err)
			}
			if sdkErr.Code != ErrCodeInvalidTraceID {
				t.Errorf("Code = %q, want %q", sdkErr.Code, ErrCodeInvalidTraceID)
			}
			if n := requests.Load(); n != 0 {
				t.Errorf("server received %d requests, want 0", n)
			}
		})
	}
}

// ---------------------------------------------------------------------------
// RequestID over HTTP: mapError -> extractRequestID (errors.go)
// ---------------------------------------------------------------------------

func TestMapError_RequestIDFromHTTPResponse(t *testing.T) {
	statuses := []int{
		http.StatusBadRequest,
		http.StatusUnauthorized,
		http.StatusPaymentRequired,
		http.StatusForbidden,
		http.StatusNotFound,
		http.StatusConflict,
		http.StatusUnprocessableEntity,
		http.StatusTooManyRequests,
		http.StatusInternalServerError,
	}

	tests := []struct {
		name            string
		bodyRequestID   string
		headerRequestID string
		want            string
	}{
		{
			name:            "body request_id wins over header",
			bodyRequestID:   "req-body-1",
			headerRequestID: "req-header-ignored",
			want:            "req-body-1",
		},
		{
			name:            "header used when body has no request_id",
			headerRequestID: "req-header-1",
			want:            "req-header-1",
		},
		{
			name: "empty when neither body nor header carries an ID",
			want: "",
		},
	}

	for _, status := range statuses {
		for _, tc := range tests {
			t.Run(fmt.Sprintf("%d/%s", status, tc.name), func(t *testing.T) {
				body := map[string]any{"message": "request failed", "code": "TEST_ERROR"}
				if tc.bodyRequestID != "" {
					body["request_id"] = tc.bodyRequestID
				}
				bodyBytes, err := json.Marshal(body)
				if err != nil {
					t.Fatalf("marshal error body: %v", err)
				}

				handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
					// Header.Set canonicalizes the key to X-Request-Id.
					if tc.headerRequestID != "" {
						w.Header().Set("X-Request-ID", tc.headerRequestID)
					}
					if status == http.StatusTooManyRequests {
						// Short Retry-After keeps the single retry under one second.
						w.Header().Set("Retry-After", "1")
					}
					writeJSON(w, status, bodyBytes)
				})
				client := newTraceTestClient(t, handler, "")

				_, callErr := client.Get(context.Background(), "mem-1")
				sdkErr, ok := baseSmriteaErrorOf(callErr)
				if !ok {
					t.Fatalf("expected *SmriteaError, got %T: %v", callErr, callErr)
				}
				if sdkErr.HTTPStatus != status {
					t.Errorf("HTTPStatus = %d, want %d", sdkErr.HTTPStatus, status)
				}
				if sdkErr.RequestID != tc.want {
					t.Errorf("RequestID = %q, want %q", sdkErr.RequestID, tc.want)
				}
			})
		}
	}
}

// ---------------------------------------------------------------------------
// extractRequestID unit table (errors.go)
// ---------------------------------------------------------------------------

func TestExtractRequestID(t *testing.T) {
	tests := []struct {
		name       string
		parsedBody map[string]any
		header     http.Header
		want       string
	}{
		{
			name:       "non-empty body string wins over header",
			parsedBody: map[string]any{"request_id": "req-body"},
			header:     http.Header{"X-Request-Id": {"req-header"}},
			want:       "req-body",
		},
		{
			name:       "empty body string falls back to header",
			parsedBody: map[string]any{"request_id": ""},
			header:     http.Header{"X-Request-Id": {"req-header"}},
			want:       "req-header",
		},
		{
			name:       "non-string body value falls back to header",
			parsedBody: map[string]any{"request_id": 42},
			header:     http.Header{"X-Request-Id": {"req-header"}},
			want:       "req-header",
		},
		{
			name:       "nil parsed body (unparseable response) falls back to header",
			parsedBody: nil,
			header:     http.Header{"X-Request-Id": {"req-header"}},
			want:       "req-header",
		},
		{
			name:       "header missing gives empty string",
			parsedBody: map[string]any{"message": "x"},
			header:     http.Header{},
			want:       "",
		},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			if got := extractRequestID(tc.parsedBody, tc.header); got != tc.want {
				t.Errorf("extractRequestID() = %q, want %q", got, tc.want)
			}
		})
	}
}
