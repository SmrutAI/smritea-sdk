using System.Diagnostics;
using Smritea.Internal.Autogen.Model;
using WireMock.RequestBuilders;
using WireMock.ResponseBuilders;
using WireMock.Server;
using Xunit;

namespace Smritea.Sdk.Tests;

public class SmriteaTimeoutTests : IDisposable
{
    private const string AppId = "app-test";
    private const string ApiKey = "test-key";
    private const string SdkTimeoutMarker = "was timeout";
    private const string HttpClientTimeoutMarker = "HttpClient.Timeout";
    private const string RetryMemoryPath = "/api/v1/sdk/memories/mem_1";
    private const string RetryScenario = "retry-budget";
    private const string SecondExchangeState = "second-exchange";
    private const int PreferredPriority = 1;
    private const int FallbackPriority = 10;

    private static readonly TimeSpan ShortSdkTimeout = TimeSpan.FromMilliseconds(400);
    private static readonly TimeSpan SuppliedClientTimeout = TimeSpan.FromMilliseconds(1200);
    private static readonly TimeSpan SlackAfterTimeout = TimeSpan.FromSeconds(3);
    private static readonly TimeSpan RetryBudget = TimeSpan.FromMilliseconds(1500);
    private static readonly TimeSpan RetryExchangeDelay = TimeSpan.FromMilliseconds(800);

    private readonly WireMockServer _wireMock = WireMockServer.Start();

    public enum Operation
    {
        Add,
        Search,
        Get,
        Delete,
    }

    public static TheoryData<StallMode, Operation> Stalls
    {
        get
        {
            var data = new TheoryData<StallMode, Operation>();
            foreach (var mode in Enum.GetValues<StallMode>())
            {
                foreach (var operation in Enum.GetValues<Operation>())
                {
                    data.Add(mode, operation);
                }
            }

            return data;
        }
    }

    public static TheoryData<string, Func<SmriteaClient>> DefaultClients => new()
    {
        { "api key and app id only", () => new SmriteaClient(ApiKey, AppId) },
        { "with base url", () => new SmriteaClient(ApiKey, AppId, "http://127.0.0.1:1") },
        { "with trace id", () => new SmriteaClient(ApiKey, AppId, "http://127.0.0.1:1", 2, "trace-abc12345") },
        { "six arguments with a null client", () => new SmriteaClient(ApiKey, AppId, "http://127.0.0.1:1", 2, null, null) },
    };

    public static TheoryData<string, bool, TimeSpan> ResolveCases => new()
    {
        { "no client: the SDK timeout applies", false, ShortSdkTimeout },
        { "caller client: no SDK timeout", true, Timeout.InfiniteTimeSpan },
    };

    public void Dispose()
    {
        _wireMock.Stop();
        _wireMock.Dispose();
    }

    [Fact]
    public void DefaultTimeoutIsTenSeconds()
    {
        Assert.Equal(TimeSpan.FromSeconds(10), SmriteaClient.DefaultTimeout);
    }

    [Theory]
    [MemberData(nameof(DefaultClients))]
    public void PublicConstructorsWithoutAClientUseTheDefaultTimeout(string name, Func<SmriteaClient> create)
    {
        using var client = create();

        Assert.True(SmriteaClient.DefaultTimeout == client.RequestTimeout, name);
    }

    [Fact]
    public void PublicConstructorWithAClientSetsNoSdkTimeout()
    {
        using var httpClient = new HttpClient();
        using var client = new SmriteaClient(ApiKey, AppId, "http://127.0.0.1:1", 2, null, httpClient);

        Assert.Equal(Timeout.InfiniteTimeSpan, client.RequestTimeout);
    }

    [Theory]
    [MemberData(nameof(ResolveCases))]
    public void ResolveTimeoutChoosesByWhoOwnsTheClient(string name, bool supplied, TimeSpan expected)
    {
        using var httpClient = new HttpClient();

        var actual = SmriteaClient.ResolveTimeout(supplied ? httpClient : null, ShortSdkTimeout);

        Assert.True(expected == actual, name);
    }

    [Theory]
    [MemberData(nameof(Stalls))]
    public async Task SdkDefaultClientFailsWithinTheBudget(StallMode mode, Operation operation)
    {
        using var server = new StallServer(mode);
        using var client = new SmriteaClient(ApiKey, AppId, server.Url, 0, null, null, ShortSdkTimeout);

        var watch = Stopwatch.StartNew();
        var ex = await Assert.ThrowsAsync<TaskCanceledException>(() => Invoke(client, operation));
        watch.Stop();

        Assert.Contains(SdkTimeoutMarker, ex.Message, StringComparison.Ordinal);
        Assert.IsType<TimeoutException>(ex.InnerException);
        Assert.True(watch.Elapsed >= ShortSdkTimeout - TimeSpan.FromMilliseconds(100), $"failed too early: {watch.Elapsed}");
        Assert.True(watch.Elapsed < ShortSdkTimeout + SlackAfterTimeout, $"failed too late: {watch.Elapsed}");
    }

