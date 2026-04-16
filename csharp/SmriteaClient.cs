// <copyright file="SmriteaClient.cs" company="PlaceholderCompany">
// Copyright (c) PlaceholderCompany. All rights reserved.
// </copyright>

namespace Smritea.Sdk;

using Smritea.Internal.Autogen.Api;
using Smritea.Internal.Autogen.Client;
using Smritea.Internal.Autogen.Model;

/// <summary>
/// Entry point for all smritea memory operations.
/// Create one instance per application and reuse it across calls.
/// All methods delegate to the auto-generated <see cref="SDKMemoryApi"/> for HTTP handling.
/// Implements <see cref="IDisposable"/> to release the underlying API client resources.
/// </summary>
public class SmriteaClient : IDisposable
{
    private const string DefaultBaseUrl = "https://api.smritea.ai";
    private const int DefaultMaxRetries = 2;
    private const double RetryCap = 30.0;

    private readonly string appId;
    private readonly int maxRetries;
    private readonly SDKMemoryApi api;

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaClient"/> class.
    /// Creates a new <see cref="SmriteaClient"/> with the default base URL and retry count.
    /// </summary>
    /// <param name="apiKey">API key for authentication.</param>
    /// <param name="appId">Application ID injected into every request.</param>
    /// <param name="baseUrl">Base URL of the smritea API. Defaults to <c>https://api.smritea.ai</c>.</param>
    /// <param name="maxRetries">Maximum number of retries on HTTP 429. Defaults to 2.</param>
    public SmriteaClient(string apiKey, string appId, string? baseUrl = null, int maxRetries = DefaultMaxRetries)
    {
        ArgumentNullException.ThrowIfNull(apiKey);
        ArgumentNullException.ThrowIfNull(appId);
        this.appId = appId;
        this.maxRetries = Math.Max(0, maxRetries);

        var resolvedBaseUrl = baseUrl ?? DefaultBaseUrl;
        var config = new Configuration
        {
            // BasePath is the server root only — the autogen paths already include /api/v1/...
            BasePath = resolvedBaseUrl,
        };
        config.ApiKey["X-API-Key"] = apiKey;
        this.api = new SDKMemoryApi(config);
    }

    /// <summary>
    /// Initializes a new instance of the <see cref="SmriteaClient"/> class.
    /// Internal constructor for testing. Allows injecting a pre-configured <see cref="SDKMemoryApi"/>
    /// backed by a test HTTP client (e.g. pointed at WireMock).
    /// </summary>
    /// <param name="appId">Application ID injected into every request.</param>
    /// <param name="maxRetries">Maximum number of retries on HTTP 429 (minimum 0).</param>
    /// <param name="api">The pre-configured SDKMemoryApi instance.</param>
    internal SmriteaClient(string appId, int maxRetries, SDKMemoryApi api)
    {
        this.appId = appId;
        this.maxRetries = Math.Max(0, maxRetries);
        this.api = api;
    }

