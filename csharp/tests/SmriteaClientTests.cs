using Smritea.Internal.Autogen.Api;
using Smritea.Internal.Autogen.Client;
using WireMock.RequestBuilders;
using WireMock.ResponseBuilders;
using WireMock.Server;
using Xunit;

namespace Smritea.Sdk.Tests;

public class SmriteaClientTests : IDisposable
{
    private readonly WireMockServer _server;

    public SmriteaClientTests()
    {
        _server = WireMockServer.Start();
    }

    public void Dispose()
    {
        _server.Stop();
        _server.Dispose();
    }

    private SmriteaClient CreateClient(int maxRetries = 2)
    {
        var config = new Configuration
        {
            // Use the WireMock server root only — the autogen paths include /api/v1/... already.
            BasePath = _server.Url,
        };
        config.ApiKey["X-API-Key"] = "test-key";
        var api = new SDKMemoryApi(config);
        return new SmriteaClient("app-test", maxRetries, api);
    }

    // -----------------------------------------------------------------------
    // Add
    // -----------------------------------------------------------------------

    [Fact]
    public async Task AddAsync_Success_ReturnsMemory()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"id\":\"mem-1\",\"content\":\"hello world\",\"app_id\":\"app-test\"}"));

        using var client = CreateClient();
        var mem = await client.AddAsync("hello world");

        Assert.NotNull(mem);
        Assert.Equal("mem-1", mem.Id);
        Assert.Equal("hello world", mem.Content);
        Assert.Equal("app-test", mem.AppId);
    }

    [Fact]
    public async Task AddAsync_UserIdShorthand_SetsActorFields()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost()
                .WithBody(b => b != null && b.Contains("\"actor_id\":\"user-42\"") && b.Contains("\"actor_type\":\"user\"")))
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"id\":\"mem-2\",\"content\":\"content\",\"app_id\":\"app-test\"}"));

        using var client = CreateClient();
        var mem = await client.AddAsync("content", new AddOptions().WithUserId("user-42"));

        Assert.NotNull(mem);
        Assert.Equal("mem-2", mem.Id);
    }

    [Fact]
    public async Task AddAsync_ExplicitActorIdAndType()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost()
                .WithBody(b => b != null && b.Contains("\"actor_id\":\"agent-7\"") && b.Contains("\"actor_type\":\"agent\"")))
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"id\":\"mem-3\",\"content\":\"content\",\"app_id\":\"app-test\"}"));

        using var client = CreateClient();
        var mem = await client.AddAsync("content",
            new AddOptions().WithActorId("agent-7").WithActorType("agent"));

        Assert.NotNull(mem);
        Assert.Equal("mem-3", mem.Id);
    }

    // -----------------------------------------------------------------------
    // Search
    // -----------------------------------------------------------------------

    [Fact]
    public async Task SearchAsync_Success_ReturnsList()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories/search").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"memories\":[{\"memory\":{\"id\":\"mem-1\",\"content\":\"found\"},\"score\":0.9}]}"));

        using var client = CreateClient();
        var results = await client.SearchAsync("found");

        Assert.NotNull(results);
        Assert.Single(results);
        Assert.Equal("mem-1", results[0].Memory!.Id);
        Assert.Equal("found", results[0].Memory!.Content);
        Assert.Equal(0.9m, results[0].Score!, 3);
    }

    [Fact]
    public async Task SearchAsync_EmptyResult_ReturnsEmptyList()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories/search").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"memories\":[]}"));

        using var client = CreateClient();
        var results = await client.SearchAsync("nothing");

        Assert.NotNull(results);
        Assert.Empty(results);
    }

    [Fact]
    public async Task SearchAsync_TemporalFilters_SentInRequest()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories/search").UsingPost()
                .WithBody(b => b != null
                    && b.Contains("\"from_time\":\"2024-01-01T00:00:00Z\"")
                    && b.Contains("\"to_time\":\"2024-12-31T23:59:59Z\"")
                    && b.Contains("\"valid_at\":\"2024-06-15T12:00:00Z\"")))
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"memories\":[{\"memory\":{\"id\":\"mem-t1\",\"content\":\"temporal\"},\"score\":0.8}]}"));

        using var client = CreateClient();
        var opts = new SearchOptions()
            .WithFromTime("2024-01-01T00:00:00Z")
            .WithToTime("2024-12-31T23:59:59Z")
            .WithValidAt("2024-06-15T12:00:00Z");
        var results = await client.SearchAsync("temporal", opts);

        Assert.NotNull(results);
        Assert.Single(results);
        Assert.Equal("mem-t1", results[0].Memory!.Id);
    }

    // -----------------------------------------------------------------------
    // Get
    // -----------------------------------------------------------------------

    [Fact]
    public async Task GetAsync_Success_ReturnsMemory()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories/mem-99").UsingGet())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"id\":\"mem-99\",\"content\":\"stored content\",\"app_id\":\"app-test\"}"));

        using var client = CreateClient();
        var mem = await client.GetAsync("mem-99");

        Assert.NotNull(mem);
        Assert.Equal("mem-99", mem.Id);
        Assert.Equal("stored content", mem.Content);
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    [Fact]
    public async Task DeleteAsync_Success_NoException()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories/mem-99").UsingDelete())
            .RespondWith(Response.Create().WithStatusCode(204));

        using var client = CreateClient();
        await client.DeleteAsync("mem-99");
    }

    // -----------------------------------------------------------------------
    // Error mapping
    // -----------------------------------------------------------------------

    [Fact]
    public async Task AddAsync_401_ThrowsSmriteaAuthException()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(401)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"detail\":\"invalid api key\"}"));

        using var client = CreateClient();
        var ex = await Assert.ThrowsAsync<SmriteaAuthException>(
            () => client.AddAsync("x"));

        Assert.Equal(401, ex.StatusCode);
    }

    [Fact]
    public async Task SearchAsync_404_ThrowsSmriteaNotFoundException()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories/search").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(404)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"detail\":\"not found\"}"));

        using var client = CreateClient();
        await Assert.ThrowsAsync<SmriteaNotFoundException>(
            () => client.SearchAsync("x"));
    }

    [Fact]
    public async Task AddAsync_400_ThrowsSmriteaValidationException()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(400)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"detail\":\"content required\"}"));

        using var client = CreateClient();
        await Assert.ThrowsAsync<SmriteaValidationException>(
            () => client.AddAsync(""));
    }

    [Fact]
    public async Task AddAsync_402_ThrowsSmriteaQuotaException()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(402)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"detail\":\"quota exceeded\"}"));

        using var client = CreateClient();
        await Assert.ThrowsAsync<SmriteaQuotaException>(
            () => client.AddAsync("x"));
    }

    // -----------------------------------------------------------------------
    // Retry behaviour
    // -----------------------------------------------------------------------

    [Fact]
    public async Task AddAsync_429_RetryThenSuccess()
    {
        // First stub has no WhenStateIs() — it matches in ANY state (including whatever
        // the initial scenario state is in this WireMock.Net version).  WillSetStateTo
        // transitions the scenario to "retried" so that subsequent requests hit stub 2.
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost())
            .InScenario("retry")
            .WillSetStateTo("retried")
            .RespondWith(Response.Create().WithStatusCode(429)
                .WithHeader("Retry-After", "1")
                .WithBody("{\"detail\":\"rate limited\"}"));
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost())
            .InScenario("retry")
            .WhenStateIs("retried")
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"id\":\"mem-ok\",\"content\":\"ok\",\"app_id\":\"app-test\"}"));

        using var client = CreateClient();
        var mem = await client.AddAsync("x");

        Assert.Equal("mem-ok", mem.Id);
    }

    [Fact]
    public async Task AddAsync_429_Exhausted_ThrowsSmriteaRateLimitException()
    {
        // maxRetries=2 means 3 total attempts; all return 429 -> SmriteaRateLimitException
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(429)
                .WithHeader("Retry-After", "1")
                .WithBody("{\"detail\":\"rate limited\"}"));

        using var client = CreateClient();
        var ex = await Assert.ThrowsAsync<SmriteaRateLimitException>(
            () => client.AddAsync("x"));

        Assert.NotNull(ex.RetryAfter);
        Assert.Equal(1, ex.RetryAfter);
    }

    // -----------------------------------------------------------------------
    // Deserialization error
    // -----------------------------------------------------------------------

    [Fact]
    public async Task AddAsync_NullBody_ThrowsSmriteaDeserializationException()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories").UsingPost())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("null"));

        using var client = CreateClient();
        await Assert.ThrowsAsync<SmriteaDeserializationException>(
            () => client.AddAsync("x"));
    }

    [Fact]
    public async Task GetAsync_NullBody_ThrowsSmriteaDeserializationException()
    {
        _server.Given(Request.Create().WithPath("/api/v1/sdk/memories/mem-null").UsingGet())
            .RespondWith(Response.Create().WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody("null"));

        using var client = CreateClient();
        await Assert.ThrowsAsync<SmriteaDeserializationException>(
            () => client.GetAsync("mem-null"));
    }

    // -----------------------------------------------------------------------
    // GetAll -- not yet implemented
    // -----------------------------------------------------------------------

    [Fact]
    public async Task GetAllAsync_ThrowsNotImplementedException()
    {
        using var client = CreateClient();
        await Assert.ThrowsAsync<NotImplementedException>(
            () => client.GetAllAsync());
    }
}
