package smritea

import "github.com/SmrutAI/smritea-sdk/go/internal/autogen"

// Memory is the canonical return type for a stored memory.
type Memory = autogen.MemoryMemoryResponse

// MemoryCreationResult is the response from Add(). Contains all memories
// created from the extracted facts (GetMemories()), plus extraction metadata:
// GetFactsExtracted(), GetSkippedCount(), GetUpdatedCount().
type MemoryCreationResult = autogen.MemoryCreateMemoryResponse

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

// MemoryScope groups actor and conversation context fields for memory operations.
// This is the public OSS-aligned name; wire values map to commondto.MemoryScope in API requests.
type MemoryScope struct {
	// ActorID is the identifier for the actor (user, agent, or system) associated
	// with this memory. Must be set together with ActorType; max 64 characters.
	ActorID *string
	// ActorType is the role of the actor. Accepted values: "user", "agent", "system".
	// Must be set together with ActorID.
	ActorType *string
	// ActorName is the human-readable display name of the actor. Optional; max 255 characters.
	ActorName *string
	// ConversationID scopes this memory to a specific conversation thread; max 64 characters.
	// Mutually exclusive with ParticipantIDs; if both are set, ConversationID takes precedence.
	ConversationID *string
	// SourceType is the origin of the memory. Accepted values: "conversation", "document",
	// "api". Defaults to "api" when omitted.
	SourceType *string
	// ParticipantIDs searches across conversations where all listed actors participated
	// (AND semantics). The service expands this list into the matching conversation IDs
	// before querying. Requires at least 2 IDs; each ID must be 1–64 characters.
	// Mutually exclusive with ConversationID; if both are set, ConversationID wins.
	// Only relevant for Search — ignored on Add.
	ParticipantIDs []string
}

// NewMemoryScope returns a new empty MemoryScope ready for fluent configuration.
func NewMemoryScope() *MemoryScope { return &MemoryScope{} }

// WithActorID sets ActorID.
func (s *MemoryScope) WithActorID(id string) *MemoryScope { s.ActorID = &id; return s }

// WithActorType sets ActorType.
func (s *MemoryScope) WithActorType(t string) *MemoryScope { s.ActorType = &t; return s }

// WithActorName sets ActorName.
func (s *MemoryScope) WithActorName(name string) *MemoryScope { s.ActorName = &name; return s }

// WithConversationID sets ConversationID.
func (s *MemoryScope) WithConversationID(id string) *MemoryScope { s.ConversationID = &id; return s }

// WithSourceType sets SourceType.
func (s *MemoryScope) WithSourceType(t string) *MemoryScope { s.SourceType = &t; return s }

// WithParticipantIDs sets ParticipantIDs to search across conversations where all listed
// actors participated. Requires at least 2 IDs. Mutually exclusive with ConversationID.
func (s *MemoryScope) WithParticipantIDs(ids []string) *MemoryScope { s.ParticipantIDs = ids; return s }

// RelativeStanding groups per-memory importance and temporal decay parameters.
type RelativeStanding struct {
	// Importance is the memory importance score (0-1). Higher = ranks higher in search.
	Importance *float32
	// DecayFactor modulates the temporal decay rate. 0 = no decay (memory score is pinned
	// permanently). 0.2 = light decay (default). 1.0 = standard. 3.0+ = aggressive.
	DecayFactor *float32
	// DecayFunction selects the temporal decay algorithm.
	// Accepted values: "exponential", "gaussian", "linear".
	DecayFunction *string
}

// NewRelativeStanding returns a new empty RelativeStanding ready for fluent configuration.
func NewRelativeStanding() *RelativeStanding { return &RelativeStanding{} }

// WithImportance sets the importance score.
func (r *RelativeStanding) WithImportance(v float32) *RelativeStanding { r.Importance = &v; return r }

// WithDecayFactor sets the decay factor.
func (r *RelativeStanding) WithDecayFactor(v float32) *RelativeStanding { r.DecayFactor = &v; return r }

// WithDecayFunction sets the decay function.
func (r *RelativeStanding) WithDecayFunction(v string) *RelativeStanding {
	r.DecayFunction = &v
	return r
}

