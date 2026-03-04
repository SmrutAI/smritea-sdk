package ai.smritea.sdk;

import ai.smritea.sdk.errors.SmriteaAuthError;
import ai.smritea.sdk.errors.SmriteaError;
import ai.smritea.sdk.errors.SmriteaNotFoundError;
import ai.smritea.sdk.errors.SmriteaQuotaError;
import ai.smritea.sdk.errors.SmriteaRateLimitError;
import ai.smritea.sdk.errors.SmriteaValidationError;
import ai.smritea.sdk.model.AddOptions;
import ai.smritea.sdk.model.Memory;
import ai.smritea.sdk.model.SearchOptions;
import ai.smritea.sdk.model.SearchResult;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Entry point for all Smritea memory operations. Create one instance per application and reuse it
 * across calls. All methods are safe for concurrent use.
 */
public class SmriteaClient {

    private static final String DEFAULT_BASE_URL = "https://api.smritea.ai";
    private static final int DEFAULT_MAX_RETRIES = 2;
    private static final double RETRY_CAP_SECONDS = 30.0;

    private final String appId;
    private final int maxRetries;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new SmriteaClient with the default base URL and retry count.
     *
     * @param apiKey the API key for authentication
     * @param appId the application ID injected into every request
     */
    public SmriteaClient(String apiKey, String appId) {
        this(apiKey, appId, DEFAULT_BASE_URL, DEFAULT_MAX_RETRIES);
    }

