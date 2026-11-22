package smritea

import (
	"bufio"
	"context"
	"errors"
	"fmt"
	"net"
	"net/http"
	"net/http/httptest"
	"strings"
	"sync"
	"sync/atomic"
	"testing"
	"time"
)

// stallMode selects how the stall server misbehaves after it receives a request.
type stallMode int

const (
	// stallNoAnswer accepts the connection, reads the request and never writes a byte.
	stallNoAnswer stallMode = iota
	// stallBody writes a 200 status line, headers announcing 100 body bytes, then 10 body
	// bytes, and stalls. The client has the headers but can never read the full body.
	stallBody
)

const (
	// shortTimeout is the request timeout used to exercise the default client quickly.
	shortTimeout = 200 * time.Millisecond
	// timeoutTolerance is the slack allowed on top of a timeout before a call counts as late.
	timeoutTolerance = time.Second
)

// startStallServer starts a loopback TCP server that answers every request in the given
// mode. It returns the base URL. The server and every open connection close on t.Cleanup.
func startStallServer(t *testing.T, mode stallMode) string {
	t.Helper()

	ln, err := net.Listen("tcp", "127.0.0.1:0")
	if err != nil {
		t.Fatalf("listen: %v", err)
	}

	var (
		mu    sync.Mutex
		conns []net.Conn
		wg    sync.WaitGroup
	)
	wg.Add(1)
	go func() {
		defer wg.Done()
		for {
			conn, acceptErr := ln.Accept()
			if acceptErr != nil {
				return
			}
			mu.Lock()
			conns = append(conns, conn)
			mu.Unlock()
			wg.Add(1)
			go func() {
				defer wg.Done()
				serveStall(conn, mode)
			}()
		}
	}()

	t.Cleanup(func() {
		ln.Close()
		mu.Lock()
		for i := range conns {
			conns[i].Close()
		}
		mu.Unlock()
		wg.Wait()
	})

	return "http://" + ln.Addr().String()
}

// serveStall reads one request from conn, then misbehaves per mode until the peer or the
// server closes the connection.
func serveStall(conn net.Conn, mode stallMode) {
	reader := bufio.NewReader(conn)
	req, err := http.ReadRequest(reader)
	if err != nil {
		return
	}
	req.Body.Close()

	if mode == stallBody {
		const partial = "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 100\r\n\r\n0123456789"
		if _, err := conn.Write([]byte(partial)); err != nil {
			return
		}
	}

	// Block until the connection is closed by the client timeout or by test cleanup.
	buf := make([]byte, 1)
	for {
		if _, err := reader.Read(buf); err != nil {
			return
		}
	}
}

func TestDefaultTimeout_Value(t *testing.T) {
	if DefaultTimeout != 10*time.Second {
		t.Errorf("DefaultTimeout = %v, want 10s", DefaultTimeout)
	}
}

func TestNewDefaultHTTPClient(t *testing.T) {
	tests := []struct {
		name    string
		timeout time.Duration
	}{
		{name: "short timeout", timeout: shortTimeout},
		{name: "default timeout", timeout: DefaultTimeout},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			got := newDefaultHTTPClient(tc.timeout)
			if got == nil {
				t.Fatal("newDefaultHTTPClient returned nil")
			}
			if got.Timeout != tc.timeout {
				t.Errorf("Timeout = %v, want %v", got.Timeout, tc.timeout)
			}
			if got == http.DefaultClient {
				t.Error("newDefaultHTTPClient returned http.DefaultClient; it must return a new client")
			}
			if got.Transport != nil {
				t.Errorf("Transport = %v, want nil (use http.DefaultTransport unchanged)", got.Transport)
			}
		})
	}

	first := newDefaultHTTPClient(shortTimeout)
	second := newDefaultHTTPClient(shortTimeout)
	if first == second {
		t.Error("each call must return a distinct client")
	}
	if http.DefaultClient.Timeout != 0 {
		t.Errorf("http.DefaultClient.Timeout = %v, want 0 (must not be mutated)", http.DefaultClient.Timeout)
	}
}

