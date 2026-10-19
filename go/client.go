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

const msgInvalidTraceID = "trace_id must be 8-100 characters of A-Z, a-z, 0-9, dot, underscore, colon or hyphen"

// SmriteaClient is the entry point for all smritea memory operations.
// Create one instance per application and reuse it across calls.
// All methods are safe for concurrent use.
type SmriteaClient struct {
	appID      string
	maxRetries int
	api        *autogen.SDKMemoryAPIService
	cfg        *autogen.Configuration
	// initErr is set by NewClient when ClientConfig.TraceID is invalid. It is a
	// *SmriteaBadRequestError with HTTP status 400. Every method returns it before
	// making any network call.
	initErr error
}

// NewClient constructs a SmriteaClient from the provided ClientConfig.
// If cfg.BaseURL is empty, it defaults to "https://api-us.smritea.ai".
// If cfg.MaxRetries is nil, it defaults to 2. Otherwise *cfg.MaxRetries is used, and a
// negative value is treated as 0 (no retry).
// If cfg.TraceID is set and valid, it is sent as the X-Trace-ID header on every call.
// If cfg.TraceID is set but invalid, NewClient still returns a client, and every method
// returns a *SmriteaBadRequestError (HTTP 400) with Code ErrCodeInvalidTraceID without
// sending a request.
func NewClient(cfg ClientConfig) *SmriteaClient {
	baseURL := cfg.BaseURL
	if baseURL == "" {
		baseURL = "https://api-us.smritea.ai"
	}

	maxRetries := 2
	if cfg.MaxRetries != nil {
		maxRetries = *cfg.MaxRetries
	}
	if maxRetries < 0 {
		maxRetries = 0
	}

	configuration := autogen.NewConfiguration()
	// configuration.Host is a no-op in the autogen Go client; base URL is
	// resolved via Servers[0].URL. The autogen appends operation paths (e.g.
	// "/api/v1/sdk/memories") to this value at request time.
	configuration.Servers = autogen.ServerConfigurations{{URL: baseURL}}
	configuration.AddDefaultHeader("X-API-Key", cfg.APIKey)

	var initErr error
	if cfg.TraceID != "" {
		if isValidTraceID(cfg.TraceID) {
			configuration.AddDefaultHeader("X-Trace-ID", cfg.TraceID)
		} else {
			initErr = &SmriteaBadRequestError{SmriteaError: SmriteaError{
				Message:    msgInvalidTraceID,
				Code:       ErrCodeInvalidTraceID,
				HTTPStatus: http.StatusBadRequest,
			}}
		}
	}

	apiClient := autogen.NewAPIClient(configuration)

	return &SmriteaClient{
		appID:      cfg.AppID,
		maxRetries: maxRetries,
		api:        apiClient.SDKMemoryAPI,
		cfg:        configuration,
		initErr:    initErr,
	}
}

// isValidTraceID reports whether id is 8-100 bytes long and every byte is one of
// A-Z, a-z, 0-9, '.', '_', ':' or '-'.
func isValidTraceID(id string) bool {
	if len(id) < 8 || len(id) > 100 {
		return false
	}
	for i := range len(id) {
		b := id[i]
		isAlnum := (b >= 'A' && b <= 'Z') || (b >= 'a' && b <= 'z') || (b >= '0' && b <= '9')
		if !isAlnum && b != '.' && b != '_' && b != ':' && b != '-' {
			return false
		}
	}
	return true
}

// Add stores a new memory with the given content. The optional AddOptions
// control which actor the memory is attributed to, metadata, and conversation
// scoping. Returns a MemoryCreationResult containing all memories created from
// the extracted facts, plus metadata (FactsExtracted, SkippedCount, UpdatedCount).
func (c *SmriteaClient) Add(ctx context.Context, content string, opts *AddOptions) (*MemoryCreationResult, error) {
	if c.initErr != nil {
		return nil, c.initErr
	}

	req := autogen.CreateMemoryRequest{
		Content: content,
		Scope:   autogen.MemoryScope{},
	}

	if opts != nil {
		if opts.Scope != nil {
			req.Scope = autogen.MemoryScope{
				ActorId:        opts.Scope.ActorID,
				ActorType:      opts.Scope.ActorType,
				ActorName:      opts.Scope.ActorName,
				ConversationId: opts.Scope.ConversationID,
				SourceType:     opts.Scope.SourceType,
				ParticipantIds: opts.Scope.ParticipantIDs,
			}
		}
		if opts.Metadata != nil {
			req.Metadata = opts.Metadata
		}
		req.EventOccurredAt = opts.EventOccurredAt
		if opts.RelativeStanding != nil {
			req.RelativeStanding = &autogen.RelativeStandingConfig{
				Importance:    opts.RelativeStanding.Importance,
				DecayFactor:   opts.RelativeStanding.DecayFactor,
				DecayFunction: opts.RelativeStanding.DecayFunction,
			}
		}
	}

	return withRetry[*MemoryCreationResult](ctx, c.maxRetries, func() (*MemoryCreationResult, error) {
		result, httpResp, err := c.api.CreateMemory(ctx).XAppID(c.appID).Request(req).Execute()
		if wErr := wrapHTTPError(httpResp, err); wErr != nil {
			return nil, wErr
		}
		return result, nil
	})
}

