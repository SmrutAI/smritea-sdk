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

// AddOptions are the optional parameters for Client.Add.
// UserID is a convenience shorthand: sets ActorID and forces ActorType="user".
//
// Use NewAddOptions() with fluent With* methods for ergonomic construction:
//
//	opts := smritea.NewAddOptions().WithUserID("user-42")
//	mem, err := client.Add(ctx, "content", opts)
type AddOptions struct {
	UserID         *string
	ActorID        *string
	ActorType      *string
	ActorName      *string
	Metadata       map[string]any
	ConversationID *string
}

// NewAddOptions returns a new empty AddOptions ready for fluent configuration.
func NewAddOptions() *AddOptions { return &AddOptions{} }

// WithUserID sets UserID (convenience shorthand that forces ActorType="user").
func (o *AddOptions) WithUserID(id string) *AddOptions { o.UserID = &id; return o }

// WithActorID sets ActorID.
func (o *AddOptions) WithActorID(id string) *AddOptions { o.ActorID = &id; return o }

// WithActorType sets ActorType.
func (o *AddOptions) WithActorType(t string) *AddOptions { o.ActorType = &t; return o }

// WithActorName sets ActorName.
func (o *AddOptions) WithActorName(name string) *AddOptions { o.ActorName = &name; return o }

// WithMetadata sets Metadata.
func (o *AddOptions) WithMetadata(m map[string]any) *AddOptions { o.Metadata = m; return o }

// WithConversationID sets ConversationID.
func (o *AddOptions) WithConversationID(id string) *AddOptions { o.ConversationID = &id; return o }

// SearchOptions are the optional parameters for Client.Search.
//
// Use NewSearchOptions() with fluent With* methods for ergonomic construction:
//
//	opts := smritea.NewSearchOptions().WithLimit(10).WithMethod("hybrid")
//	results, err := client.Search(ctx, "query", opts)
type SearchOptions struct {
	UserID         *string
	ActorID        *string
	ActorType      *string
	Limit          *int32
	Method         *string
	Threshold      *float32
	GraphDepth     *int32
	ConversationID *string
}

// NewSearchOptions returns a new empty SearchOptions ready for fluent configuration.
func NewSearchOptions() *SearchOptions { return &SearchOptions{} }

// WithUserID sets UserID (convenience shorthand that forces ActorType="user").
func (o *SearchOptions) WithUserID(id string) *SearchOptions { o.UserID = &id; return o }

// WithActorID sets ActorID.
func (o *SearchOptions) WithActorID(id string) *SearchOptions { o.ActorID = &id; return o }

// WithActorType sets ActorType.
func (o *SearchOptions) WithActorType(t string) *SearchOptions { o.ActorType = &t; return o }

// WithLimit sets the maximum number of results.
func (o *SearchOptions) WithLimit(n int32) *SearchOptions { o.Limit = &n; return o }

// WithMethod sets the search method.
func (o *SearchOptions) WithMethod(m string) *SearchOptions { o.Method = &m; return o }

// WithThreshold sets the minimum similarity threshold.
func (o *SearchOptions) WithThreshold(t float32) *SearchOptions { o.Threshold = &t; return o }

// WithGraphDepth sets the graph traversal depth.
func (o *SearchOptions) WithGraphDepth(d int32) *SearchOptions { o.GraphDepth = &d; return o }

// WithConversationID sets ConversationID.
func (o *SearchOptions) WithConversationID(id string) *SearchOptions {
	o.ConversationID = &id
	return o
}
