package smritea

import "github.com/SmrutAI/smritea-sdk/go/internal/autogen"

// Memory is the canonical return type for a stored memory.
type Memory = autogen.MemoryMemoryResponse

// SearchResult is a single ranked search result.
type SearchResult = autogen.MemorySearchMemoryResponse

// SearchMemoriesResponse wraps the list returned by the search endpoint.
type SearchMemoriesResponse = autogen.MemorySearchMemoriesResponse

// ClientConfig holds constructor options for SmriteaClient.
type ClientConfig struct {
	APIKey     string
	AppID      string
	BaseURL    string // default: https://api.smritea.ai
	MaxRetries int    // default: 2; 0 disables retry
}

// Scope groups actor and conversation context fields for memory operations.
type Scope struct {
	ActorID               *string
	ActorType             *string
	ActorName             *string
	ConversationID        *string
	ConversationMessageID *string
	SourceType            *string
}

// NewScope returns a new empty Scope ready for fluent configuration.
func NewScope() *Scope { return &Scope{} }

// WithActorID sets ActorID.
func (s *Scope) WithActorID(id string) *Scope { s.ActorID = &id; return s }

// WithActorType sets ActorType.
func (s *Scope) WithActorType(t string) *Scope { s.ActorType = &t; return s }

// WithActorName sets ActorName.
func (s *Scope) WithActorName(name string) *Scope { s.ActorName = &name; return s }

// WithConversationID sets ConversationID.
func (s *Scope) WithConversationID(id string) *Scope { s.ConversationID = &id; return s }

// WithConversationMessageID sets ConversationMessageID.
func (s *Scope) WithConversationMessageID(id string) *Scope { s.ConversationMessageID = &id; return s }

// WithSourceType sets SourceType.
func (s *Scope) WithSourceType(t string) *Scope { s.SourceType = &t; return s }

// AddOptions are the optional parameters for Client.Add.
//
// Use NewAddOptions() with fluent With* methods for ergonomic construction:
//
//	opts := smritea.NewAddOptions().WithScope(smritea.NewScope().WithActorID("actor-42").WithActorType("user"))
//	mem, err := client.Add(ctx, "content", opts)
type AddOptions struct {
	Scope    *Scope
	Metadata map[string]any
}

// NewAddOptions returns a new empty AddOptions ready for fluent configuration.
func NewAddOptions() *AddOptions { return &AddOptions{} }

// WithScope sets the actor and conversation context for this add operation.
func (o *AddOptions) WithScope(s *Scope) *AddOptions { o.Scope = s; return o }

// WithMetadata sets Metadata.
func (o *AddOptions) WithMetadata(m map[string]any) *AddOptions { o.Metadata = m; return o }

// SearchOptions are the optional parameters for Client.Search.
//
// Use NewSearchOptions() with fluent With* methods for ergonomic construction:
//
//	opts := smritea.NewSearchOptions().WithScope(smritea.NewScope().WithActorID("actor-42")).WithLimit(10)
//	results, err := client.Search(ctx, "query", opts)
type SearchOptions struct {
	Scope      *Scope
	Limit      *int32
	Threshold  *float32
	GraphDepth *int32
	// FromTime is an ISO-8601 datetime string — only return memories created at or after this time.
	FromTime *string
	// ToTime is an ISO-8601 datetime string — only return memories created at or before this time.
	ToTime *string
	// ValidAt is an ISO-8601 datetime string — return memories valid at exactly this point in time.
	ValidAt *string
}

// NewSearchOptions returns a new empty SearchOptions ready for fluent configuration.
func NewSearchOptions() *SearchOptions { return &SearchOptions{} }

// WithScope sets the actor and conversation context for this search operation.
func (o *SearchOptions) WithScope(s *Scope) *SearchOptions { o.Scope = s; return o }

// WithLimit sets the maximum number of results.
func (o *SearchOptions) WithLimit(n int32) *SearchOptions { o.Limit = &n; return o }

// WithThreshold sets the minimum similarity threshold.
func (o *SearchOptions) WithThreshold(t float32) *SearchOptions { o.Threshold = &t; return o }

// WithGraphDepth sets the graph traversal depth.
func (o *SearchOptions) WithGraphDepth(d int32) *SearchOptions { o.GraphDepth = &d; return o }

// WithFromTime sets the lower bound for memory creation time filter.
func (o *SearchOptions) WithFromTime(t string) *SearchOptions {
	o.FromTime = &t
	return o
}

// WithToTime sets the upper bound for memory creation time filter.
func (o *SearchOptions) WithToTime(t string) *SearchOptions {
	o.ToTime = &t
	return o
}

// WithValidAt sets the point-in-time filter for memory validity.
func (o *SearchOptions) WithValidAt(t string) *SearchOptions {
	o.ValidAt = &t
	return o
}