// TestNewClient_HTTPClientSelection verifies which *http.Client NewClient wires into the
// generated client, and that it never alters a caller-supplied client.
func TestNewClient_HTTPClientSelection(t *testing.T) {
	custom := &http.Client{}
	customWithTimeout := &http.Client{Timeout: 3 * time.Second}

	tests := []struct {
		name        string
		supplied    *http.Client
		wantSame    bool
		wantTimeout time.Duration
	}{
		{name: "nil gets a default client with DefaultTimeout", supplied: nil, wantSame: false, wantTimeout: DefaultTimeout},
		{name: "supplied client without timeout is kept and gets none", supplied: custom, wantSame: true, wantTimeout: 0},
		{name: "supplied client with own timeout is kept unchanged", supplied: customWithTimeout, wantSame: true, wantTimeout: 3 * time.Second},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			client := NewClient(ClientConfig{APIKey: "k", AppID: "a", HTTPClient: tc.supplied})

			got := client.cfg.HTTPClient
			if got == nil {
				t.Fatal("cfg.HTTPClient is nil")
			}
			if tc.wantSame && got != tc.supplied {
				t.Errorf("cfg.HTTPClient = %p, want the supplied client %p", got, tc.supplied)
			}
			if !tc.wantSame && got == http.DefaultClient {
				t.Error("cfg.HTTPClient is http.DefaultClient, which has no timeout")
			}
			if got.Timeout != tc.wantTimeout {
				t.Errorf("cfg.HTTPClient.Timeout = %v, want %v", got.Timeout, tc.wantTimeout)
			}
			if tc.supplied != nil && tc.supplied.Timeout != tc.wantTimeout {
				t.Errorf("supplied client Timeout changed to %v, want %v", tc.supplied.Timeout, tc.wantTimeout)
			}
		})
	}
}

// TestDefaultHTTPClient_TimesOut drives every public method against a server that never
// answers, and against a server that sends headers and then stalls the body. The default
// client with a short timeout must fail each call within the timeout plus tolerance.
func TestDefaultHTTPClient_TimesOut(t *testing.T) {
	methods := []struct {
		name string
		call func(c *SmriteaClient) error
	}{
		{name: "Add", call: func(c *SmriteaClient) error {
			_, err := c.Add(context.Background(), "x", nil)
			return err
		}},
		{name: "Search", call: func(c *SmriteaClient) error {
			_, err := c.Search(context.Background(), "q", nil)
			return err
		}},
		{name: "Get", call: func(c *SmriteaClient) error {
			_, err := c.Get(context.Background(), "mem_1")
			return err
		}},
		{name: "Delete", call: func(c *SmriteaClient) error {
			return c.Delete(context.Background(), "mem_1")
		}},
	}
	modes := []struct {
		name string
		mode stallMode
	}{
		{name: "server never answers", mode: stallNoAnswer},
		{name: "headers then stalled body", mode: stallBody},
	}

	for _, m := range modes {
		for _, method := range methods {
			t.Run(fmt.Sprintf("%s/%s", m.name, method.name), func(t *testing.T) {
				baseURL := startStallServer(t, m.mode)
				client := NewClient(ClientConfig{
					APIKey:     "test-key",
					AppID:      "app-test",
					BaseURL:    baseURL,
					MaxRetries: Int(0),
					HTTPClient: newDefaultHTTPClient(shortTimeout),
				})

				start := time.Now()
				err := method.call(client)
				elapsed := time.Since(start)

				if err == nil {
					t.Fatal("expected an error from a stalled server, got nil")
				}
				var smErr *SmriteaError
				if !errors.As(err, &smErr) {
					t.Fatalf("expected *SmriteaError, got %T: %v", err, err)
				}
				if elapsed < shortTimeout*9/10 {
					t.Errorf("call failed after %v, before the %v timeout", elapsed, shortTimeout)
				}
				if elapsed > shortTimeout+timeoutTolerance {
					t.Errorf("call took %v, want at most %v", elapsed, shortTimeout+timeoutTolerance)
				}
			})
		}
	}
}

// TestDefaultHTTPClient_NoAnswerMessage verifies the caller sees the client timeout reason
// when the server never answers.
func TestDefaultHTTPClient_NoAnswerMessage(t *testing.T) {
	baseURL := startStallServer(t, stallNoAnswer)
	client := NewClient(ClientConfig{
		APIKey:     "test-key",
		AppID:      "app-test",
		BaseURL:    baseURL,
		MaxRetries: Int(0),
		HTTPClient: newDefaultHTTPClient(shortTimeout),
	})

	_, err := client.Get(context.Background(), "mem_1")

	var smErr *SmriteaError
	if !errors.As(err, &smErr) {
		t.Fatalf("expected *SmriteaError, got %T: %v", err, err)
	}
	const want = "Client.Timeout exceeded"
	if !strings.Contains(smErr.Message, want) {
		t.Errorf("Message = %q, want it to contain %q", smErr.Message, want)
	}
}

