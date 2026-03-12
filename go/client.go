package smritea

import (
	"context"
	"errors"
	"fmt"
	"io"
	"math"
	"math/rand"
	"net/http"
	"time"

	"github.com/SmrutAI/smritea-sdk/go/internal/autogen"
)

const retryCap = 30 * time.Second

// SmriteaClient is the entry point for all smritea memory operations.
// Create one instance per application and reuse it across calls.
// All methods are safe for concurrent use.
type SmriteaClient struct {
	appID      string
	maxRetries int
	api        *autogen.SDKMemoryAPIService
	cfg        *autogen.Configuration
}

// NewClient constructs a SmriteaClient from the provided ClientConfig.
// If cfg.BaseURL is empty, it defaults to "https://api.smritea.ai".
// If cfg.MaxRetries is 0, it defaults to 2.
func NewClient(cfg ClientConfig) *SmriteaClient {
	baseURL := cfg.BaseURL
	if baseURL == "" {
		baseURL = "https://api.smritea.ai"
	}

	maxRetries := cfg.MaxRetries
	if maxRetries == 0 {
		maxRetries = 2
	}

	configuration := autogen.NewConfiguration()
	// configuration.Host is a no-op in the autogen Go client; base URL is
	// resolved via Servers[0].URL. The autogen appends operation paths (e.g.
	// "/api/v1/sdk/memories") to this value at request time.
	configuration.Servers = autogen.ServerConfigurations{{URL: baseURL}}
	configuration.AddDefaultHeader("X-API-Key", cfg.APIKey)

	apiClient := autogen.NewAPIClient(configuration)

	return &SmriteaClient{
		appID:      cfg.AppID,
		maxRetries: maxRetries,
		api:        apiClient.SDKMemoryAPI,
		cfg:        configuration,
	}
}

// Add stores a new memory with the given content. The optional AddOptions
// control which actor the memory is attributed to, metadata, and conversation
// scoping. When opts.UserID is set it takes precedence over opts.ActorID and
// forces actor_type="user".
func (c *SmriteaClient) Add(ctx context.Context, content string, opts *AddOptions) (*Memory, error) {
	req := autogen.MemoryCreateMemoryRequest{
		AppId:   &c.appID,
		Content: &content,
	}

	if opts != nil {
		actorID, actorType := resolveActor(opts.UserID, opts.ActorID, opts.ActorType)
		req.ActorId = actorID
		req.ActorType = actorType
		req.ActorName = opts.ActorName
		req.ConversationId = opts.ConversationID
		if opts.Metadata != nil {
			req.Metadata = opts.Metadata
		}
	}

	return withRetry[*Memory](ctx, c.maxRetries, func() (*Memory, error) {
		result, httpResp, err := c.api.CreateMemory(ctx).Request(req).Execute()
		if wErr := wrapHTTPError(httpResp, err); wErr != nil {
			return nil, wErr
		}
		return result, nil
	})
}

// Search retrieves memories ranked by relevance to the given query.
// The optional SearchOptions control actor scoping, result count, search
// method, similarity threshold, graph traversal depth, and conversation scope.
// Returns an empty (non-nil) slice when no memories match.
func (c *SmriteaClient) Search(ctx context.Context, query string, opts *SearchOptions) ([]*SearchResult, error) {
	// AppId and Query are non-pointer required fields in MemorySearchMemoryRequest
	// (unlike MemoryCreateMemoryRequest where they are *string).
	req := autogen.MemorySearchMemoryRequest{
		AppId: c.appID,
		Query: query,
	}

	if opts != nil {
		actorID, actorType := resolveActor(opts.UserID, opts.ActorID, opts.ActorType)
		req.ActorId = actorID
		req.ActorType = actorType
		// SearchOptions uses *int32 to match the autogen types directly.
		req.Limit = opts.Limit
		if opts.Method != nil {
			m := autogen.ModelEnumsSearchMethod(*opts.Method)
			req.Method = &m
		}
		req.Threshold = opts.Threshold
		req.GraphDepth = opts.GraphDepth
		req.ConversationId = opts.ConversationID
		req.FromTime = opts.FromTime
		req.ToTime = opts.ToTime
		req.ValidAt = opts.ValidAt
	}

	return withRetry[[]*SearchResult](ctx, c.maxRetries, func() ([]*SearchResult, error) {
		// Method is SearchMemories (plural), not SearchMemory.
		respBody, httpResp, err := c.api.SearchMemories(ctx).Request(req).Execute()
		if wErr := wrapHTTPError(httpResp, err); wErr != nil {
			return nil, wErr
		}
		// GetMemories() returns []SearchResult (value slice); convert to
		// []*SearchResult so callers can safely retain individual pointers.
		memories := respBody.GetMemories()
		results := make([]*SearchResult, len(memories))
		for i := range memories {
			m := memories[i]
			results[i] = &m
		}
		return results, nil
	})
}

