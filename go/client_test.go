package smritea

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync/atomic"
	"testing"
	"time"
)

// ---------------------------------------------------------------------------
// Test helpers
// ---------------------------------------------------------------------------

func intPtr(n int) *int       { return &n }
func strPtr(s string) *string { return &s }

// newTestClient spins up an httptest server with handler and returns a client
// pointed at it.  t.Cleanup closes the server automatically.
// maxRetries is forwarded verbatim to ClientConfig; pass ≥1 for retry tests.
func newTestClient(t *testing.T, handler http.Handler, maxRetries int) *SmriteaClient {
	t.Helper()
	srv := httptest.NewServer(handler)
	t.Cleanup(srv.Close)
	return NewClient(ClientConfig{
		APIKey:     "test-key",
		AppID:      "app-test",
		BaseURL:    srv.URL,
		MaxRetries: maxRetries,
	})
}

// memJSON returns a minimal MemoryMemoryResponse JSON body.
func memJSON(id, content string) []byte {
	return []byte(fmt.Sprintf(`{"id":%q,"content":%q,"app_id":"app-test"}`, id, content))
}

// writeJSON writes status + JSON body.
func writeJSON(w http.ResponseWriter, status int, body []byte) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if _, err := w.Write(body); err != nil {
		panic(fmt.Sprintf("writeJSON: %v", err))
	}
}

// errJSON returns a minimal error body the autogen can decode as ErrorsAppError.
func errJSON(msg string) []byte {
	return []byte(fmt.Sprintf(`{"detail":%q}`, msg))
}

// ---------------------------------------------------------------------------
// retryDelay tests
// ---------------------------------------------------------------------------

func TestRetryDelay_WithRetryAfter10(t *testing.T) {
	// retryAfter=10 → float64(10)*0.9*float64(time.Second) = 9s
	got := retryDelay(0, intPtr(10))
	want := 9 * time.Second
	if got != want {
		t.Errorf("retryDelay(0, ptr(10)) = %v, want %v", got, want)
	}
}

func TestRetryDelay_WithRetryAfter40(t *testing.T) {
	// retryAfter=40 → 36s, capped at 30s
	got := retryDelay(0, intPtr(40))
	want := retryCap
	if got != want {
		t.Errorf("retryDelay(0, ptr(40)) = %v, want %v (retryCap)", got, want)
	}
}

func TestRetryDelay_WithRetryAfter1(t *testing.T) {
	// retryAfter=1 → float64(1)*0.9*float64(time.Second) = 900_000_000ns = 900ms
	got := retryDelay(0, intPtr(1))
	want := 900 * time.Millisecond
	if got != want {
		t.Errorf("retryDelay(0, ptr(1)) = %v, want %v", got, want)
	}
}

func TestRetryDelay_Exponential_Attempt0(t *testing.T) {
	// attempt=0, no retryAfter → base = min(2^0 * s, 30s) = 1s
	// jitter factor in [0.75, 1.25] → result in [750ms, 1250ms]
	const runs = 500
	lo, hi := 750*time.Millisecond, 1250*time.Millisecond
	for i := 0; i < runs; i++ {
		got := retryDelay(0, nil)
		if got < lo || got > hi {
			t.Errorf("run %d: retryDelay(0, nil) = %v, want [%v, %v]", i, got, lo, hi)
			return
		}
	}
}

func TestRetryDelay_Exponential_Attempt1(t *testing.T) {
	// attempt=1 → base = min(2^1 * s, 30s) = 2s
	// jitter factor in [0.75, 1.25] → result in [1.5s, 2.5s]
	const runs = 500
	lo, hi := 1500*time.Millisecond, 2500*time.Millisecond
	for i := 0; i < runs; i++ {
		got := retryDelay(1, nil)
		if got < lo || got > hi {
			t.Errorf("run %d: retryDelay(1, nil) = %v, want [%v, %v]", i, got, lo, hi)
			return
		}
	}
}

func TestRetryDelay_Exponential_Attempt5(t *testing.T) {
	// attempt=5 → base = min(32s, 30s) = 30s
	// jitter [0.75, 1.25] → [22.5s, 37.5s], capped at 30s → [22.5s, 30s]
	const runs = 500
	lo := 22500 * time.Millisecond // 30s * 0.75
	for i := 0; i < runs; i++ {
		got := retryDelay(5, nil)
		if got < lo || got > retryCap {
			t.Errorf("run %d: retryDelay(5, nil) = %v, want [%v, %v]", i, got, lo, retryCap)
			return
		}
	}
}

