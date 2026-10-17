using WireMock.RequestBuilders;
using WireMock.ResponseBuilders;
using WireMock.Server;
using Xunit;

namespace Smritea.Sdk.Tests;

/// <summary>
/// Covers the X-Trace-ID request header (set through the public constructor) and the
/// RequestId value populated on mapped HTTP errors (body request_id first, then X-Request-ID header).
/// </summary>
public class SmriteaTraceRequestIdTests : IDisposable
{
    private const string MemoriesPath = "/api/v1/sdk/memories";
    private const string SuccessBody = "{\"memories\":[{\"id\":\"mem-1\",\"content\":\"x\",\"app_id\":\"app-test\"}]}";

    private readonly WireMockServer _server;

    public SmriteaTraceRequestIdTests()
    {
        _server = WireMockServer.Start();
    }

    public void Dispose()
    {
        _server.Stop();
        _server.Dispose();
    }

    // -----------------------------------------------------------------------
    // Data sources
    // -----------------------------------------------------------------------

    public static TheoryData<string> ValidTraceIds => new()
    {
        "abcdefgh",
        new string('a', 100),
        "a.b_c:d-e",
    };

    public static TheoryData<string> InvalidTraceIds => new()
    {
        "abcdefg",
        new string('a', 101),
        "abc def ghi",
    };

    public static TheoryData<int> ErrorStatuses => new()
    {
        400, 401, 402, 403, 404, 409, 422, 429, 500,
    };

    public static TheoryData<int, string> ErrorStatusesWithHeaderName => new()
    {
        { 400, "X-Request-ID" }, { 400, "x-request-id" },
        { 401, "X-Request-ID" }, { 401, "x-request-id" },
        { 402, "X-Request-ID" }, { 402, "x-request-id" },
        { 403, "X-Request-ID" }, { 403, "x-request-id" },
        { 404, "X-Request-ID" }, { 404, "x-request-id" },
        { 409, "X-Request-ID" }, { 409, "x-request-id" },
        { 422, "X-Request-ID" }, { 422, "x-request-id" },
        { 429, "X-Request-ID" }, { 429, "x-request-id" },
        { 500, "X-Request-ID" }, { 500, "x-request-id" },
    };

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private SmriteaClient CreateClient(string? traceId = null) =>
        new SmriteaClient("test-key", "app-test", _server.Url, maxRetries: 0, traceId: traceId);

    private void StubAddMemory(int status, string body, string? headerName = null, string? headerValue = null)
    {
        var response = Response.Create()
            .WithStatusCode(status)
            .WithHeader("Content-Type", "application/json")
            .WithBody(body);

        if (headerName is not null && headerValue is not null)
        {
            response = response.WithHeader(headerName, headerValue);
        }

        _server.Given(Request.Create().WithPath(MemoriesPath).UsingPost())
            .RespondWith(response);
    }

    private IEnumerable<string> ReceivedRequestHeaderValues(string headerName) =>
        _server.LogEntries
            .SelectMany(entry => entry.RequestMessage.Headers)
            .Where(header => string.Equals(header.Key, headerName, StringComparison.OrdinalIgnoreCase))
            .SelectMany(header => header.Value);

    // -----------------------------------------------------------------------
    // 1. traceId header
    // -----------------------------------------------------------------------

    [Theory]
    [MemberData(nameof(ValidTraceIds))]
    public async Task Constructor_ValidTraceId_SendsXTraceIdHeader(string traceId)
    {
        StubAddMemory(200, SuccessBody);

        using var client = CreateClient(traceId);
        await client.AddAsync("x");

        Assert.Single(_server.LogEntries);
        Assert.Equal(traceId, Assert.Single(ReceivedRequestHeaderValues("X-Trace-ID")));
    }

    [Fact]
    public async Task Constructor_NullTraceId_SendsNoXTraceIdHeader()
    {
        StubAddMemory(200, SuccessBody);

        using var client = CreateClient(traceId: null);
        await client.AddAsync("x");

        Assert.Single(_server.LogEntries);
        Assert.Empty(ReceivedRequestHeaderValues("X-Trace-ID"));
    }

    // -----------------------------------------------------------------------
    // 2. invalid traceId
    // -----------------------------------------------------------------------

    [Theory]
    [MemberData(nameof(InvalidTraceIds))]
    public void Constructor_InvalidTraceId_ThrowsArgumentException_AndSendsNoRequests(string traceId)
    {
        Assert.Throws<ArgumentException>(() => CreateClient(traceId));

        Assert.Empty(_server.LogEntries);
    }

    // -----------------------------------------------------------------------
    // 3. RequestId on mapped errors
    // -----------------------------------------------------------------------

    [Theory]
    [MemberData(nameof(ErrorStatuses))]
    public async Task MappedError_BodyRequestId_IsUsedAsRequestId(int status)
    {
        // The X-Request-ID header carries a different value to prove the body value takes precedence.
        var bodyRequestId = $"req-body-{status}";
        var body = $"{{\"detail\":\"boom\",\"request_id\":\"{bodyRequestId}\"}}";
        StubAddMemory(status, body, "X-Request-ID", $"req-header-{status}");

        using var client = CreateClient();
        var ex = await Assert.ThrowsAnyAsync<SmriteaException>(() => client.AddAsync("x"));

        Assert.Equal(status, ex.HTTPStatus);
        Assert.Equal(bodyRequestId, ex.RequestId);
    }

    [Theory]
    [MemberData(nameof(ErrorStatusesWithHeaderName))]
    public async Task MappedError_NoBodyRequestId_UsesXRequestIdHeader(int status, string headerName)
    {
        var headerValue = $"req-header-{status}";
        StubAddMemory(status, "{\"detail\":\"boom\"}", headerName, headerValue);

        using var client = CreateClient();
        var ex = await Assert.ThrowsAnyAsync<SmriteaException>(() => client.AddAsync("x"));

        Assert.Equal(status, ex.HTTPStatus);
        Assert.Equal(headerValue, ex.RequestId);
    }

    [Theory]
    [MemberData(nameof(ErrorStatuses))]
    public async Task MappedError_NoRequestIdAnywhere_RequestIdIsNull(int status)
    {
        StubAddMemory(status, "{\"detail\":\"boom\"}");

        using var client = CreateClient();
        var ex = await Assert.ThrowsAnyAsync<SmriteaException>(() => client.AddAsync("x"));

        Assert.Equal(status, ex.HTTPStatus);
        Assert.Null(ex.RequestId);
    }
}
