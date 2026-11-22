package ai.smritea.sdk;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.smritea.sdk._internal.autogen.model.MemoryResponse;
import ai.smritea.sdk.errors.SmriteaError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.github.tomakehurst.wiremock.stubbing.Scenario;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests of the total timeout of one HTTP exchange. A real server on the loopback interface stalls
 * in two ways: it never answers, or it sends the headers and a part of the body and then stops.
 */
@WireMockTest
class SmriteaTimeoutTest {

  /** The budget of the wrapper in the tests: short, so that the suite stays fast. */
  private static final Duration SHORT = Duration.ofMillis(500);

  /** The time that a call may take over its budget. */
  private static final Duration TOLERANCE = Duration.ofMillis(1500);

  /** The time after which a test that hangs fails instead. */
  private static final Duration HANG_LIMIT = Duration.ofSeconds(15);

  private static final String MEMORY_PATH = "/api/v1/sdk/memories/mem_1";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  /** How the stall server misbehaves. */
  private enum Stall {
    /** It accepts the connection and never answers. */
    NEVER_ANSWER,
    /** It sends 200, a Content-Length of 100 and 10 body bytes, then stops. */
    STALL_BODY
  }

  /** A server on the loopback interface that stalls every connection. */
  private static final class StallServer implements AutoCloseable {
    private static final String RESPONSE_HEAD =
        "HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 100\r\n\r\n";
    private static final String PARTIAL_BODY = "0123456789";
    private static final String END_OF_HEAD = "\r\n\r\n";

    private final ServerSocket server;
    private final List<Socket> sockets = new CopyOnWriteArrayList<>();

    StallServer(Stall stall) throws IOException {
      server = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
      Thread acceptor = new Thread(() -> acceptAll(stall), "stall-server");
      acceptor.setDaemon(true);
      acceptor.start();
    }

    String baseUrl() {
      return "http://127.0.0.1:" + server.getLocalPort();
    }

    private void acceptAll(Stall stall) {
      while (!server.isClosed()) {
        try {
          Socket socket = server.accept();
          sockets.add(socket);
          if (stall == Stall.STALL_BODY) {
            respond(socket);
          }
        } catch (IOException e) {
          // The server is closed, or the client gave up before the response head: stop or go on.
          continue;
        }
      }
    }

    private void respond(Socket socket) throws IOException {
      InputStream in = socket.getInputStream();
      StringBuilder head = new StringBuilder();
      while (!head.toString().endsWith(END_OF_HEAD)) {
        int next = in.read();
        if (next < 0) {
          return;
        }
        head.append((char) next);
      }
      socket
          .getOutputStream()
          .write((RESPONSE_HEAD + PARTIAL_BODY).getBytes(StandardCharsets.UTF_8));
      socket.getOutputStream().flush();
    }

    @Override
    public void close() throws IOException {
      server.close();
      for (Socket socket : sockets) {
        socket.close();
      }
    }
  }

  private static HttpClient httpClientOf(SmriteaClient client) throws ReflectiveOperationException {
    Field apiField = SmriteaClient.class.getDeclaredField("api");
    apiField.setAccessible(true);
    Object api = apiField.get(client);
    Field httpField = api.getClass().getDeclaredField("memberVarHttpClient");
    httpField.setAccessible(true);
    return (HttpClient) httpField.get(api);
  }

  // ---------------------------------------------------------------------------
  // Wiring of the default and of a given client
  // ---------------------------------------------------------------------------

  @Test
  void defaultTimeoutIsTenSeconds() {
    assertEquals(Duration.ofSeconds(10), SmriteaClient.DEFAULT_TIMEOUT);
  }

