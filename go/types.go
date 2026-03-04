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
type AddOptions struct {
	UserID         *string
	ActorID        *string
	ActorType      *string
	ActorName      *string
	Metadata       map[string]any
	ConversationID *string
}

// SearchOptions are the optional parameters for Client.Search.
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