    /// <summary>
    /// Stores a new memory with the given content. The optional <paramref name="opts"/>
    /// control which actor the memory is attributed to, metadata, and conversation scoping.
    /// </summary>
    /// <param name="content">The memory content text to store.</param>
    /// <param name="opts">Optional add options for actor attribution, metadata, and conversation scoping.</param>
    /// <param name="ct">Cancellation token.</param>
    /// <returns>A <see cref="Task{TResult}"/> resolving to a <see cref="MemoryCreationResult"/> containing all
    /// memories created from the extracted facts, plus metadata (factsExtracted, skippedCount,
    /// updatedCount).</returns>
    public async Task<MemoryCreationResult> AddAsync(string content, AddOptions? opts = null, CancellationToken ct = default)
    {
        var request = new MemoryCreateMemoryRequest
        {
            AppId = this.appId,
            Content = content,
        };

        if (opts is not null)
        {
            if (opts.Scope is not null)
            {
                var scope = new CommondtoMemoryScope();
                if (opts.Scope.ActorId is not null)
                {
                    scope.ActorId = opts.Scope.ActorId;
                }

                if (opts.Scope.ActorType is not null)
                {
                    scope.ActorType = Enum.Parse<CommondtoMemoryScope.ActorTypeEnum>(
                        opts.Scope.ActorType, ignoreCase: true);
                }

                if (opts.Scope.ActorName is not null)
                {
                    scope.ActorName = opts.Scope.ActorName;
                }

                if (opts.Scope.ConversationId is not null)
                {
                    scope.ConversationId = opts.Scope.ConversationId;
                }

                if (opts.Scope.SourceType is not null)
                {
                    scope.SourceType = Enum.Parse<CommondtoMemoryScope.SourceTypeEnum>(
                        opts.Scope.SourceType, ignoreCase: true);
                }

                if (opts.Scope.ParticipantIds is not null && opts.Scope.ParticipantIds.Count > 0)
                {
                    scope.ParticipantIds = opts.Scope.ParticipantIds.ToList();
                }

                request.Scope = scope;
            }

            if (opts.Metadata is not null)
            {
                request.Metadata = opts.Metadata;
            }

            if (opts.EventOccurredAt is not null)
            {
                request.EventOccurredAt = opts.EventOccurredAt;
            }

            if (opts.RelativeStanding is not null)
            {
                var rs = new CommondtoRelativeStandingConfig();
                if (opts.RelativeStanding.Importance.HasValue)
                {
                    rs.Importance = (decimal)opts.RelativeStanding.Importance.Value;
                }

                if (opts.RelativeStanding.DecayFactor.HasValue)
                {
                    rs.DecayFactor = (decimal)opts.RelativeStanding.DecayFactor.Value;
                }

                if (opts.RelativeStanding.DecayFunction is not null)
                {
                    rs.DecayFunction = Enum.Parse<CommondtoRelativeStandingConfig.DecayFunctionEnum>(
                        opts.RelativeStanding.DecayFunction, ignoreCase: true);
                }

                request.RelativeStanding = rs;
            }
        }

        return await this.ExecuteWithRetryAsync(
            async token =>
            {
                try
                {
                    var resp = await this.api.CreateMemoryAsync(request, token);
                    if (resp is null)
                    {
                        throw new SmriteaDeserializationException(
                            "Server returned null for CreateMemory. The response body could not be deserialized.");
                    }

                    return resp;
                }
                catch (ApiException e)
                {
                    throw MapError(e);
                }
            },
            ct);
    }