func TestRetryDelay_ZeroRetryAfter(t *testing.T) {
	// retryAfter=0 → (*retryAfter > 0) is false → falls through to exponential
	const runs = 500
	lo, hi := 750*time.Millisecond, 1250*time.Millisecond
	for i := 0; i < runs; i++ {
		got := retryDelay(0, intPtr(0))
		if got < lo || got > hi {
			t.Errorf("run %d: retryDelay(0, ptr(0)) = %v, want [%v, %v]", i, got, lo, hi)
			return
		}
	}
}

// ---------------------------------------------------------------------------
// resolveActor tests
// ---------------------------------------------------------------------------

func TestResolveActor_UserIDSet(t *testing.T) {
	// userID non-nil takes precedence, forces actorType="user"
	gotID, gotType := resolveActor(strPtr("u1"), strPtr("a1"), strPtr("agent"))

	if gotID == nil {
		t.Fatal("expected non-nil actorID, got nil")
	}
	if *gotID != "u1" {
		t.Errorf("actorID = %q, want %q", *gotID, "u1")
	}
	if gotType == nil {
		t.Fatal("expected non-nil actorType, got nil")
	}
	if *gotType != "user" {
		t.Errorf("actorType = %q, want %q", *gotType, "user")
	}
}

func TestResolveActor_NoUserID(t *testing.T) {
	// userID nil → passthrough
	gotID, gotType := resolveActor(nil, strPtr("a1"), strPtr("agent"))

	if gotID == nil {
		t.Fatal("expected non-nil actorID, got nil")
	}
	if *gotID != "a1" {
		t.Errorf("actorID = %q, want %q", *gotID, "a1")
	}
	if gotType == nil {
		t.Fatal("expected non-nil actorType, got nil")
	}
	if *gotType != "agent" {
		t.Errorf("actorType = %q, want %q", *gotType, "agent")
	}
}

func TestResolveActor_AllNil(t *testing.T) {
	gotID, gotType := resolveActor(nil, nil, nil)

	if gotID != nil {
		t.Errorf("expected nil actorID, got %q", *gotID)
	}
	if gotType != nil {
		t.Errorf("expected nil actorType, got %q", *gotType)
	}
}

// ---------------------------------------------------------------------------
// Integration tests — httptest-based, exercise the full HTTP round-trip.
//
// The autogen appends operation paths to cfg.BaseURL (= httptest server URL):
//   POST   /api/v1/sdk/memories           → CreateMemory
//   POST   /api/v1/sdk/memories/search    → SearchMemories
//   GET    /api/v1/sdk/memories/{id}      → GetMemory
//   DELETE /api/v1/sdk/memories/{id}      → DeleteMemory
// ---------------------------------------------------------------------------

const (
	pathCreate = "/api/v1/sdk/memories"
	pathSearch = "/api/v1/sdk/memories/search"
)

// memIDFromPath extracts the memory ID from paths like /api/v1/sdk/memories/{id}.
func memIDFromPath(path string) string {
	return strings.TrimPrefix(path, pathCreate+"/")
}

// ---------------------------------------------------------------------------
// Add
// ---------------------------------------------------------------------------

func TestAdd_Success(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost || r.URL.Path != pathCreate {
			http.Error(w, "unexpected", http.StatusBadRequest)
			return
		}
		writeJSON(w, http.StatusOK, memJSON("mem-1", "hello world"))
	})
	client := newTestClient(t, handler, 1)

	mem, err := client.Add(context.Background(), "hello world", nil)
	if err != nil {
		t.Fatalf("Add: unexpected error: %v", err)
	}
	if mem == nil || mem.Id == nil || *mem.Id != "mem-1" {
		t.Errorf("Add: got id=%v, want mem-1", mem)
	}
}

func TestAdd_UserIDShorthand(t *testing.T) {
	// When UserID is set it must become actor_id and actor_type must be "user".
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		actorID, actorIDOK := body["actor_id"].(string)
		actorType, actorTypeOK := body["actor_type"].(string)
		if !actorIDOK || actorID != "user-42" {
			http.Error(w, "wrong actor_id", http.StatusBadRequest)
			return
		}
		if !actorTypeOK || actorType != "user" {
			http.Error(w, "wrong actor_type", http.StatusBadRequest)
			return
		}
		writeJSON(w, http.StatusOK, memJSON("mem-2", "content"))
	})
	client := newTestClient(t, handler, 1)

	_, err := client.Add(context.Background(), "content", &AddOptions{UserID: strPtr("user-42")})
	if err != nil {
		t.Fatalf("Add UserID shorthand: unexpected error: %v", err)
	}
}

