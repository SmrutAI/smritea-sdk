package ai.smritea.sdk;

import ai.smritea.sdk._internal.autogen.ApiClient;
import ai.smritea.sdk._internal.autogen.ApiException;
import ai.smritea.sdk._internal.autogen.api.SdkMemoryApi;
import ai.smritea.sdk._internal.autogen.model.CommondtoMemoryScope;
import ai.smritea.sdk._internal.autogen.model.CommondtoRelativeStandingConfig;
import ai.smritea.sdk._internal.autogen.model.MemoryCreateMemoryRequest;
import ai.smritea.sdk._internal.autogen.model.MemoryCreateMemoryResponse;
import ai.smritea.sdk._internal.autogen.model.MemoryMemoryResponse;
import ai.smritea.sdk._internal.autogen.model.MemorySearchMemoriesResponse;
import ai.smritea.sdk._internal.autogen.model.MemorySearchMemoryRequest;
import ai.smritea.sdk._internal.autogen.model.MemorySearchMemoryResponse;
import ai.smritea.sdk._internal.autogen.model.ModelEnumsRerankerType;
import ai.smritea.sdk._internal.autogen.model.ModelEnumsSearchMethod;
import ai.smritea.sdk.errors.SmriteaAuthError;
import ai.smritea.sdk.errors.SmriteaDeserializationError;
import ai.smritea.sdk.errors.SmriteaError;
import ai.smritea.sdk.errors.SmriteaNotFoundError;
import ai.smritea.sdk.errors.SmriteaQuotaError;
import ai.smritea.sdk.errors.SmriteaRateLimitError;
import ai.smritea.sdk.errors.SmriteaValidationError;
import ai.smritea.sdk.model.AddOptions;
import ai.smritea.sdk.model.Memory;
import ai.smritea.sdk.model.MemoryCreationResult;
import ai.smritea.sdk.model.MemoryScope;
import ai.smritea.sdk.model.RelativeStanding;
import ai.smritea.sdk.model.SearchOptions;
import ai.smritea.sdk.model.SearchResult;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
  private final SdkMemoryApi api;

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
    this.appId = appId;
    this.maxRetries = Math.max(0, maxRetries);

    // The autogen SdkMemoryApi already includes /api/v1 in every path (e.g.
    // /api/v1/sdk/memories). updateBaseUri() parses the full URL into separate scheme/host/port
    // fields so getBaseUri() returns the bare host. setBasePath() would only set the path
    // suffix
    // and getBaseUri() would produce a malformed URL with the full URL string as the path.
    String resolvedBaseUrl = baseUrl != null ? baseUrl : DEFAULT_BASE_URL;
    ApiClient apiClient = new ApiClient();
    apiClient.updateBaseUri(resolvedBaseUrl);
    apiClient.setRequestInterceptor(builder -> builder.header("X-API-Key", apiKey));
    this.api = new SdkMemoryApi(apiClient);
  }

  /**
   * Package-private constructor for testing. Allows injecting a custom {@link SdkMemoryApi} backed
   * by a test {@link ApiClient} (e.g. pointed at WireMock).
   *
   * @param appId the application ID injected into every request
   * @param maxRetries maximum number of retries on 429 responses (minimum 0)
   * @param api the pre-configured SdkMemoryApi instance
   */
  SmriteaClient(String appId, int maxRetries, SdkMemoryApi api) {
    this.appId = appId;
    this.maxRetries = Math.max(0, maxRetries);
    this.api = api;
  }

  /**
   * Stores a new memory with the given content.
   *
   * @param content the memory content text
   * @param opts optional parameters controlling actor attribution, metadata, and conversation
   * @return a MemoryCreationResult containing all memories created from the extracted facts, plus
   *     metadata: factsExtracted, extractionConfidence, skippedCount, updatedCount
   * @throws SmriteaError on any API error
   */
  public MemoryCreationResult add(String content, AddOptions opts) {
    MemoryCreateMemoryRequest request = new MemoryCreateMemoryRequest();
    request.setAppId(appId);
    request.setContent(content);

    if (opts != null) {
      MemoryScope scope = opts.getScope();
      if (scope != null) {
        CommondtoMemoryScope autogenScope = new CommondtoMemoryScope();
        if (scope.getActorId() != null) {
          autogenScope.setActorId(scope.getActorId());
        }
        if (scope.getActorType() != null) {
          autogenScope.setActorType(
              CommondtoMemoryScope.ActorTypeEnum.fromValue(scope.getActorType()));
        }
        if (scope.getActorName() != null) {
          autogenScope.setActorName(scope.getActorName());
        }
        if (scope.getConversationId() != null) {
          autogenScope.setConversationId(scope.getConversationId());
        }
        if (scope.getSourceType() != null) {
          autogenScope.setSourceType(
              CommondtoMemoryScope.SourceTypeEnum.fromValue(scope.getSourceType()));
        }
        if (scope.getParticipantIds() != null && !scope.getParticipantIds().isEmpty()) {
          autogenScope.setParticipantIds(scope.getParticipantIds());
        }
        request.setScope(autogenScope);
      }
      if (opts.getMetadata() != null) {
        request.setMetadata(opts.getMetadata());
      }
      if (opts.getEventOccurredAt() != null) {
        request.setEventOccurredAt(opts.getEventOccurredAt());
      }
      RelativeStanding rs = opts.getRelativeStanding();
      if (rs != null) {
        CommondtoRelativeStandingConfig autogenRs = new CommondtoRelativeStandingConfig();
        if (rs.getImportance() != null) {
          autogenRs.setImportance(BigDecimal.valueOf(rs.getImportance()));
        }
        if (rs.getDecayFactor() != null) {
          autogenRs.setDecayFactor(BigDecimal.valueOf(rs.getDecayFactor()));
        }
        if (rs.getDecayFunction() != null) {
          autogenRs.setDecayFunction(
              CommondtoRelativeStandingConfig.DecayFunctionEnum.fromValue(rs.getDecayFunction()));
        }
        request.setRelativeStanding(autogenRs);
      }
    }

    return executeWithRetry(
        () -> {
          try {
            MemoryCreateMemoryResponse resp = api.createMemory(request);
            if (resp == null) {
              throw new SmriteaDeserializationError(
                  "server returned null body for add() — expected a MemoryCreateMemoryResponse"
                      + " object");
            }
            return new MemoryCreationResult(resp);
          } catch (ApiException e) {
            throw mapError(e);
          }
        });
  }

  /**
   * Retrieves memories ranked by relevance to the given query. Returns an empty (non-null) list
   * when no memories match.
   *
   * @param query the search query text
   * @param opts optional parameters controlling actor scoping, result count, threshold, etc.
   * @return a list of SearchResult instances, never null
   * @throws SmriteaError on any API error
   */
  public List<SearchResult> search(String query, SearchOptions opts) {
    MemorySearchMemoryRequest request = new MemorySearchMemoryRequest();
    request.setAppId(appId);
    request.setQuery(query);

    if (opts != null) {
      MemoryScope scope = opts.getScope();
      if (scope != null) {
        CommondtoMemoryScope autogenScope = new CommondtoMemoryScope();
        if (scope.getActorId() != null) {
          autogenScope.setActorId(scope.getActorId());
        }
        if (scope.getActorType() != null) {
          autogenScope.setActorType(
              CommondtoMemoryScope.ActorTypeEnum.fromValue(scope.getActorType()));
        }
        if (scope.getConversationId() != null) {
          autogenScope.setConversationId(scope.getConversationId());
        }
        if (scope.getParticipantIds() != null && !scope.getParticipantIds().isEmpty()) {
          autogenScope.setParticipantIds(scope.getParticipantIds());
        }
        request.setScope(autogenScope);
      }
      if (opts.getLimit() != null) {
        request.setLimit(opts.getLimit());
      }
      if (opts.getThreshold() != null) {
        request.setThreshold(BigDecimal.valueOf(opts.getThreshold()));
      }
      if (opts.getGraphDepth() != null) {
        request.setGraphDepth(opts.getGraphDepth());
      }
      if (opts.getFromTime() != null) {
        request.setFromTime(opts.getFromTime());
      }
      if (opts.getToTime() != null) {
        request.setToTime(opts.getToTime());
      }
      if (opts.getValidAt() != null) {
        request.setValidAt(opts.getValidAt());
      }
      if (opts.getMethod() != null) {
        request.setMethod(ModelEnumsSearchMethod.fromValue(opts.getMethod()));
      }
      if (opts.getRerankerType() != null) {
        request.setRerankerType(ModelEnumsRerankerType.fromValue(opts.getRerankerType()));
      }
    }

    return executeWithRetry(
        () -> {
          try {
            MemorySearchMemoriesResponse resp = api.searchMemories(request);
            List<MemorySearchMemoryResponse> memories = resp.getMemories();
            if (memories == null || memories.isEmpty()) {
              return Collections.emptyList();
            }
            List<SearchResult> results = new ArrayList<>(memories.size());
            for (int i = 0; i < memories.size(); i++) {
              results.add(new SearchResult(memories.get(i)));
            }
            return results;
          } catch (ApiException e) {
            throw mapError(e);
          }
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
    return executeWithRetry(
        () -> {
          try {
            MemoryMemoryResponse resp = api.getMemory(memoryId);
            if (resp == null) {
              throw new SmriteaDeserializationError(
                  "server returned null body for get() — expected a MemoryMemoryResponse object");
            }
            return new Memory(resp);
          } catch (ApiException e) {
            throw mapError(e);
          }
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
    executeWithRetry(
        () -> {
          try {
            api.deleteMemory(memoryId);
            return null;
          } catch (ApiException e) {
            throw mapError(e);
          }
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
    // Unreachable: the loop always returns on success or throws on non-retriable errors.
    // On the final attempt, SmriteaRateLimitError is rethrown directly.
    // This satisfies the Java compiler's requirement for a return/throw after the loop.
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
   * Extracts the Retry-After header value from an {@link ApiException}.
   *
   * @return the header value, or null if not present
   */
  private String getRetryAfterHeader(ApiException e) {
    if (e.getResponseHeaders() == null) {
      return null;
    }
    return e.getResponseHeaders().firstValue("Retry-After").orElse(null);
  }

  /**
   * Parses a Retry-After header string into an Integer (seconds).
   *
   * @return the parsed value, or null if unparseable
   */
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

  /**
   * Converts an {@link ApiException} into the appropriate typed SDK error. Matches the Go mapError
   * function exactly.
   */
  private SmriteaError mapError(ApiException e) {
    int statusCode = e.getCode();
    String body = e.getResponseBody();
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
        Integer retryAfter = parseRetryAfter(getRetryAfterHeader(e));
        return new SmriteaRateLimitError(body, statusCode, retryAfter);
      default:
        return new SmriteaError(body, statusCode);
    }
  }
}
