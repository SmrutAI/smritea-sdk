using System.Net;
using WireMock.RequestBuilders;
using WireMock.ResponseBuilders;
using WireMock.Server;
using Xunit;

namespace Smritea.Sdk.Tests;

public class SmriteaHttpClientTests : IDisposable
{
    private const string NotFoundBody = "{\"code\":\"MEMORY_NOT_FOUND\",\"message\":\"Memory not found\"}";
    private const string MemoryPath = "/api/v1/sdk/memories/mem_1";

    private readonly WireMockServer _server;

    public SmriteaHttpClientTests()
    {
        _server = WireMockServer.Start();
        _server
            .Given(Request.Create().WithPath(MemoryPath).UsingGet())
            .RespondWith(Response.Create()
                .WithStatusCode(404)
                .WithHeader("Content-Type", "application/json")
                .WithBody(NotFoundBody));
    }

    public void Dispose()
    {
        _server.Stop();
        _server.Dispose();
    }

    public static TheoryData<string, bool, string?> Cases => new()
    {
        { "custom client without trace id", true, null },
        { "custom client with trace id", true, "trace-abc12345" },
        { "null client uses a default client", false, null },
    };

    [Theory]
    [MemberData(nameof(Cases))]
    public async Task SendsThroughTheGivenClient(string name, bool custom, string? traceId)
    {
        var handler = new CountingHandler();
        using var httpClient = new HttpClient(handler);
        using var client = new SmriteaClient("test-key", "app-test", _server.Url, 0, traceId, custom ? httpClient : null);

        await Assert.ThrowsAsync<SmriteaNotFoundException>(() => client.GetAsync("mem_1"));

        Assert.True((custom ? 1 : 0) == handler.Count, name);
        var entry = Assert.Single(_server.LogEntries);
        Assert.Equal("test-key", entry.RequestMessage!.Headers!["X-API-Key"].First());
        if (traceId is not null)
        {
            Assert.Equal(traceId, entry.RequestMessage!.Headers!["X-Trace-ID"].First());
        }
        else
        {
            Assert.False(entry.RequestMessage!.Headers!.ContainsKey("X-Trace-ID"), name);
        }
    }

    [Fact]
    public async Task DisposeLeavesTheGivenClientOpen()
    {
        using var httpClient = new HttpClient();
        var client = new SmriteaClient("test-key", "app-test", _server.Url, 0, null, httpClient);
        client.Dispose();

        var response = await httpClient.GetAsync(_server.Url + MemoryPath);

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    private sealed class CountingHandler : DelegatingHandler
    {
        private int _count;

        public CountingHandler()
            : base(new HttpClientHandler())
        {
        }

        public int Count => _count;

        protected override Task<HttpResponseMessage> SendAsync(HttpRequestMessage request, CancellationToken cancellationToken)
        {
            Interlocked.Increment(ref _count);
            return base.SendAsync(request, cancellationToken);
        }
    }
}