// AddOptions are the optional parameters for Client.Add.
//
// Use NewAddOptions() with fluent With* methods for ergonomic construction:
//
//	opts := smritea.NewAddOptions().WithScope(smritea.NewMemoryScope().WithActorID("actor-42").WithActorType("user"))
//	mem, err := client.Add(ctx, "content", opts)
type AddOptions struct {
	Scope    *MemoryScope
	Metadata map[string]any
	// EventOccurredAt is an ISO-8601 datetime string — when this content was created or occurred.
	// Used by the extraction LLM to resolve relative temporal expressions like "last year".
	// Defaults to current time if nil.
	EventOccurredAt *string
	// RelativeStanding sets importance and temporal decay configuration for this memory.
	RelativeStanding *RelativeStanding
}

// NewAddOptions returns a new empty AddOptions ready for fluent configuration.
func NewAddOptions() *AddOptions { return &AddOptions{} }

// WithScope sets the actor and conversation context for this add operation.
func (o *AddOptions) WithScope(s *MemoryScope) *AddOptions { o.Scope = s; return o }

// WithMetadata sets Metadata.
func (o *AddOptions) WithMetadata(m map[string]any) *AddOptions { o.Metadata = m; return o }

// WithEventOccurredAt sets the temporal anchor for extraction.
func (o *AddOptions) WithEventOccurredAt(t string) *AddOptions { o.EventOccurredAt = &t; return o }

// WithRelativeStanding sets importance and decay configuration.
func (o *AddOptions) WithRelativeStanding(r *RelativeStanding) *AddOptions {
	o.RelativeStanding = r
	return o
}

// SearchOptions are the optional parameters for Client.Search.
//
// Use NewSearchOptions() with fluent With* methods for ergonomic construction:
//
//	opts := smritea.NewSearchOptions().WithScope(smritea.NewMemoryScope().WithActorID("actor-42")).WithLimit(10)
//	results, err := client.Search(ctx, "query", opts)
type SearchOptions struct {
	Scope      *MemoryScope
	Limit      *int32
	Threshold  *float32
	GraphDepth *int32
	// FromTime is an ISO-8601 datetime string — only return memories created at or after this time.
	FromTime *string
	// ToTime is an ISO-8601 datetime string — only return memories created at or before this time.
	ToTime *string
	// ValidAt is an ISO-8601 datetime string — return memories valid at exactly this point in time.
	ValidAt *string
	// Method overrides the search method. Accepted values: "quick_search", "deep_search",
	// "context_aware_search". Defaults to app config if nil.
	Method *string
	// RerankerType overrides the reranker. Accepted values: "rrf_temporal", "rrf", "temporal",
	// "node_distance", "mmr", "cross_encoder". Only applies to deep_search. Defaults to app config if nil.
	RerankerType *string
	// MetadataFilter is a MongoDB-style operator DSL for filtering search results by their metadata.
	// Supports $eq, $ne, $gt, $gte, $lt, $lte, $in, $nin, $contains, $and, $or, $not, and wildcard "*".
	// Values must be string, int64, or float64. Booleans and nested objects are rejected by the server.
	// Example: map[string]any{"department": "engineering"} or
	// map[string]any{"level": map[string]any{"$gte": 4}}.
	// Note: $contains is applied as a post-filter and may return fewer results than Limit.
	// $contains inside $or is rejected with HTTP 400.
	MetadataFilter map[string]any
}

// NewSearchOptions returns a new empty SearchOptions ready for fluent configuration.
func NewSearchOptions() *SearchOptions { return &SearchOptions{} }

// WithScope sets the actor and conversation context for this search operation.
func (o *SearchOptions) WithScope(s *MemoryScope) *SearchOptions { o.Scope = s; return o }

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

// WithMethod sets the search method override.
func (o *SearchOptions) WithMethod(m string) *SearchOptions {
	o.Method = &m
	return o
}

// WithRerankerType sets the reranker type override.
func (o *SearchOptions) WithRerankerType(r string) *SearchOptions {
	o.RerankerType = &r
	return o
}

// WithMetadataFilter sets a MongoDB-style operator DSL filter on memory metadata.
// Supports $eq, $ne, $gt, $gte, $lt, $lte, $in, $nin, $contains, $and, $or, $not,
// and wildcard "*". Values must be string, int64, or float64.
// Note: $contains is applied as a post-filter and may return fewer results than Limit.
// $contains inside $or is rejected by the server (HTTP 400).
func (o *SearchOptions) WithMetadataFilter(m map[string]any) *SearchOptions {
	o.MetadataFilter = m
	return o
}
