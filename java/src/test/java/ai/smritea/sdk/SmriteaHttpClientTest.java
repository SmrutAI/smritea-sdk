package ai.smritea.sdk;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import ai.smritea.sdk.errors.SmriteaNotFoundError;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.IOException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Tests of the HTTP client option of {@link SmriteaClient}. */
@WireMockTest
class SmriteaHttpClientTest {

  private static final String NOT_FOUND_BODY =
      "{\"code\":\"MEMORY_NOT_FOUND\",\"message\":\"Memory not found\"}";
  private static final String PATH = "/api/v1/sdk/memories/mem_1";

  /** An HTTP client that records every request and passes it to a delegate. */
  private static final class CountingHttpClient extends HttpClient {
    private final HttpClient delegate;
    final List<HttpRequest> sent = new CopyOnWriteArrayList<>();

    CountingHttpClient(HttpClient delegate) {
      this.delegate = delegate;
    }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
        throws IOException, InterruptedException {
      sent.add(request);
      return delegate.send(request, handler);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request, HttpResponse.BodyHandler<T> handler) {
      sent.add(request);
      return delegate.sendAsync(request, handler);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
        HttpRequest request,
        HttpResponse.BodyHandler<T> handler,
        HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
      sent.add(request);
      return delegate.sendAsync(request, handler, pushPromiseHandler);
    }

    @Override
    public Optional<CookieHandler> cookieHandler() {
      return delegate.cookieHandler();
    }

    @Override
    public Optional<Duration> connectTimeout() {
      return delegate.connectTimeout();
    }

    @Override
    public Redirect followRedirects() {
      return delegate.followRedirects();
    }

    @Override
    public Optional<ProxySelector> proxy() {
      return delegate.proxy();
    }

    @Override
    public SSLContext sslContext() {
      return delegate.sslContext();
    }

    @Override
    public SSLParameters sslParameters() {
      return delegate.sslParameters();
    }

    @Override
    public Optional<Authenticator> authenticator() {
      return delegate.authenticator();
    }

    @Override
    public Version version() {
      return delegate.version();
    }

    @Override
    public Optional<Executor> executor() {
      return delegate.executor();
    }
  }

  static Stream<Arguments> cases() {
    return Stream.of(
        Arguments.of("custom client without trace id", true, null),
        Arguments.of("custom client with trace id", true, "trace-abc12345"),
        Arguments.of("null client uses a default client", false, null));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("cases")
  void sendsThroughTheGivenClient(
      String name, boolean custom, String traceId, WireMockRuntimeInfo wm) {
    stubFor(
        get(PATH)
            .willReturn(
                aResponse()
                    .withStatus(404)
                    .withHeader("Content-Type", "application/json")
                    .withBody(NOT_FOUND_BODY)));
    CountingHttpClient counting = new CountingHttpClient(HttpClient.newHttpClient());
    SmriteaClient client =
        new SmriteaClient(
            "test-key", "app-test", wm.getHttpBaseUrl(), 0, traceId, custom ? counting : null);

    assertThrows(SmriteaNotFoundError.class, () -> client.get("mem_1"), name);

    assertEquals(custom ? 1 : 0, counting.sent.size(), name);
    verify(1, getRequestedFor(urlEqualTo(PATH)).withHeader("X-API-Key", equalTo("test-key")));
    if (traceId != null) {
      verify(1, getRequestedFor(urlEqualTo(PATH)).withHeader("X-Trace-ID", equalTo(traceId)));
    } else {
      verify(1, getRequestedFor(urlEqualTo(PATH)).withoutHeader("X-Trace-ID"));
    }
  }

  static Stream<Arguments> setters() {
    return Stream.of(
        Arguments.of("cookieHandler", (Consumer<HttpClient.Builder>) b -> b.cookieHandler(null)),
        Arguments.of(
            "connectTimeout",
            (Consumer<HttpClient.Builder>) b -> b.connectTimeout(Duration.ofSeconds(1))),
        Arguments.of("sslContext", (Consumer<HttpClient.Builder>) b -> b.sslContext(null)),
        Arguments.of("sslParameters", (Consumer<HttpClient.Builder>) b -> b.sslParameters(null)),
        Arguments.of("executor", (Consumer<HttpClient.Builder>) b -> b.executor(null)),
        Arguments.of(
            "followRedirects",
            (Consumer<HttpClient.Builder>) b -> b.followRedirects(HttpClient.Redirect.NEVER)),
        Arguments.of(
            "version", (Consumer<HttpClient.Builder>) b -> b.version(HttpClient.Version.HTTP_1_1)),
        Arguments.of("priority", (Consumer<HttpClient.Builder>) b -> b.priority(1)),
        Arguments.of("proxy", (Consumer<HttpClient.Builder>) b -> b.proxy(null)),
        Arguments.of("authenticator", (Consumer<HttpClient.Builder>) b -> b.authenticator(null)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("setters")
  void builderSetterFails(String name, Consumer<HttpClient.Builder> setter) {
    UnsupportedOperationException e =
        assertThrows(
            UnsupportedOperationException.class,
            () -> setter.accept(new ProvidedHttpClientBuilder(HttpClient.newHttpClient())),
            name);
    assertEquals(ProvidedHttpClientBuilder.CONFIGURED, e.getMessage(), name);
  }

  @Test
  void buildReturnsTheGivenClient() {
    HttpClient given = HttpClient.newHttpClient();
    assertSame(given, new ProvidedHttpClientBuilder(given).build());
  }
}