    [Theory]
    [MemberData(nameof(Stalls))]
    public async Task SuppliedClientGetsNoSdkTimeoutAndItsOwnLimitFires(StallMode mode, Operation operation)
    {
        using var server = new StallServer(mode);
        using var httpClient = new HttpClient { Timeout = SuppliedClientTimeout };
        using var client = new SmriteaClient(ApiKey, AppId, server.Url, 0, null, httpClient, ShortSdkTimeout);

        var watch = Stopwatch.StartNew();
        var ex = await Assert.ThrowsAsync<TaskCanceledException>(() => Invoke(client, operation));
        watch.Stop();

        Assert.Equal(Timeout.InfiniteTimeSpan, client.RequestTimeout);
        Assert.DoesNotContain(SdkTimeoutMarker, ex.Message, StringComparison.Ordinal);
        Assert.Contains(HttpClientTimeoutMarker, ex.Message, StringComparison.Ordinal);
        Assert.True(
            watch.Elapsed >= SuppliedClientTimeout - TimeSpan.FromMilliseconds(100),
            $"the SDK cut the supplied client off after {watch.Elapsed}");
    }

    [Theory]
    [MemberData(nameof(Stalls))]
    public async Task CallerCancellationTokenIsTheOnlyLimitOfASuppliedClient(StallMode mode, Operation operation)
    {
        using var server = new StallServer(mode);
        using var httpClient = new HttpClient { Timeout = Timeout.InfiniteTimeSpan };
        using var client = new SmriteaClient(ApiKey, AppId, server.Url, 0, null, httpClient, ShortSdkTimeout);
        using var cts = new CancellationTokenSource(SuppliedClientTimeout);

        var watch = Stopwatch.StartNew();
        var ex = await Assert.ThrowsAnyAsync<OperationCanceledException>(() => Invoke(client, operation, cts.Token));
        watch.Stop();

        Assert.DoesNotContain(SdkTimeoutMarker, ex.Message, StringComparison.Ordinal);
        Assert.True(
            watch.Elapsed >= SuppliedClientTimeout - TimeSpan.FromMilliseconds(100),
            $"the SDK cut the supplied client off after {watch.Elapsed}");
    }

    [Fact]
    public async Task RetryAfterRateLimitStartsANewExchangeWithAFreshBudget()
    {
        var memoryJson = new MemoryResponse(id: "mem_1", content: "hello", appId: AppId).ToJson();
        _wireMock
            .Given(Request.Create().WithPath(RetryMemoryPath).UsingGet())
            .AtPriority(FallbackPriority)
            .InScenario(RetryScenario)
            .WillSetStateTo(SecondExchangeState)
            .RespondWith(Response.Create()
                .WithStatusCode(429)
                .WithHeader("Retry-After", "1")
                .WithHeader("Content-Type", "application/json")
                .WithBody("{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"slow down\"}")
                .WithDelay(RetryExchangeDelay));
        _wireMock
            .Given(Request.Create().WithPath(RetryMemoryPath).UsingGet())
            .AtPriority(PreferredPriority)
            .InScenario(RetryScenario)
            .WhenStateIs(SecondExchangeState)
            .RespondWith(Response.Create()
                .WithStatusCode(200)
                .WithHeader("Content-Type", "application/json")
                .WithBody(memoryJson)
                .WithDelay(RetryExchangeDelay));
        using var client = new SmriteaClient(ApiKey, AppId, _wireMock.Url, 1, null, null, RetryBudget);

        var watch = Stopwatch.StartNew();
        var memory = await client.GetAsync("mem_1");
        watch.Stop();

        Assert.Equal("mem_1", memory.Id);
        Assert.Equal(2, _wireMock.LogEntries.Count());

        // Each exchange stays inside the budget, but the exchanges plus the retry sleep exceed it:
        // the budget is per exchange, and the sleep is not counted.
        Assert.True(watch.Elapsed > RetryBudget, $"the call took {watch.Elapsed}, so the budget was shared");
    }

    private static Task Invoke(SmriteaClient client, Operation operation, CancellationToken ct = default)
    {
        return operation switch
        {
            Operation.Add => client.AddAsync("hello", ct: ct),
            Operation.Search => client.SearchAsync("hello", ct: ct),
            Operation.Get => client.GetAsync("mem_1", ct),
            Operation.Delete => client.DeleteAsync("mem_1", ct),
            _ => throw new ArgumentOutOfRangeException(nameof(operation), operation, "unknown operation"),
        };
    }
}