  static Stream<Arguments> publicConstructors() {
    return Stream.of(
        Arguments.of("2 arguments", (Supplier<SmriteaClient>) () -> new SmriteaClient("k", "a")),
        Arguments.of(
            "4 arguments",
            (Supplier<SmriteaClient>) () -> new SmriteaClient("k", "a", "http://localhost:1", 0)),
        Arguments.of(
            "5 arguments",
            (Supplier<SmriteaClient>)
                () -> new SmriteaClient("k", "a", "http://localhost:1", 0, null)),
        Arguments.of(
            "6 arguments with a null client",
            (Supplier<SmriteaClient>)
                () -> new SmriteaClient("k", "a", "http://localhost:1", 0, null, null)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("publicConstructors")
  void publicConstructorsWireTheDefaultTimeout(String name, Supplier<SmriteaClient> constructor)
      throws Exception {
    HttpClient http = httpClientOf(constructor.get());

    TimeoutHttpClient wrapper = assertInstanceOf(TimeoutHttpClient.class, http, name);
    assertEquals(SmriteaClient.DEFAULT_TIMEOUT, wrapper.timeout(), name);
    assertEquals(Optional.of(SmriteaClient.DEFAULT_TIMEOUT), wrapper.connectTimeout(), name);
  }

  @Test
  void aGivenClientReachesTheApiUnchanged() throws Exception {
    HttpClient given = HttpClient.newHttpClient();

    SmriteaClient client = new SmriteaClient("k", "a", "http://localhost:1", 0, null, given);

    assertSame(given, httpClientOf(client));
    assertEquals(Optional.empty(), given.connectTimeout());
  }

  @Test
  void aGivenClientGetsNoTimeoutFromTheSdk() throws Exception {
    try (StallServer server = new StallServer(Stall.NEVER_ANSWER)) {
      // The wrapper budget is SHORT, but it must not apply to a client that the caller gives.
      SmriteaClient client =
          new SmriteaClient("k", "a", server.baseUrl(), 0, null, HttpClient.newHttpClient(), SHORT);
      AtomicReference<Throwable> outcome = new AtomicReference<>();
      Thread call =
          new Thread(
              () -> {
                try {
                  client.get("mem_1");
                } catch (RuntimeException e) {
                  outcome.set(e);
                }
              });
      call.setDaemon(true);
      call.start();

      call.join(SHORT.plus(TOLERANCE).toMillis());

      assertTrue(call.isAlive(), "the call must still wait after the budget; outcome: " + outcome);
      assertNull(outcome.get());
    }
  }

  // ---------------------------------------------------------------------------
  // Timeout through SmriteaClient: every operation, both stalls
  // ---------------------------------------------------------------------------

  static Stream<Arguments> stalledOperations() {
    return Stream.of(Stall.values())
        .flatMap(
            stall ->
                Stream.of(
                    Arguments.of("add", stall, (Consumer<SmriteaClient>) c -> c.add("hello", null)),
                    Arguments.of(
                        "search", stall, (Consumer<SmriteaClient>) c -> c.search("hello", null)),
                    Arguments.of("get", stall, (Consumer<SmriteaClient>) c -> c.get("mem_1")),
                    Arguments.of(
                        "delete", stall, (Consumer<SmriteaClient>) c -> c.delete("mem_1"))));
  }

  @ParameterizedTest(name = "{0} / {1}")
  @MethodSource("stalledOperations")
  void defaultClientTimesOut(String operation, Stall stall, Consumer<SmriteaClient> call)
      throws Exception {
    try (StallServer server = new StallServer(stall)) {
      SmriteaClient client = new SmriteaClient("k", "a", server.baseUrl(), 0, null, null, SHORT);
      long started = System.nanoTime();

      SmriteaError error =
          assertTimeoutPreemptively(
              HANG_LIMIT, () -> assertThrows(SmriteaError.class, () -> call.accept(client)));

      Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
      String name = operation + " / " + stall;
      assertEquals(SmriteaError.class, error.getClass(), name);
      assertTrue(error.getMessage().startsWith("request failed: "), name + ": " + error);
      assertTrue(error.getMessage().contains("timed out"), name + ": " + error);
      assertInstanceOf(HttpTimeoutException.class, error.getCause(), name);
      assertTrue(elapsed.compareTo(SHORT.minusMillis(150)) >= 0, name + " too early: " + elapsed);
      assertTrue(elapsed.compareTo(SHORT.plus(TOLERANCE)) <= 0, name + " too late: " + elapsed);
    }
  }

  // ---------------------------------------------------------------------------
  // The wrapper itself: send and sendAsync, stream and non-stream bodies
  // ---------------------------------------------------------------------------

  private enum Api {
    SEND,
    SEND_ASYNC,
    SEND_ASYNC_WITH_PUSH_HANDLER
  }

  private enum Body {
    STREAM,
    STRING
  }

  private static <T> HttpResponse<T> exchange(
      TimeoutHttpClient client, HttpRequest request, HttpResponse.BodyHandler<T> handler, Api api)
      throws Exception {
    if (api == Api.SEND) {
      return client.send(request, handler);
    }
    if (api == Api.SEND_ASYNC) {
      return client.sendAsync(request, handler).get();
    }
    return client.sendAsync(request, handler, null).get();
  }

  /** Sends a GET to the URL and reads the whole body. Unwraps the failure of an async call. */
  private static byte[] fetch(TimeoutHttpClient client, String url, Api api, Body body)
      throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
    try {
      if (body == Body.STREAM) {
        HttpResponse<InputStream> response =
            exchange(client, request, HttpResponse.BodyHandlers.ofInputStream(), api);
        try (InputStream in = response.body()) {
          return in.readAllBytes();
        }
      }
      return exchange(client, request, HttpResponse.BodyHandlers.ofString(), api)
          .body()
          .getBytes(StandardCharsets.UTF_8);
    } catch (ExecutionException e) {
      if (e.getCause() instanceof Exception) {
        throw (Exception) e.getCause();
      }
      throw e;
    }
  }

  static Stream<Arguments> wrapperCases() {
    return Stream.of(Stall.values())
        .flatMap(
            stall ->
                Stream.of(Api.values())
                    .flatMap(
                        api ->
                            Stream.of(Body.values()).map(body -> Arguments.of(stall, api, body))));
  }

  @ParameterizedTest(name = "{0} / {1} / {2}")
  @MethodSource("wrapperCases")
  void wrapperBoundsTheWholeExchange(Stall stall, Api api, Body body) throws Exception {
    try (StallServer server = new StallServer(stall)) {
      TimeoutHttpClient client = new TimeoutHttpClient(HttpClient.newHttpClient(), SHORT);
      long started = System.nanoTime();

      Exception error =
          assertTimeoutPreemptively(
              HANG_LIMIT,
              () ->
                  assertThrows(
                      HttpTimeoutException.class,
                      () -> fetch(client, server.baseUrl() + "/x", api, body)));

      Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
      String name = stall + " / " + api + " / " + body;
      assertTrue(error.getMessage().contains("timed out"), name + ": " + error);
      assertTrue(elapsed.compareTo(SHORT.minusMillis(150)) >= 0, name + " too early: " + elapsed);
      assertTrue(elapsed.compareTo(SHORT.plus(TOLERANCE)) <= 0, name + " too late: " + elapsed);
    }
  }

  @Test
  void plainRequestTimeoutDoesNotBoundTheBody() throws Exception {
    // Control: the JDK request timeout alone leaves a stalled body read blocked.
    try (StallServer server = new StallServer(Stall.STALL_BODY)) {
      HttpClient plain = HttpClient.newHttpClient();
      HttpRequest request =
          HttpRequest.newBuilder(URI.create(server.baseUrl() + "/x")).GET().timeout(SHORT).build();
      AtomicReference<Throwable> outcome = new AtomicReference<>();
      Thread read =
          new Thread(
              () -> {
                try {
                  plain
                      .send(request, HttpResponse.BodyHandlers.ofInputStream())
                      .body()
                      .readAllBytes();
                } catch (IOException | InterruptedException e) {
                  outcome.set(e);
                }
              });
      read.setDaemon(true);
      read.start();

      read.join(SHORT.plus(TOLERANCE).toMillis());

      assertTrue(read.isAlive(), "a plain request timeout must not end the body read: " + outcome);
    }
  }

  @ParameterizedTest(name = "{0} / {1}")
  @MethodSource("successCases")
  void wrapperPassesASuccessfulResponse(Api api, Body body, WireMockRuntimeInfo wm)
      throws Exception {
    stubFor(get("/ok").willReturn(okJson("{\"ok\":true}")));
    TimeoutHttpClient client = new TimeoutHttpClient(HttpClient.newHttpClient(), SHORT);

    byte[] bytes = fetch(client, wm.getHttpBaseUrl() + "/ok", api, body);

    assertArrayEquals("{\"ok\":true}".getBytes(StandardCharsets.UTF_8), bytes);
  }

  static Stream<Arguments> successCases() {
    return Stream.of(Api.values())
        .flatMap(api -> Stream.of(Body.values()).map(body -> Arguments.of(api, body)));
  }

  @Test
  void readingTheEndAfterTheDeadlineIsNotAnError(WireMockRuntimeInfo wm) throws Exception {
    stubFor(get("/ok").willReturn(okJson("{\"ok\":true}")));
    TimeoutHttpClient client = new TimeoutHttpClient(HttpClient.newHttpClient(), SHORT);
    HttpRequest request = HttpRequest.newBuilder(URI.create(wm.getHttpBaseUrl() + "/ok")).build();

    HttpResponse<InputStream> response =
        client.send(request, HttpResponse.BodyHandlers.ofInputStream());
    try (InputStream in = response.body()) {
      assertArrayEquals("{\"ok\":true}".getBytes(StandardCharsets.UTF_8), in.readAllBytes());
      Thread.sleep(SHORT.plusMillis(300).toMillis());

      assertEquals(-1, in.read());
    }
  }

  @ParameterizedTest(name = "{0}")
  @EnumSource(Api.class)
  void wrapperMirrorsTheDelegateSettings(Api api) {
    HttpClient delegate =
        HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    TimeoutHttpClient client = new TimeoutHttpClient(delegate, SHORT);

    assertEquals(delegate.connectTimeout(), client.connectTimeout(), api.name());
    assertEquals(delegate.followRedirects(), client.followRedirects(), api.name());
    assertEquals(delegate.version(), client.version(), api.name());
    assertEquals(delegate.proxy(), client.proxy(), api.name());
    assertEquals(delegate.cookieHandler(), client.cookieHandler(), api.name());
    assertEquals(delegate.authenticator(), client.authenticator(), api.name());
    assertEquals(delegate.executor(), client.executor(), api.name());
    assertEquals(delegate.sslContext(), client.sslContext(), api.name());
    assertEquals(SHORT, client.timeout(), api.name());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("invalidTimeouts")
  void wrapperRejectsANonPositiveTimeout(String name, Duration timeout) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new TimeoutHttpClient(HttpClient.newHttpClient(), timeout),
        name);
  }

  static Stream<Arguments> invalidTimeouts() {
    return Stream.of(
        Arguments.of("zero", Duration.ZERO), Arguments.of("negative", Duration.ofSeconds(-1)));
  }

  @Test
  void timersShareOneDaemonThread() throws Exception {
    try (StallServer server = new StallServer(Stall.NEVER_ANSWER)) {
      TimeoutHttpClient client = new TimeoutHttpClient(HttpClient.newHttpClient(), SHORT);
      for (int i = 0; i < 3; i++) {
        assertThrows(
            HttpTimeoutException.class,
            () -> fetch(client, server.baseUrl() + "/x", Api.SEND, Body.STREAM));
      }
    }

    long timerThreads =
        Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.getName().equals("smritea-http-timeout"))
            .count();
    assertEquals(1, timerThreads);
    assertTrue(
        Thread.getAllStackTraces().keySet().stream()
            .filter(t -> t.getName().equals("smritea-http-timeout"))
            .allMatch(Thread::isDaemon));
  }