// Search retrieves memories ranked by relevance to the given query.
// The optional SearchOptions control actor scoping, result count,
// graph traversal depth, and conversation scope.
// Returns an empty (non-nil) slice when no memories match.
func (c *SmriteaClient) Search(ctx context.Context, query string, opts *SearchOptions) ([]*SearchResult, error) {
	if c.initErr != nil {
		return nil, c.initErr
	}

	// Query is a non-pointer required field in SearchMemoryRequest.
	req := autogen.SearchMemoryRequest{
		Query: query,
	}

	if opts != nil {
		if opts.Scope != nil {
			req.Scope = &autogen.MemoryScope{
				ActorId:        opts.Scope.ActorID,
				ActorType:      opts.Scope.ActorType,
				ConversationId: opts.Scope.ConversationID,
				ParticipantIds: opts.Scope.ParticipantIDs,
			}
		}
		// SearchOptions uses *int32 to match the autogen types directly.
		req.Limit = opts.Limit
		req.GraphDepth = opts.GraphDepth
		req.FromTime = opts.FromTime
		req.ToTime = opts.ToTime
		req.ValidAt = opts.ValidAt
		if opts.Method != nil {
			method := autogen.SearchMethod(*opts.Method)
			req.Method = &method
		}
		if opts.RerankerType != nil {
			rt := autogen.RerankerType(*opts.RerankerType)
			req.RerankerType = &rt
		}
		if opts.MetadataFilter != nil {
			req.MetadataFilter = opts.MetadataFilter
		}
		req.SpeakerActorId = opts.SpeakerActorID
	}

	return withRetry[[]*SearchResult](ctx, c.maxRetries, func() ([]*SearchResult, error) {
		// Method is SearchMemories (plural), not SearchMemory.
		respBody, httpResp, err := c.api.SearchMemories(ctx).XAppID(c.appID).Request(req).Execute()
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
	if c.initErr != nil {
		return nil, c.initErr
	}

	return withRetry[*Memory](ctx, c.maxRetries, func() (*Memory, error) {
		result, httpResp, err := c.api.GetMemory(ctx, memoryID).XAppID(c.appID).Execute()
		if wErr := wrapHTTPError(httpResp, err); wErr != nil {
			return nil, wErr
		}
		return result, nil
	})
}

// Delete permanently removes a memory by its ID. Returns SmriteaNotFoundError
// when the memory does not exist.
func (c *SmriteaClient) Delete(ctx context.Context, memoryID string) error {
	if c.initErr != nil {
		return c.initErr
	}

	_, err := withRetry[struct{}](ctx, c.maxRetries, func() (struct{}, error) {
		httpResp, err := c.api.DeleteMemory(ctx, memoryID).XAppID(c.appID).Execute()
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
	if c.initErr != nil {
		return nil, c.initErr
	}

	return nil, errors.New("smritea: GetAll() is not yet available. " +
		"The list memories endpoint is pending server-side implementation")
}

// withRetry executes fn up to maxRetries+1 times, retrying only on
// SmriteaTooManyRequestsError. Other errors are returned immediately. After all
// retries are exhausted, the last SmriteaTooManyRequestsError is returned.
func withRetry[T any](ctx context.Context, maxRetries int, fn func() (T, error)) (T, error) {
	for attempt := 0; attempt <= maxRetries; attempt++ {
		result, err := fn()
		if err == nil {
			return result, nil
		}

		var rateErr *SmriteaTooManyRequestsError
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

	// Unreachable for maxRetries >= 0: the loop always returns. NewClient clamps negative values to 0.
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