    /**
     * Creates a new SmriteaClient with full configuration.
     *
     * @param apiKey the API key for authentication
     * @param appId the application ID injected into every request
     * @param baseUrl the API base URL (defaults to https://api.smritea.ai if null)
     * @param maxRetries maximum number of retries on 429 responses (minimum 0)
     */
    public SmriteaClient(String apiKey, String appId, String baseUrl, int maxRetries) {
        Objects.requireNonNull(apiKey, "apiKey must not be null");
        Objects.requireNonNull(appId, "appId must not be null");
        this.apiKey = apiKey;
        this.appId = appId;
        this.baseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
        this.maxRetries = Math.max(0, maxRetries);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Stores a new memory with the given content. When {@code opts.getUserId()} is set it takes
     * precedence over {@code opts.getActorId()} and forces actor_type="user".
     *
     * @param content the memory content text
     * @param opts optional parameters controlling actor attribution, metadata, and conversation
     * @return the created Memory
     * @throws SmriteaError on any API error
     */
    public Memory add(String content, AddOptions opts) {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", appId);
        body.put("content", content);

        if (opts != null) {
            // resolveActor: if userId is set, use it as actorId and force actorType="user"
            String actorId = opts.getUserId() != null ? opts.getUserId() : opts.getActorId();
            String actorType = opts.getUserId() != null ? "user" : opts.getActorType();

            if (actorId != null) body.put("actor_id", actorId);
            if (actorType != null) body.put("actor_type", actorType);
            if (opts.getActorName() != null) body.put("actor_name", opts.getActorName());
            if (opts.getMetadata() != null) body.put("metadata", opts.getMetadata());
            if (opts.getConversationId() != null) body.put("conversation_id", opts.getConversationId());
        }

        return executeWithRetry(() -> {
            HttpResponse<String> resp = sendRequest("POST", "/api/v1/sdk/memories", body);
            if (resp.statusCode() >= 400) {
                throw mapError(resp.statusCode(), resp.body(), getRetryAfterHeader(resp));
            }
            return parseMemory(objectMapper.readTree(resp.body()));
        });
    }

    /**
     * Retrieves memories ranked by relevance to the given query. Returns an empty (non-null) list
     * when no memories match.
     *
     * @param query the search query text
     * @param opts optional parameters controlling actor scoping, result count, search method, etc.
     * @return a list of SearchResult instances, never null
     * @throws SmriteaError on any API error
     */
    public List<SearchResult> search(String query, SearchOptions opts) {
        Map<String, Object> body = new HashMap<>();
        body.put("app_id", appId);
        body.put("query", query);

        if (opts != null) {
            String actorId = opts.getUserId() != null ? opts.getUserId() : opts.getActorId();
            String actorType = opts.getUserId() != null ? "user" : opts.getActorType();

            if (actorId != null) body.put("actor_id", actorId);
            if (actorType != null) body.put("actor_type", actorType);
            if (opts.getLimit() != null) body.put("limit", opts.getLimit());
            if (opts.getMethod() != null) body.put("method", opts.getMethod());
            if (opts.getThreshold() != null) body.put("threshold", opts.getThreshold());
            if (opts.getGraphDepth() != null) body.put("graph_depth", opts.getGraphDepth());
            if (opts.getConversationId() != null) body.put("conversation_id", opts.getConversationId());
        }

        return executeWithRetry(() -> {
            HttpResponse<String> resp = sendRequest("POST", "/api/v1/sdk/memories/search", body);
            if (resp.statusCode() >= 400) {
                throw mapError(resp.statusCode(), resp.body(), getRetryAfterHeader(resp));
            }
            // Parse: {"memories": [{"memory": {...}, "score": 0.9}, ...]}
            JsonNode root = objectMapper.readTree(resp.body());
            JsonNode memoriesNode = root.get("memories");
            if (memoriesNode == null || !memoriesNode.isArray()) {
                return Collections.emptyList();
            }
            List<SearchResult> results = new ArrayList<>();
            for (JsonNode node : memoriesNode) {
                Memory mem = parseMemory(node.get("memory"));
                Double score = node.has("score") ? node.get("score").asDouble() : null;
                results.add(new SearchResult(mem, score));
            }
            return results;
        });
    }

    /**
     * Fetches a single memory by its ID.
     *
     * @param memoryId the memory ID to retrieve
     * @return the Memory
     * @throws SmriteaNotFoundError when the memory does not exist
     * @throws SmriteaError on any other API error
     */
    public Memory get(String memoryId) {
        return executeWithRetry(() -> {
            HttpResponse<String> resp = sendRequest("GET", "/api/v1/sdk/memories/" + memoryId, null);
            if (resp.statusCode() >= 400) {
                throw mapError(resp.statusCode(), resp.body(), getRetryAfterHeader(resp));
            }
            return parseMemory(objectMapper.readTree(resp.body()));
        });
    }

    /**
     * Permanently removes a memory by its ID.
     *
     * @param memoryId the memory ID to delete
     * @throws SmriteaNotFoundError when the memory does not exist
     * @throws SmriteaError on any other API error
     */
    public void delete(String memoryId) {
        executeWithRetry(() -> {
            HttpResponse<String> resp =
                    sendRequest("DELETE", "/api/v1/sdk/memories/" + memoryId, null);
            if (resp.statusCode() >= 400) {
                throw mapError(resp.statusCode(), resp.body(), getRetryAfterHeader(resp));
            }
            return null;
        });
    }

    /**
     * Not yet implemented. The list memories endpoint is pending server-side implementation. This
     * method is provided for forward compatibility.
     *
     * @return never returns normally
     * @throws UnsupportedOperationException always
     */
    public List<Memory> getAll() {
        throw new UnsupportedOperationException(
                "getAll() is not yet available. The list memories endpoint is pending server-side"
                        + " implementation.");
    }

    // ---------------------------------------------------------------------------
    // Retry logic
    // ---------------------------------------------------------------------------

    @FunctionalInterface
    private interface SupplierWithException<T> {
        T get() throws Exception;
    }

    /**
     * Executes the supplier with retry on 429 (rate limit) errors. Other errors bubble up
     * immediately. Matches the Go withRetry implementation exactly.
     */
    private <T> T executeWithRetry(SupplierWithException<T> fn) {
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return fn.get();
            } catch (SmriteaRateLimitError e) {
                if (attempt < maxRetries) {
                    double delay = retryDelay(attempt, e.getRetryAfter());
                    try {
                        Thread.sleep((long) (delay * 1000));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new SmriteaError("Request interrupted during retry", null);
                    }
                    continue;
                }
                throw e;
            } catch (SmriteaError e) {
                throw e;
            } catch (Exception e) {
                throw new SmriteaError(e.getMessage(), null);
            }
        }
        throw new SmriteaError("max retries exceeded", null);
    }

    /**
     * Computes sleep duration before the next retry attempt. When retryAfter is provided by the
     * server, 90% of that value is used (to account for clock skew) capped at RETRY_CAP_SECONDS.
     * Otherwise exponential backoff with jitter in [0.75, 1.25] is applied.
     */
    private double retryDelay(int attempt, Integer retryAfter) {
        if (retryAfter != null && retryAfter > 0) {
            return Math.min(retryAfter * 0.9, RETRY_CAP_SECONDS);
        }
        double base = Math.min(Math.pow(2, attempt), RETRY_CAP_SECONDS);
        double jitter = base * (0.75 + 0.5 * ThreadLocalRandom.current().nextDouble());
        return Math.min(jitter, RETRY_CAP_SECONDS);
    }

    // ---------------------------------------------------------------------------
    // Error mapping
    // ---------------------------------------------------------------------------

    /**
     * Converts an HTTP status code into the appropriate typed SDK error. Matches the Go mapError
     * function exactly.
     */
    private SmriteaError mapError(int statusCode, String body, String retryAfterHeader) {
        switch (statusCode) {
            case 400:
                return new SmriteaValidationError(body, statusCode);
            case 401:
                return new SmriteaAuthError(body, statusCode);
            case 402:
                return new SmriteaQuotaError(body, statusCode);
            case 404:
                return new SmriteaNotFoundError(body, statusCode);
            case 429:
                Integer retryAfter = parseRetryAfter(retryAfterHeader);
                return new SmriteaRateLimitError(body, statusCode, retryAfter);
            default:
                return new SmriteaError(body, statusCode);
        }
    }

    // ---------------------------------------------------------------------------
    // HTTP helpers
    // ---------------------------------------------------------------------------

    private HttpResponse<String> sendRequest(String method, String path, Map<String, Object> body)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder =
                HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + path))
                        .header("Content-Type", "application/json")
                        .header("X-API-Key", apiKey);

        if (body != null) {
            String json = objectMapper.writeValueAsString(body);
            builder.method(method, HttpRequest.BodyPublishers.ofString(json));
        } else if ("DELETE".equals(method)) {
            builder.DELETE();
        } else {
            builder.GET();
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String getRetryAfterHeader(HttpResponse<String> resp) {
        return resp.headers().firstValue("Retry-After").orElse(null);
    }

    private Integer parseRetryAfter(String header) {
        if (header == null || header.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(header);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // ---------------------------------------------------------------------------
    // JSON deserialization helpers
    // ---------------------------------------------------------------------------

    /**
     * Parses a Memory from a JsonNode. This avoids requiring Jackson annotations on the Memory
     * class by extracting fields manually and calling the all-args constructor.
     */
    private Memory parseMemory(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return new Memory(
                textOrNull(node, "id"),
                textOrNull(node, "app_id"),
                textOrNull(node, "content"),
                textOrNull(node, "actor_id"),
                textOrNull(node, "actor_type"),
                textOrNull(node, "actor_name"),
                parseMetadata(node.get("metadata")),
                textOrNull(node, "conversation_id"),
                textOrNull(node, "conversation_message_id"),
                textOrNull(node, "active_from"),
                textOrNull(node, "active_to"),
                textOrNull(node, "created_at"),
                textOrNull(node, "updated_at"));
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode child = node.get(field);
        if (child == null || child.isNull()) {
            return null;
        }
        return child.asText();
    }

    private Map<String, Object> parseMetadata(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        return objectMapper.convertValue(node, new TypeReference<Map<String, Object>>() {});
    }
}
