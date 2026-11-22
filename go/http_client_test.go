package smritea

import (
	"context"
	"errors"
	"net/http"
	"net/http/httptest"
	"sync/atomic"
	"testing"
)

// countingTransport counts the requests that pass through it.
type countingTransport struct {
	count atomic.Int32
}

func (c *countingTransport) RoundTrip(r *http.Request) (*http.Response, error) {
	c.count.Add(1)
	return http.DefaultTransport.RoundTrip(r)
}

// seenRequest holds what the fake server received.
type seenRequest struct {
	path    string
	apiKey  string
	traceID string
}

func TestNewClient_HTTPClient(t *testing.T) {
	tests := []struct {
		name      string
		useCustom bool
		traceID   string
	}{
		{name: "custom client without trace id", useCustom: true},
		{name: "custom client with trace id", useCustom: true, traceID: "trace-abc12345"},
		{name: "nil client uses the SDK default client", useCustom: false},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			seen := make(chan seenRequest, 1)
			srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
				seen <- seenRequest{path: r.URL.Path, apiKey: r.Header.Get("X-API-Key"), traceID: r.Header.Get("X-Trace-ID")}
				writeJSON(w, http.StatusNotFound, errJSON("Memory not found"))
			}))
			t.Cleanup(srv.Close)

			transport := &countingTransport{}
			var httpClient *http.Client
			if tc.useCustom {
				httpClient = &http.Client{Transport: transport}
			}
			client := NewClient(ClientConfig{
				APIKey:     "test-key",
				AppID:      "app-test",
				BaseURL:    srv.URL,
				MaxRetries: Int(0),
				TraceID:    tc.traceID,
				HTTPClient: httpClient,
			})

			_, err := client.Get(context.Background(), "mem_1")

			var notFound *SmriteaNotFoundError
			if !errors.As(err, &notFound) {
				t.Fatalf("expected *SmriteaNotFoundError, got %T: %v", err, err)
			}
			got := <-seen
			if got.path != "/api/v1/sdk/memories/mem_1" {
				t.Errorf("path = %q, want /api/v1/sdk/memories/mem_1", got.path)
			}
			if got.apiKey != "test-key" {
				t.Errorf("X-API-Key = %q, want test-key", got.apiKey)
			}
			if got.traceID != tc.traceID {
				t.Errorf("X-Trace-ID = %q, want %q", got.traceID, tc.traceID)
			}
			wantCount := int32(0)
			if tc.useCustom {
				wantCount = 1
			}
			if c := transport.count.Load(); c != wantCount {
				t.Errorf("custom transport saw %d requests, want %d", c, wantCount)
			}
			if tc.useCustom {
				if client.cfg.HTTPClient != httpClient {
					t.Errorf("cfg.HTTPClient = %p, want the supplied client %p", client.cfg.HTTPClient, httpClient)
				}
			} else if client.cfg.HTTPClient == http.DefaultClient || client.cfg.HTTPClient.Timeout != DefaultTimeout {
				t.Errorf("cfg.HTTPClient = %p with Timeout %v, want a new client with Timeout %v",
					client.cfg.HTTPClient, client.cfg.HTTPClient.Timeout, DefaultTimeout)
			}
		})
	}
}