// Get fetches a single memory by its ID. Returns SmriteaNotFoundError when
// the memory does not exist.
func (c *SmriteaClient) Get(ctx context.Context, memoryID string) (*Memory, error) {
	return withRetry[*Memory](ctx, c.maxRetries, func() (*Memory, error) {
		result, httpResp, err := c.api.GetMemory(ctx, memoryID).Execute()
		if wErr := wrapHTTPError(httpResp, err); wErr != nil {
			return nil, wErr
		}
		return result, nil
	})
}

// Delete permanently removes a memory by its ID. Returns SmriteaNotFoundError
// when the memory does not exist.
func (c *SmriteaClient) Delete(ctx context.Context, memoryID string) error {
	_, err := withRetry[struct{}](ctx, c.maxRetries, func() (struct{}, error) {
		httpResp, err := c.api.DeleteMemory(ctx, memoryID).Execute()
		if wErr := wrapHTTPError(httpResp, err); wErr != nil {
			return struct{}{}, wErr
		}
		return struct{}{}, nil
	})
	return err
}

// GetAll is not yet implemented. The list memories endpoint is pending server-side
// implementation. This method is provided for forward compatibility.
func (c *SmriteaClient) GetAll(_ context.Context) ([]*Memory, error) {
	return nil, errors.New("smritea: GetAll() is not yet available. " +
		"The list memories endpoint is pending server-side implementation")
}

// resolveActor applies the user_id convenience shorthand. When userID is
// non-nil it is used as the actor ID and the type is forced to "user".
// Otherwise actorID and actorType are returned unchanged.
func resolveActor(userID, actorID, actorType *string) (*string, *string) {
	if userID != nil {
		t := "user"
		return userID, &t
	}
	return actorID, actorType
}

// withRetry executes fn up to maxRetries+1 times, retrying only on
// SmriteaRateLimitError. Other errors are returned immediately. After all
// retries are exhausted, a SmriteaError is returned.
func withRetry[T any](ctx context.Context, maxRetries int, fn func() (T, error)) (T, error) {
	for attempt := 0; attempt <= maxRetries; attempt++ {
		result, err := fn()
		if err == nil {
			return result, nil
		}

		var rateErr *SmriteaRateLimitError
		if errors.As(err, &rateErr) && attempt < maxRetries {
			delay := retryDelay(attempt, rateErr.RetryAfter)
			select {
			case <-ctx.Done():
				var zero T
				// Wrap with %w so wrapcheck is satisfied while preserving the
				// error chain — errors.Is(err, context.Canceled) still works.
				return zero, fmt.Errorf("%w", ctx.Err())
			case <-time.After(delay):
				continue
			}
		}

		var zero T
		return zero, err
	}

	var zero T
	return zero, &SmriteaError{Message: "max retries exceeded"}
}

// retryDelay computes how long to sleep before the next retry attempt.
// When retryAfter is provided by the server, 90 % of that value is used (to
// account for clock skew) capped at retryCap. Otherwise exponential backoff
// with ±25 % jitter is applied, also capped at retryCap.
func retryDelay(attempt int, retryAfter *int) time.Duration {
	if retryAfter != nil && *retryAfter > 0 {
		// Compute in nanoseconds first to avoid float64→Duration truncation
		// (time.Duration(0.9) truncates to 0 nanoseconds).
		advised := time.Duration(float64(*retryAfter) * 0.9 * float64(time.Second))
		return min(advised, retryCap)
	}

	base := min(
		time.Duration(math.Pow(2, float64(attempt)))*time.Second,
		retryCap,
	)
	// Apply ±25 % jitter: multiply base by a factor in [0.75, 1.25].
	//nolint:gosec // math/rand is intentional here; jitter does not require cryptographic randomness.
	jitter := time.Duration(float64(base) * (0.75 + 0.5*rand.Float64()))
	return min(jitter, retryCap)
}

// wrapHTTPError translates a raw HTTP error into a typed SmriteaError.
// When resp is available the body is read and mapError is used to produce
// the most specific error subtype. Without a response a generic SmriteaError
// is returned. Returns nil when err is nil.
func wrapHTTPError(resp *http.Response, err error) error {
	if err == nil {
		return nil
	}
	if resp != nil {
		defer resp.Body.Close()
		body, readErr := io.ReadAll(resp.Body)
		if readErr != nil {
			// Body unreadable; still map by status code with an empty body.
			body = []byte{}
		}
		return mapError(resp, body)
	}
	return &SmriteaError{Message: err.Error()}
}