// TestRetry_GetsFreshTimeoutBudget verifies a 429 retry starts a new exchange with its own
// timeout budget and that the retry sleep is not counted. Each exchange takes 300 ms, the
// retry sleep is 900 ms (90 % of Retry-After 1), and the timeout is 600 ms: the whole call
// lasts far longer than one budget but every exchange fits in its own.
func TestRetry_GetsFreshTimeoutBudget(t *testing.T) {
	const (
		exchangeDelay = 300 * time.Millisecond
		budget        = 600 * time.Millisecond
	)

	tests := []struct {
		name         string
		maxRetries   int
		answers      []int
		wantAttempts int32
	}{
		{name: "429 then 200 succeeds", maxRetries: 2, answers: []int{http.StatusTooManyRequests, http.StatusOK}, wantAttempts: 2},
		{name: "429 twice then 200 succeeds", maxRetries: 2, answers: []int{http.StatusTooManyRequests, http.StatusTooManyRequests, http.StatusOK}, wantAttempts: 3},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			var attempts atomic.Int32
			srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
				n := int(attempts.Add(1))
				time.Sleep(exchangeDelay)
				status := tc.answers[n-1]
				if status == http.StatusTooManyRequests {
					w.Header().Set("Retry-After", "1")
					writeJSON(w, status, errJSON("rate limited"))
					return
				}
				writeJSON(w, status, memSingleJSON("mem_1", "content"))
			}))
			t.Cleanup(srv.Close)

			client := NewClient(ClientConfig{
				APIKey:     "test-key",
				AppID:      "app-test",
				BaseURL:    srv.URL,
				MaxRetries: Int(tc.maxRetries),
				HTTPClient: newDefaultHTTPClient(budget),
			})

			start := time.Now()
			mem, err := client.Get(context.Background(), "mem_1")
			elapsed := time.Since(start)

			if err != nil {
				t.Fatalf("Get: %v", err)
			}
			if mem.GetId() != "mem_1" {
				t.Errorf("memory id = %q, want mem_1", mem.GetId())
			}
			if got := attempts.Load(); got != tc.wantAttempts {
				t.Errorf("server received %d attempts, want %d", got, tc.wantAttempts)
			}
			if elapsed <= budget {
				t.Errorf("call lasted %v, want more than one budget (%v) to prove the budget is per exchange", elapsed, budget)
			}
		})
	}
}

// TestCustomHTTPClient_NoTimeoutAdded verifies a caller-supplied client without a Timeout
// is used as given: it sends the request and is neither replaced, wrapped nor given a timeout.
func TestCustomHTTPClient_NoTimeoutAdded(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		time.Sleep(shortTimeout)
		writeJSON(w, http.StatusOK, memSingleJSON("mem_1", "content"))
	}))
	t.Cleanup(srv.Close)

	transport := &countingTransport{}
	supplied := &http.Client{Transport: transport}
	client := NewClient(ClientConfig{
		APIKey:     "test-key",
		AppID:      "app-test",
		BaseURL:    srv.URL,
		MaxRetries: Int(0),
		HTTPClient: supplied,
	})

	if _, err := client.Get(context.Background(), "mem_1"); err != nil {
		t.Fatalf("Get: %v", err)
	}

	if client.cfg.HTTPClient != supplied {
		t.Errorf("cfg.HTTPClient = %p, want the supplied client %p", client.cfg.HTTPClient, supplied)
	}
	if supplied.Timeout != 0 {
		t.Errorf("supplied client Timeout = %v, want 0 (the SDK must add none)", supplied.Timeout)
	}
	if c := transport.count.Load(); c != 1 {
		t.Errorf("supplied transport saw %d requests, want 1", c)
	}
}

// TestContext_BoundsCallOnStalledServer verifies the ctx argument still bounds a call when
// the HTTP timeout is far longer than the context deadline, and that a cancelled context
// stops the call.
func TestContext_BoundsCallOnStalledServer(t *testing.T) {
	tests := []struct {
		name   string
		mode   stallMode
		cancel func() (context.Context, context.CancelFunc)
		bound  time.Duration
	}{
		{
			name: "deadline while server never answers",
			mode: stallNoAnswer,
			cancel: func() (context.Context, context.CancelFunc) {
				return context.WithTimeout(context.Background(), shortTimeout)
			},
			bound: shortTimeout,
		},
		{
			name: "deadline while body is stalled",
			mode: stallBody,
			cancel: func() (context.Context, context.CancelFunc) {
				return context.WithTimeout(context.Background(), shortTimeout)
			},
			bound: shortTimeout,
		},
		{
			name: "cancel while server never answers",
			mode: stallNoAnswer,
			cancel: func() (context.Context, context.CancelFunc) {
				ctx, cancel := context.WithCancel(context.Background())
				time.AfterFunc(shortTimeout, cancel)
				return ctx, cancel
			},
			bound: shortTimeout,
		},
	}

	for _, tc := range tests {
		t.Run(tc.name, func(t *testing.T) {
			baseURL := startStallServer(t, tc.mode)
			client := NewClient(ClientConfig{
				APIKey:     "test-key",
				AppID:      "app-test",
				BaseURL:    baseURL,
				MaxRetries: Int(0),
				// The default 10 s budget is far longer than the context bound under test.
				HTTPClient: newDefaultHTTPClient(DefaultTimeout),
			})
			ctx, cancel := tc.cancel()
			defer cancel()

			start := time.Now()
			_, err := client.Get(ctx, "mem_1")
			elapsed := time.Since(start)

			if err == nil {
				t.Fatal("expected an error, got nil")
			}
			if elapsed > tc.bound+timeoutTolerance {
				t.Errorf("call took %v, want at most %v", elapsed, tc.bound+timeoutTolerance)
			}
		})
	}
}
