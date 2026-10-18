package smritea

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
)

// TestMaxRetries_AttemptCount verifies how many HTTP requests the client sends when the
// server always answers 429, for each MaxRetries setting. A nil pointer selects the
// default of 2 retries; a negative value is clamped to 0.
func TestMaxRetries_AttemptCount(t *testing.T) {
	tests := []struct {
		name         string
		maxRetries   *int
		wantAttempts int32
	}{
		{name: "nil uses default of 2 retries (3 attempts)", maxRetries: nil, wantAttempts: 3},
		{name: "pointer to 0 disables retry (1 attempt)", maxRetries: Int(0), wantAttempts: 1},
		{name: "pointer to 1 allows 1 retry (2 attempts)", maxRetries: Int(1), wantAttempts: 2},
		{name: "negative value is clamped to 0 (1 attempt)", maxRetries: Int(-1), wantAttempts: 1},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			var attempts atomic.Int32
			handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
				attempts.Add(1)
				// Retry-After: 1 makes every retry sleep a fixed 900ms, keeping the test fast.
				w.Header().Set("Retry-After", "1")
				writeJSON(w, http.StatusTooManyRequests, errJSON("rate limited"))
			})
			srv := httptest.NewServer(handler)
			t.Cleanup(srv.Close)

			client := NewClient(ClientConfig{
				APIKey:     "test-key",
				AppID:      "app-test",
				BaseURL:    srv.URL,
				MaxRetries: tc.maxRetries,
			})

			_, err := client.Add(context.Background(), "x", nil)
			var rlErr *SmriteaTooManyRequestsError
			if !errors.As(err, &rlErr) {
				t.Fatalf("expected SmriteaTooManyRequestsError after retries are exhausted, got %T: %v", err, err)
			}
			if got := attempts.Load(); got != tc.wantAttempts {
				t.Errorf("server received %d attempts, want %d", got, tc.wantAttempts)
			}
		})
	}
}