    /// <summary>
    /// Retrieves memories ranked by relevance to the given query.
    /// Returns an empty list when no memories match.
    /// </summary>
    /// <param name="query">The search query string.</param>
    /// <param name="opts">Optional search options for filtering and ranking.</param>
    /// <param name="ct">Cancellation token.</param>
    /// <returns>A <see cref="Task{TResult}"/> resolving to a read-only list of <see cref="SearchResult"/> items.</returns>
    public async Task<IReadOnlyList<SearchResult>> SearchAsync(string query, SearchOptions? opts = null, CancellationToken ct = default)
    {
        // Must use the named-argument constructor form.
        // The autogen constructor validates appId/query and throws ArgumentNullException
        // if they are null. Object initializer syntax calls the constructor first with all
        // defaults (null), which triggers the guard before the setters run.
        var request = new MemorySearchMemoryRequest(appId: this.appId, query: query);

        if (opts is not null)
        {
            if (opts.Scope is not null)
            {
                var scope = new CommondtoMemoryScope();
                if (opts.Scope.ActorId is not null)
                {
                    scope.ActorId = opts.Scope.ActorId;
                }

                if (opts.Scope.ActorType is not null)
                {
                    scope.ActorType = Enum.Parse<CommondtoMemoryScope.ActorTypeEnum>(
                        opts.Scope.ActorType, ignoreCase: true);
                }

                if (opts.Scope.ConversationId is not null)
                {
                    scope.ConversationId = opts.Scope.ConversationId;
                }

                if (opts.Scope.ParticipantIds is not null && opts.Scope.ParticipantIds.Count > 0)
                {
                    scope.ParticipantIds = opts.Scope.ParticipantIds.ToList();
                }

                request.Scope = scope;
            }

            if (opts.Limit is not null)
            {
                request.Limit = opts.Limit.Value;
            }

            if (opts.Threshold is not null)
            {
                request.Threshold = (decimal)opts.Threshold.Value;
            }

            if (opts.GraphDepth is not null)
            {
                request.GraphDepth = opts.GraphDepth.Value;
            }

            if (opts.FromTime is not null)
            {
                request.FromTime = opts.FromTime;
            }

            if (opts.ToTime is not null)
            {
                request.ToTime = opts.ToTime;
            }

            if (opts.ValidAt is not null)
            {
                request.ValidAt = opts.ValidAt;
            }

            if (opts.Method is not null)
            {
                request.Method = Enum.Parse<ModelEnumsSearchMethod>(opts.Method, ignoreCase: true);
            }

            if (opts.RerankerType is not null)
            {
                request.RerankerType = Enum.Parse<ModelEnumsRerankerType>(opts.RerankerType, ignoreCase: true);
            }

            if (opts.MetadataFilter is not null)
            {
                request.MetadataFilter = opts.MetadataFilter;
            }
        }

        return await this.ExecuteWithRetryAsync<IReadOnlyList<SearchResult>>(
            async token =>
            {
                try
                {
                    var resp = await this.api.SearchMemoriesAsync(request, token);
                    var memories = resp.Memories;
                    if (memories is null || memories.Count == 0)
                    {
                        return Array.Empty<SearchResult>();
                    }

                    return memories.AsReadOnly();
                }
                catch (ApiException e)
                {
                    throw MapError(e);
                }
            },
            ct);
    }

    /// <summary>
    /// Fetches a single memory by its ID.
    /// Throws <see cref="SmriteaNotFoundException"/> when the memory does not exist.
    /// </summary>
    /// <param name="memoryId">The unique identifier of the memory to retrieve.</param>
    /// <param name="ct">Cancellation token.</param>
    /// <returns>A <see cref="Task{TResult}"/> resolving to the <see cref="Memory"/>.</returns>
    public async Task<Memory> GetAsync(string memoryId, CancellationToken ct = default)
    {
        return await this.ExecuteWithRetryAsync(
            async token =>
            {
                try
                {
                    var resp = await this.api.GetMemoryAsync(memoryId, token);
                    if (resp is null)
                    {
                        throw new SmriteaDeserializationException(
                            "Server returned null for GetMemory. The response body could not be deserialized.");
                    }

                    return resp;
                }
                catch (ApiException e)
                {
                    throw MapError(e);
                }
            },
            ct);
    }

    /// <summary>
    /// Permanently removes a memory by its ID.
    /// Throws <see cref="SmriteaNotFoundException"/> when the memory does not exist.
    /// </summary>
    /// <param name="memoryId">The unique identifier of the memory to delete.</param>
    /// <param name="ct">Cancellation token.</param>
    /// <returns>A <see cref="Task"/> representing the asynchronous delete operation.</returns>
    public async Task DeleteAsync(string memoryId, CancellationToken ct = default)
    {
        await this.ExecuteWithRetryAsync<int>(
            async token =>
            {
                try
                {
                    await this.api.DeleteMemoryAsync(memoryId, token);
                    return 0; // dummy return for generic retry helper
                }
                catch (ApiException e)
                {
                    throw MapError(e);
                }
            },
            ct);
    }

    /// <summary>
    /// Not yet implemented. The list memories endpoint is pending server-side implementation.
    /// </summary>
    /// <param name="ct">Cancellation token.</param>
    /// <returns>A <see cref="Task{TResult}"/> resolving to a read-only list of all <see cref="Memory"/> items.</returns>
    public Task<IReadOnlyList<Memory>> GetAllAsync(CancellationToken ct = default)
    {
        throw new NotImplementedException(
            "GetAllAsync() is not yet available. The list memories endpoint is pending server-side implementation.");
    }