  // ---------------------------------------------------------------------------
  // A 429 retry starts a new exchange with a new budget
  // ---------------------------------------------------------------------------

  @Test
  void retryAfter429StartsANewBudget(WireMockRuntimeInfo wm) throws Exception {
    String memory =
        MAPPER.writeValueAsString(new MemoryResponse().id("mem_1").content("c").appId("a"));
    Duration budget = Duration.ofMillis(1500);
    Duration exchange = Duration.ofMillis(900);
    stubFor(
        get(MEMORY_PATH)
            .inScenario("retry")
            .whenScenarioStateIs(Scenario.STARTED)
            .willReturn(
                aResponse()
                    .withStatus(429)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"code\":\"RATE_LIMITED\",\"message\":\"slow down\"}")
                    .withFixedDelay((int) exchange.toMillis()))
            .willSetStateTo("second"));
    stubFor(
        get(MEMORY_PATH)
            .inScenario("retry")
            .whenScenarioStateIs("second")
            .willReturn(okJson(memory).withFixedDelay((int) exchange.toMillis())));
    SmriteaClient client = new SmriteaClient("k", "a", wm.getHttpBaseUrl(), 1, null, null, budget);
    long started = System.nanoTime();

    assertNotNull(client.get("mem_1"));

    Duration elapsed = Duration.ofNanos(System.nanoTime() - started);
    verify(2, getRequestedFor(urlEqualTo(MEMORY_PATH)));
    // Two exchanges and a retry sleep take longer than one budget, yet no exchange exceeded it.
    assertTrue(elapsed.compareTo(budget) > 0, "elapsed " + elapsed);
  }
}