func TestAdd_ExplicitActorIDAndType(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		var body map[string]interface{}
		if err := json.NewDecoder(r.Body).Decode(&body); err != nil {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
		actorID, actorIDOK := body["actor_id"].(string)
		actorType, actorTypeOK := body["actor_type"].(string)
		if !actorIDOK || actorID != "agent-7" || !actorTypeOK || actorType != "agent" {
			http.Error(w, "wrong actor fields", http.StatusBadRequest)
			return
		}
		writeJSON(w, http.StatusOK, memJSON("mem-3", "content"))
	})
	client := newTestClient(t, handler, 1)

	_, err := client.Add(context.Background(), "content", &AddOptions{
		ActorID:   strPtr("agent-7"),
		ActorType: strPtr("agent"),
	})
	if err != nil {
		t.Fatalf("Add explicit actor: unexpected error: %v", err)
	}
}

// ---------------------------------------------------------------------------
// Search
// ---------------------------------------------------------------------------

func TestSearch_Success(t *testing.T) {
	resp := `{"memories":[{"memory":{"id":"mem-1","content":"found"},"score":0.9}]}`
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodPost || r.URL.Path != pathSearch {
			http.Error(w, "unexpected", http.StatusBadRequest)
			return
		}
		writeJSON(w, http.StatusOK, []byte(resp))
	})
	client := newTestClient(t, handler, 1)

	results, err := client.Search(context.Background(), "found", nil)
	if err != nil {
		t.Fatalf("Search: unexpected error: %v", err)
	}
	if len(results) != 1 {
		t.Fatalf("Search: got %d results, want 1", len(results))
	}
	if results[0].Memory == nil || results[0].Memory.Id == nil || *results[0].Memory.Id != "mem-1" {
		t.Errorf("Search: got unexpected result %+v", results[0])
	}
}

func TestSearch_EmptyResult(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusOK, []byte(`{"memories":[]}`))
	})
	client := newTestClient(t, handler, 1)

	results, err := client.Search(context.Background(), "nothing", nil)
	if err != nil {
		t.Fatalf("Search empty: unexpected error: %v", err)
	}
	if results == nil {
		t.Error("Search empty: expected non-nil empty slice, got nil")
	}
	if len(results) != 0 {
		t.Errorf("Search empty: got %d results, want 0", len(results))
	}
}

// ---------------------------------------------------------------------------
// Get
// ---------------------------------------------------------------------------

func TestGet_Success(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodGet {
			http.Error(w, "unexpected method", http.StatusBadRequest)
			return
		}
		id := memIDFromPath(r.URL.Path)
		writeJSON(w, http.StatusOK, memJSON(id, "stored content"))
	})
	client := newTestClient(t, handler, 1)

	mem, err := client.Get(context.Background(), "mem-99")
	if err != nil {
		t.Fatalf("Get: unexpected error: %v", err)
	}
	if mem == nil || mem.Id == nil || *mem.Id != "mem-99" {
		t.Errorf("Get: got id=%v, want mem-99", mem)
	}
}

// ---------------------------------------------------------------------------
// Delete
// ---------------------------------------------------------------------------

func TestDelete_Success(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method != http.MethodDelete {
			http.Error(w, "unexpected method", http.StatusBadRequest)
			return
		}
		w.WriteHeader(http.StatusNoContent)
	})
	client := newTestClient(t, handler, 1)

	if err := client.Delete(context.Background(), "mem-99"); err != nil {
		t.Fatalf("Delete: unexpected error: %v", err)
	}
}

// ---------------------------------------------------------------------------
// Error mapping
// ---------------------------------------------------------------------------

func TestAdd_401_MapsToAuthError(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusUnauthorized, errJSON("invalid api key"))
	})
	client := newTestClient(t, handler, 1)

	_, err := client.Add(context.Background(), "x", nil)
	var authErr *SmriteaAuthError
	if !errors.As(err, &authErr) {
		t.Fatalf("expected SmriteaAuthError, got %T: %v", err, err)
	}
	if authErr.StatusCode != http.StatusUnauthorized {
		t.Errorf("StatusCode = %d, want 401", authErr.StatusCode)
	}
}

func TestSearch_404_MapsToNotFoundError(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusNotFound, errJSON("not found"))
	})
	client := newTestClient(t, handler, 1)

	_, err := client.Search(context.Background(), "x", nil)
	var nfErr *SmriteaNotFoundError
	if !errors.As(err, &nfErr) {
		t.Fatalf("expected SmriteaNotFoundError, got %T: %v", err, err)
	}
}

func TestAdd_400_MapsToValidationError(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusBadRequest, errJSON("content required"))
	})
	client := newTestClient(t, handler, 1)

	_, err := client.Add(context.Background(), "", nil)
	var valErr *SmriteaValidationError
	if !errors.As(err, &valErr) {
		t.Fatalf("expected SmriteaValidationError, got %T: %v", err, err)
	}
}