    /// <summary>Disposes the underlying <see cref="SDKMemoryApi"/> and its HTTP resources.</summary>
    public void Dispose()
    {
        this.api.Dispose();
        GC.SuppressFinalize(this);
    }

    // ---------------------------------------------------------------------------
    // Static helpers — must appear before non-static member per SA1204
    // ---------------------------------------------------------------------------

    /// <summary>
    /// Computes how long to sleep before the next retry attempt.
    /// When <paramref name="retryAfter"/> is provided by the server, 90% of that value is used
    /// (to account for clock skew) capped at <see cref="RetryCap"/>. Otherwise exponential backoff
    /// with +/-25% jitter is applied, also capped at <see cref="RetryCap"/>.
    /// </summary>
    /// <param name="attempt">The zero-based retry attempt number.</param>
    /// <param name="retryAfter">Seconds from the server Retry-After header, or null if absent.</param>
    /// <returns>The number of seconds to sleep before the next attempt.</returns>
    private static double RetryDelay(int attempt, int? retryAfter)
    {
        if (retryAfter is > 0)
        {
            return Math.Min(retryAfter.Value * 0.9, RetryCap);
        }

        var baseDelay = Math.Min(Math.Pow(2, attempt), RetryCap);
        var jitter = baseDelay * (0.75 + (0.5 * Random.Shared.NextDouble()));
        return Math.Min(jitter, RetryCap);
    }

    /// <summary>
    /// Extracts the Retry-After header value from an <see cref="ApiException"/>.
    /// </summary>
    /// <param name="e">The API exception whose headers to inspect.</param>
    /// <returns>The header value string, or null if not present.</returns>
    private static string? GetRetryAfterHeader(ApiException e)
    {
        if (e.Headers is null)
        {
            return null;
        }

        return e.Headers.TryGetValue("Retry-After", out var values)
            ? values.FirstOrDefault()
            : null;
    }

    /// <summary>
    /// Parses the Retry-After header string into an integer number of seconds.
    /// Returns null if the header is absent or not a valid integer.
    /// </summary>
    /// <param name="header">The raw Retry-After header string, or null if absent.</param>
    /// <returns>The parsed number of seconds, or null if the header is absent or unparseable.</returns>
    private static int? ParseRetryAfter(string? header)
    {
        if (string.IsNullOrEmpty(header))
        {
            return null;
        }

        return int.TryParse(header, out var val) ? val : null;
    }

    /// <summary>
    /// Converts an <see cref="ApiException"/> into the appropriate typed SDK exception.
    /// Matches the Java/Go/Python mapError function exactly.
    /// </summary>
    /// <param name="e">The autogen API exception to convert.</param>
    /// <returns>A typed <see cref="SmriteaException"/> matching the HTTP status code.</returns>
    private static SmriteaException MapError(ApiException e)
    {
        var httpStatus = e.ErrorCode;
        var (message, errorCode, body) = ExtractErrorFieldsWithBody(e.ErrorContent as string);
        return httpStatus switch
        {
            400 => new SmriteaValidationException(message, httpStatus, errorCode, body),
            401 => new SmriteaAuthException(message, httpStatus, errorCode, body),
            402 => new SmriteaQuotaException(message, httpStatus, errorCode, body),
            404 => new SmriteaNotFoundException(message, httpStatus, errorCode, body),
            429 => new SmriteaRateLimitException(message, httpStatus, ParseRetryAfter(GetRetryAfterHeader(e)), errorCode, body),
            _ => new SmriteaException(message, httpStatus, errorCode, body),
        };
    }

