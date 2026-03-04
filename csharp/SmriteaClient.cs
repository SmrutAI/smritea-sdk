namespace Smritea.Sdk;

using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;

/// <summary>
/// Entry point for all smritea memory operations.
/// Create one instance per application and reuse it across calls.
/// Implements <see cref="IDisposable"/> to release the underlying <see cref="HttpClient"/>
/// when the client owns it.
/// </summary>
public class SmriteaClient : IDisposable
{
    private const string DefaultBaseUrl = "https://api.smritea.ai";
    private const int DefaultMaxRetries = 2;
    private const double RetryCap = 30.0;

    private readonly string _appId;
    private readonly int _maxRetries;
    private readonly HttpClient _httpClient;
    private readonly JsonSerializerOptions _jsonOptions;
    private readonly bool _ownsHttpClient;

    /// <summary>
    /// Creates a new <see cref="SmriteaClient"/> that owns its own <see cref="HttpClient"/>.
    /// </summary>
    /// <param name="apiKey">API key for authentication.</param>
    /// <param name="appId">Application ID injected into every request.</param>
    /// <param name="baseUrl">Base URL of the smritea API. Defaults to <c>https://api.smritea.ai</c>.</param>
    /// <param name="maxRetries">Maximum number of retries on HTTP 429. Defaults to 2.</param>
    public SmriteaClient(string apiKey, string appId, string? baseUrl = null, int maxRetries = DefaultMaxRetries)
    {
        ArgumentNullException.ThrowIfNull(apiKey);
        ArgumentNullException.ThrowIfNull(appId);
        _appId = appId;
        _maxRetries = Math.Max(0, maxRetries);
        _ownsHttpClient = true;
        _httpClient = new HttpClient { BaseAddress = new Uri(baseUrl ?? DefaultBaseUrl) };
        _httpClient.DefaultRequestHeaders.Add("X-API-Key", apiKey);
        _jsonOptions = new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        };
    }

    /// <summary>
    /// Creates a new <see cref="SmriteaClient"/> with an externally managed <see cref="HttpClient"/>.
    /// Useful for testing with WireMock or custom HTTP pipelines.
    /// The caller is responsible for disposing the <paramref name="httpClient"/>.
    /// </summary>
    /// <param name="apiKey">API key for authentication.</param>
    /// <param name="appId">Application ID injected into every request.</param>
    /// <param name="httpClient">Pre-configured HTTP client.</param>
    /// <param name="maxRetries">Maximum number of retries on HTTP 429. Defaults to 2.</param>
    public SmriteaClient(string apiKey, string appId, HttpClient httpClient, int maxRetries = DefaultMaxRetries)
    {
        ArgumentNullException.ThrowIfNull(apiKey);
        ArgumentNullException.ThrowIfNull(appId);
        ArgumentNullException.ThrowIfNull(httpClient);
        _appId = appId;
        _maxRetries = Math.Max(0, maxRetries);
        _ownsHttpClient = false;
        _httpClient = httpClient;
        _httpClient.DefaultRequestHeaders.TryAddWithoutValidation("X-API-Key", apiKey);
        _jsonOptions = new JsonSerializerOptions
        {
            PropertyNamingPolicy = JsonNamingPolicy.SnakeCaseLower,
            DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        };
    }

    /// <summary>
    /// Stores a new memory with the given content. The optional <paramref name="opts"/>
    /// control which actor the memory is attributed to, metadata, and conversation scoping.
    /// When <see cref="AddOptions.UserId"/> is set it takes precedence over
    /// <see cref="AddOptions.ActorId"/> and forces actor_type="user".
    /// </summary>
    public async Task<Memory> AddAsync(string content, AddOptions? opts = null, CancellationToken ct = default)
    {
        var body = new Dictionary<string, object?> { ["app_id"] = _appId, ["content"] = content };

        if (opts is not null)
        {
            var actorId = opts.UserId ?? opts.ActorId;
            var actorType = opts.UserId is not null ? "user" : opts.ActorType;

            if (actorId is not null) body["actor_id"] = actorId;
            if (actorType is not null) body["actor_type"] = actorType;
            if (opts.ActorName is not null) body["actor_name"] = opts.ActorName;
            if (opts.Metadata is not null) body["metadata"] = opts.Metadata;
            if (opts.ConversationId is not null) body["conversation_id"] = opts.ConversationId;
        }

        return await ExecuteWithRetryAsync(async token =>
        {
            var resp = await SendRequestAsync(HttpMethod.Post, "/api/v1/sdk/memories", body, token);
            var respBody = await resp.Content.ReadAsStringAsync(token);
            if (!resp.IsSuccessStatusCode)
                throw MapError((int)resp.StatusCode, respBody, GetRetryAfterHeader(resp));
            return JsonSerializer.Deserialize<Memory>(respBody, _jsonOptions)
                   ?? throw new SmriteaException("Failed to deserialize memory response");
        }, ct);
    }

    /// <summary>
    /// Retrieves memories ranked by relevance to the given query.
    /// Returns an empty list when no memories match.
    /// </summary>
    public async Task<IReadOnlyList<SearchResult>> SearchAsync(string query, SearchOptions? opts = null, CancellationToken ct = default)
    {
        var body = new Dictionary<string, object?> { ["app_id"] = _appId, ["query"] = query };

        if (opts is not null)
        {
            var actorId = opts.UserId ?? opts.ActorId;
            var actorType = opts.UserId is not null ? "user" : opts.ActorType;

            if (actorId is not null) body["actor_id"] = actorId;
            if (actorType is not null) body["actor_type"] = actorType;
            if (opts.Limit is not null) body["limit"] = opts.Limit;
            if (opts.Method is not null) body["method"] = opts.Method;
            if (opts.Threshold is not null) body["threshold"] = opts.Threshold;
            if (opts.GraphDepth is not null) body["graph_depth"] = opts.GraphDepth;
            if (opts.ConversationId is not null) body["conversation_id"] = opts.ConversationId;
        }

        return await ExecuteWithRetryAsync(async token =>
        {
            var resp = await SendRequestAsync(HttpMethod.Post, "/api/v1/sdk/memories/search", body, token);
            var respBody = await resp.Content.ReadAsStringAsync(token);
            if (!resp.IsSuccessStatusCode)
                throw MapError((int)resp.StatusCode, respBody, GetRetryAfterHeader(resp));

            using var doc = JsonDocument.Parse(respBody);
            if (!doc.RootElement.TryGetProperty("memories", out var memoriesEl))
                return Array.Empty<SearchResult>();

            var results = new List<SearchResult>();
            foreach (var item in memoriesEl.EnumerateArray())
            {
                Memory? mem = null;
                if (item.TryGetProperty("memory", out var memEl))
                    mem = JsonSerializer.Deserialize<Memory>(memEl.GetRawText(), _jsonOptions);
                double? score = item.TryGetProperty("score", out var scoreEl) ? scoreEl.GetDouble() : null;
                results.Add(new SearchResult { Memory = mem, Score = score });
            }
            return results;
        }, ct);
    }

    /// <summary>
    /// Fetches a single memory by its ID.
    /// Throws <see cref="SmriteaNotFoundException"/> when the memory does not exist.
    /// </summary>
    public async Task<Memory> GetAsync(string memoryId, CancellationToken ct = default)
    {
        return await ExecuteWithRetryAsync(async token =>
        {
            var resp = await SendRequestAsync(HttpMethod.Get, $"/api/v1/sdk/memories/{memoryId}", null, token);
            var respBody = await resp.Content.ReadAsStringAsync(token);
            if (!resp.IsSuccessStatusCode)
                throw MapError((int)resp.StatusCode, respBody, GetRetryAfterHeader(resp));
            return JsonSerializer.Deserialize<Memory>(respBody, _jsonOptions)
                   ?? throw new SmriteaException("Failed to deserialize memory response");
        }, ct);
    }

    /// <summary>
    /// Permanently removes a memory by its ID.
    /// Throws <see cref="SmriteaNotFoundException"/> when the memory does not exist.
    /// </summary>
    public async Task DeleteAsync(string memoryId, CancellationToken ct = default)
    {
        await ExecuteWithRetryAsync(async token =>
        {
            var resp = await SendRequestAsync(HttpMethod.Delete, $"/api/v1/sdk/memories/{memoryId}", null, token);
            if (!resp.IsSuccessStatusCode)
            {
                var respBody = await resp.Content.ReadAsStringAsync(token);
                throw MapError((int)resp.StatusCode, respBody, GetRetryAfterHeader(resp));
            }
            return 0; // dummy return for generic retry helper
        }, ct);
    }

    /// <summary>
    /// Not yet implemented. The list memories endpoint is pending server-side implementation.
    /// </summary>
    public Task<IReadOnlyList<Memory>> GetAllAsync(CancellationToken ct = default)
    {
        throw new NotImplementedException(
            "GetAllAsync() is not yet available. The list memories endpoint is pending server-side implementation.");
    }

    /// <summary>Disposes the underlying <see cref="HttpClient"/> if this client owns it.</summary>
    public void Dispose()
    {
        if (_ownsHttpClient) _httpClient.Dispose();
        GC.SuppressFinalize(this);
    }

    /// <summary>
    /// Executes <paramref name="fn"/> up to <c>_maxRetries + 1</c> times, retrying only on
    /// <see cref="SmriteaRateLimitException"/>. Other errors propagate immediately.
    /// </summary>
    private async Task<T> ExecuteWithRetryAsync<T>(Func<CancellationToken, Task<T>> fn, CancellationToken ct)
    {
        for (var attempt = 0; attempt <= _maxRetries; attempt++)
        {
            try
            {
                return await fn(ct);
            }
            catch (SmriteaRateLimitException ex) when (attempt < _maxRetries)
            {
                var delay = RetryDelay(attempt, ex.RetryAfter);
                await Task.Delay(TimeSpan.FromSeconds(delay), ct);
            }
        }
        throw new SmriteaException("max retries exceeded");
    }

    /// <summary>
    /// Computes how long to sleep before the next retry attempt.
    /// When <paramref name="retryAfter"/> is provided by the server, 90% of that value is used
    /// (to account for clock skew) capped at <see cref="RetryCap"/>. Otherwise exponential backoff
    /// with +/-25% jitter is applied, also capped at <see cref="RetryCap"/>.
    /// </summary>
    private static double RetryDelay(int attempt, int? retryAfter)
    {
        if (retryAfter is > 0)
        {
            return Math.Min(retryAfter.Value * 0.9, RetryCap);
        }
        var baseDelay = Math.Min(Math.Pow(2, attempt), RetryCap);
        var jitter = baseDelay * (0.75 + 0.5 * Random.Shared.NextDouble());
        return Math.Min(jitter, RetryCap);
    }

    /// <summary>
    /// Maps an HTTP status code to the appropriate typed SDK exception.
    /// </summary>
    private static SmriteaException MapError(int statusCode, string body, string? retryAfterHeader)
    {
        return statusCode switch
        {
            400 => new SmriteaValidationException(body, statusCode),
            401 => new SmriteaAuthException(body, statusCode),
            402 => new SmriteaQuotaException(body, statusCode),
            404 => new SmriteaNotFoundException(body, statusCode),
            429 => new SmriteaRateLimitException(body, statusCode, ParseRetryAfter(retryAfterHeader)),
            _ => new SmriteaException(body, statusCode),
        };
    }

    /// <summary>
    /// Sends an HTTP request with optional JSON body.
    /// </summary>
    private async Task<HttpResponseMessage> SendRequestAsync(
        HttpMethod method, string path, Dictionary<string, object?>? body, CancellationToken ct)
    {
        using var request = new HttpRequestMessage(method, path);
        if (body is not null)
        {
            var json = JsonSerializer.Serialize(body, _jsonOptions);
            request.Content = new StringContent(json, Encoding.UTF8, "application/json");
        }
        return await _httpClient.SendAsync(request, ct);
    }

    /// <summary>
    /// Extracts the Retry-After header value as a string, or null if absent.
    /// </summary>
    private static string? GetRetryAfterHeader(HttpResponseMessage resp)
    {
        return resp.Headers.RetryAfter?.Delta?.TotalSeconds is double secs
            ? ((int)secs).ToString()
            : null;
    }

    /// <summary>
    /// Parses the Retry-After header string into an integer number of seconds.
    /// Returns null if the header is absent or not a valid integer.
    /// </summary>
    private static int? ParseRetryAfter(string? header)
    {
        if (string.IsNullOrEmpty(header)) return null;
        return int.TryParse(header, out var val) ? val : null;
    }
}