func TestAdd_402_MapsToQuotaError(t *testing.T) {
	handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		writeJSON(w, http.StatusPaymentRequired, errJSON("quota exceeded"))
	})
	client := newTestClient(t, handler, 1)

	_, err := client.Add(context.Background(), "x", nil)
	var quotaErr *SmriteaQuotaError
	if !errors.As(err, &quotaErr) {
		t.Fatalf("expected SmriteaQuotaError, got %T: %v", err, err)
	}
}

// ---------------------------------------------------------------------------
// Retry behaviour
// ---------------------------------------------------------------------------

func TestAdd_429_RetryThenSuccess(t *testing.T) {
	var callCount atomic.Int32
	handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		n := callCount.Add(1)
		if n == 1 {
			// First call: rate-limited.  Short Retry-After so the test doesn't
			// take too long (90% of 1 s = 900 ms).
			w.Header().Set("Retry-After", "1")
			writeJSON(w, http.StatusTooManyRequests, errJSON("rate limited"))
			return
		}
		writeJSON(w, http.StatusOK, memJSON("mem-ok", "ok"))
	})
	client := newTestClient(t, handler, 2)

	mem, err := client.Add(context.Background(), "x", nil)
	if err != nil {
		t.Fatalf("Add retry-then-success: unexpected error: %v", err)
	}
	if mem == nil || mem.Id == nil || *mem.Id != "mem-ok" {
		t.Errorf("Add retry-then-success: got unexpected memory %+v", mem)
	}
	if n := callCount.Load(); n != 2 {
		t.Errorf("expected 2 calls, got %d", n)
	}
}

func TestAdd_429_Exhausted_MapsToRateLimitError(t *testing.T) {
	var callCount atomic.Int32
	handler := http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		callCount.Add(1)
		w.Header().Set("Retry-After", "1")
		writeJSON(w, http.StatusTooManyRequests, errJSON("rate limited"))
	})
	// maxRetries=1 → 2 total attempts; both return 429 → SmriteaRateLimitError.
	client := newTestClient(t, handler, 1)

	_, err := client.Add(context.Background(), "x", nil)
	var rlErr *SmriteaRateLimitError
	if !errors.As(err, &rlErr) {
		t.Fatalf("expected SmriteaRateLimitError, got %T: %v", err, err)
	}
	if rlErr.RetryAfter == nil || *rlErr.RetryAfter != 1 {
		t.Errorf("RetryAfter = %v, want ptr(1)", rlErr.RetryAfter)
	}
	if n := callCount.Load(); n != 2 {
		t.Errorf("expected 2 calls, got %d", n)
	}
}

func TestWithRetry_ContextCanceledDuringRetrySleep(t *testing.T) {
	// Tests withRetry directly (no HTTP) so there is no race between the HTTP
	// body read and context cancellation.
	// fn() returns SmriteaRateLimitError immediately; withRetry enters the
	// retry select (27 s sleep); we cancel — expect context.Canceled unwrapped.
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	retryAfter := 30
	firstCallDone := make(chan struct{})
	var signaled atomic.Bool

	fn := func() (*Memory, error) {
		if signaled.CompareAndSwap(false, true) {
			close(firstCallDone)
		}
		return nil, &SmriteaRateLimitError{
			SmriteaError: SmriteaError{Message: "rate limited", StatusCode: 429},
			RetryAfter:   &retryAfter,
		}
	}

	errCh := make(chan error, 1)
	go func() {
		_, callErr := withRetry[*Memory](ctx, 2, fn)
		errCh <- callErr
	}()

	// fn returned and withRetry is about to enter the retry select.
	// Cancelling now ensures ctx.Done() is ready when the select evaluates.
	<-firstCallDone
	cancel()

	select {
	case err := <-errCh:
		if !errors.Is(err, context.Canceled) {
			t.Errorf("expected context.Canceled, got %T: %v", err, err)
		}
	case <-time.After(5 * time.Second):
		t.Fatal("timed out waiting for context cancellation to propagate")
	}
}

// ---------------------------------------------------------------------------
// GetAll — not-yet-implemented stub
// ---------------------------------------------------------------------------

func TestGetAll_NotImplemented(t *testing.T) {
	client := NewClient(ClientConfig{APIKey: "k", AppID: "a"})
	_, err := client.GetAll(context.Background())
	if err == nil {
		t.Fatal("expected error from GetAll, got nil")
	}
	if !strings.Contains(err.Error(), "not yet available") {
		t.Errorf("unexpected error message: %v", err)
	}
}