    /// <summary>
    /// Parses the JSON response body once and extracts both the human-readable "message"
    /// and machine-readable "code" fields from the server's error payload, along with the full
    /// parsed body. Falls back to ("Unknown error", "INTERNAL_ERROR") if the body is absent or
    /// unparseable. Never returns the raw response body as the message.
    /// </summary>
    /// <param name="body">The raw JSON response body string, possibly null.</param>
    /// <returns>A tuple of (message, errorCode, parsedBody) where parsedBody is null if parsing fails.</returns>
    private static (string Message, string ErrorCode, object? Body) ExtractErrorFieldsWithBody(string? body)
    {
        if (string.IsNullOrEmpty(body))
        {
            return ("Unknown error", "INTERNAL_ERROR", null);
        }

        try
        {
            object? parsedBody = System.Text.Json.JsonSerializer.Deserialize<object>(body);
            var json = System.Text.Json.JsonDocument.Parse(body);
            var root = json.RootElement;

            var message = "Unknown error";
            if (root.TryGetProperty("message", out var messageProp) && messageProp.ValueKind == System.Text.Json.JsonValueKind.String)
            {
                var extracted = messageProp.GetString();
                if (!string.IsNullOrEmpty(extracted))
                {
                    message = extracted;
                }
            }

            var errorCode = "INTERNAL_ERROR";
            if (root.TryGetProperty("code", out var codeProp) && codeProp.ValueKind == System.Text.Json.JsonValueKind.String)
            {
                var extracted = codeProp.GetString();
                if (!string.IsNullOrEmpty(extracted))
                {
                    errorCode = extracted;
                }
            }

            return (message, errorCode, parsedBody);
        }
        catch
        {
            // JSON parsing failed — body is not valid JSON
            return ("Unknown error", "INTERNAL_ERROR", null);
        }
    }

    /// <summary>
    /// Parses the JSON response body once and extracts both the human-readable "message"
    /// and machine-readable "code" fields from the server's error payload.
    /// Falls back to ("Unknown error", "INTERNAL_ERROR") if the body is absent or unparseable.
    /// Never returns the raw response body as the message.
    /// </summary>
    /// <param name="body">The raw JSON response body string, possibly null.</param>
    /// <returns>A tuple of (message, errorCode).</returns>
    private static (string Message, string ErrorCode) ExtractErrorFields(string? body)
    {
        var (message, errorCode, _) = ExtractErrorFieldsWithBody(body);
        return (message, errorCode);
    }

    // ---------------------------------------------------------------------------
    // Retry logic — non-static, comes after static helpers per SA1204
    // ---------------------------------------------------------------------------

    /// <summary>
    /// Executes <paramref name="fn"/> up to <c>maxRetries + 1</c> times, retrying only on
    /// <see cref="SmriteaRateLimitException"/>. Other errors propagate immediately.
    /// After all retries are exhausted, the last <see cref="SmriteaRateLimitException"/> is rethrown.
    /// </summary>
    /// <param name="fn">The async function to execute, receiving a <see cref="CancellationToken"/>.</param>
    /// <param name="ct">Cancellation token forwarded to <paramref name="fn"/> and delay calls.</param>
    /// <returns>A <see cref="Task{TResult}"/> resolving to the result of <paramref name="fn"/>.</returns>
    private async Task<T> ExecuteWithRetryAsync<T>(Func<CancellationToken, Task<T>> fn, CancellationToken ct)
    {
        SmriteaRateLimitException? lastRateLimitEx = null;
        for (var attempt = 0; attempt <= this.maxRetries; attempt++)
        {
            try
            {
                return await fn(ct);
            }
            catch (SmriteaRateLimitException ex)
            {
                lastRateLimitEx = ex;
                if (attempt < this.maxRetries)
                {
                    var delay = RetryDelay(attempt, ex.RetryAfter);
                    await Task.Delay(TimeSpan.FromSeconds(delay), ct);
                }
            }
        }

        // All retries exhausted — rethrow the last rate limit exception so callers
        // can inspect RetryAfter for their own backoff logic.
        throw lastRateLimitEx ?? new SmriteaException("max retries exceeded");
    }
}
