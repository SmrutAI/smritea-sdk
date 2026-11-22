package ai.smritea.sdk;

import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * An {@link HttpClient} that limits the TOTAL time of one HTTP exchange: from the moment the
 * request is sent until the whole response body is read.
 *
 * <p>{@code HttpRequest.Builder.timeout} alone is not enough. It covers only the wait for the
 * response headers. A server that sends the headers and then stops sending the body would block a
 * reader of the body stream forever. This client applies one deadline to the three phases of an
 * exchange:
 *
 * <ol>
 *   <li>The wait for the response headers: the request is sent with the remaining time as its
 *       request timeout, and a timer cancels the exchange when the deadline passes.
 *   <li>The read of an {@link InputStream} body: the stream is returned inside a stream that a
 *       timer closes at the deadline, so a blocked read fails with an {@link HttpTimeoutException}.
 *   <li>The read of any other body type: the timer cancels the exchange, so the call fails with an
 *       {@link HttpTimeoutException}.
 * </ol>
 *
 * <p>All timers run on one shared daemon thread. Each exchange starts with a fresh budget. This
 * class is used only for the default client of {@link SmriteaClient}. A client that the caller
 * supplies is never wrapped.
 */
final class TimeoutHttpClient extends HttpClient {
  private static final String TIMER_THREAD_NAME = "smritea-http-timeout";
  private static final Duration MIN_REMAINING = Duration.ofMillis(1);
  private static final ScheduledThreadPoolExecutor TIMER = createTimer();

  private final HttpClient delegate;
  private final Duration timeout;

  /**
   * Wraps a client.
   *
   * @param delegate the client that sends the requests
   * @param timeout the total budget of one exchange, positive
   */
  TimeoutHttpClient(HttpClient delegate, Duration timeout) {
    this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
    this.timeout = Objects.requireNonNull(timeout, "timeout must not be null");
    if (timeout.isZero() || timeout.isNegative()) {
      throw new IllegalArgumentException("timeout must be positive");
    }
  }

  /**
   * Creates the default client of {@link SmriteaClient}: a new JDK client whose connect timeout is
   * {@code timeout}, wrapped so that {@code timeout} limits every exchange.
   *
   * @param timeout the total budget of one exchange, positive
   * @return the new client
   */
  static TimeoutHttpClient newDefault(Duration timeout) {
    return new TimeoutHttpClient(HttpClient.newBuilder().connectTimeout(timeout).build(), timeout);
  }

  /**
   * Returns the total budget of one exchange.
   *
   * @return the budget
   */
  Duration timeout() {
    return timeout;
  }

  private static ScheduledThreadPoolExecutor createTimer() {
    ScheduledThreadPoolExecutor timer =
        new ScheduledThreadPoolExecutor(
            1,
            task -> {
              Thread thread = new Thread(task, TIMER_THREAD_NAME);
              thread.setDaemon(true);
              return thread;
            });
    timer.setRemoveOnCancelPolicy(true);
    return timer;
  }

  @Override
  public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
      throws IOException, InterruptedException {
    CompletableFuture<HttpResponse<T>> future = sendAsync(request, handler);
    try {
      return future.get();
    } catch (InterruptedException e) {
      future.cancel(true);
      throw e;
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof IOException) {
        throw (IOException) cause;
      }
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new IOException(cause);
    }
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request, HttpResponse.BodyHandler<T> handler) {
    return exchange(request, handler, delegate::sendAsync);
  }

  @Override
  public <T> CompletableFuture<HttpResponse<T>> sendAsync(
      HttpRequest request,
      HttpResponse.BodyHandler<T> handler,
      HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
    return exchange(
        request,
        handler,
        (bounded, guarded) -> delegate.sendAsync(bounded, guarded, pushPromiseHandler));
  }

  /**
   * Runs one exchange under one deadline. The returned future completes with the response, or
   * exceptionally with an {@link HttpTimeoutException} when the deadline passes before the response
   * headers are complete.
   */
  private <T> CompletableFuture<HttpResponse<T>> exchange(
      HttpRequest request,
      HttpResponse.BodyHandler<T> handler,
      BiFunction<HttpRequest, HttpResponse.BodyHandler<T>, CompletableFuture<HttpResponse<T>>>
          sender) {
    Objects.requireNonNull(request, "request must not be null");
    Objects.requireNonNull(handler, "handler must not be null");
    long deadline = System.nanoTime() + timeout.toNanos();
    CompletableFuture<HttpResponse<T>> inner =
        sender.apply(withTimeout(request, remaining(deadline)), guardBody(handler, deadline));
    CompletableFuture<HttpResponse<T>> outer = new CompletableFuture<>();
    ScheduledFuture<?> timer =
        TIMER.schedule(
            () -> {
              if (outer.completeExceptionally(timeoutException(null))) {
                inner.cancel(true);
              }
            },
            remaining(deadline).toNanos(),
            TimeUnit.NANOSECONDS);
    inner.whenComplete(
        (response, error) -> {
          timer.cancel(false);
          if (error == null) {
            outer.complete(response);
          } else if (error instanceof CancellationException) {
            outer.cancel(true);
          } else {
            outer.completeExceptionally(error);
          }
        });
    outer.whenComplete(
        (response, error) -> {
          if (outer.isCancelled()) {
            inner.cancel(true);
          }
        });
    return outer;
  }

  /** Copies the request and sets its request timeout to the time left, or less if it is lower. */
  private static HttpRequest withTimeout(HttpRequest request, Duration remaining) {
    Duration limit = request.timeout().filter(t -> t.compareTo(remaining) < 0).orElse(remaining);
    HttpRequest.Builder builder =
        HttpRequest.newBuilder(request.uri())
            .method(
                request.method(),
                request.bodyPublisher().orElseGet(HttpRequest.BodyPublishers::noBody))
            .expectContinue(request.expectContinue())
            .timeout(limit);
    request.version().ifPresent(builder::version);
    request.headers().map().forEach((name, values) -> values.forEach(v -> builder.header(name, v)));
    return builder.build();
  }

  /** Returns the time left until the deadline, never less than {@link #MIN_REMAINING}. */
  private static Duration remaining(long deadline) {
    Duration left = Duration.ofNanos(deadline - System.nanoTime());
    return left.compareTo(MIN_REMAINING) < 0 ? MIN_REMAINING : left;
  }

  private <T> HttpResponse.BodyHandler<T> guardBody(
      HttpResponse.BodyHandler<T> handler, long deadline) {
    return info ->
        HttpResponse.BodySubscribers.mapping(handler.apply(info), body -> guard(body, deadline));
  }

  /**
   * Returns an {@link InputStream} body inside a stream that the deadline closes. Any other body is
   * returned as it is: its read ends when the exchange is cancelled. The cast is safe because the
   * guarded stream is only created for a body that is an {@link InputStream}, and the only handler
   * of that type is the one for {@code InputStream}.
   */
  @SuppressWarnings("unchecked")
  private <T> T guard(T body, long deadline) {
    if (body instanceof InputStream) {
      return (T)
          new DeadlineInputStream((InputStream) body, remaining(deadline), timeoutException(null));
    }
    return body;
  }

  private HttpTimeoutException timeoutException(Throwable cause) {
    HttpTimeoutException e =
        new HttpTimeoutException("request timed out after " + timeout.toMillis() + " ms");
    if (cause != null) {
      e.initCause(cause);
    }
    return e;
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

  @Override
  public WebSocket.Builder newWebSocketBuilder() {
    return delegate.newWebSocketBuilder();
  }

  /** One read of the wrapped stream. */
  @FunctionalInterface
  private interface Read {
    int read() throws IOException;
  }

  /**
   * An {@link InputStream} that a timer closes when the deadline passes. A read that is blocked at
   * that time, and every later read, fails with an {@link HttpTimeoutException}. The timer is
   * cancelled when the end of the stream is read or the stream is closed.
   */
  private static final class DeadlineInputStream extends InputStream {
    private static final int ACTIVE = 0;
    private static final int DONE = 1;
    private static final int TIMED_OUT = 2;

    private final InputStream delegate;
    private final HttpTimeoutException timeoutError;
    private final AtomicInteger state = new AtomicInteger(ACTIVE);
    private final ScheduledFuture<?> timer;

    DeadlineInputStream(
        InputStream delegate, Duration remaining, HttpTimeoutException timeoutError) {
      this.delegate = delegate;
      this.timeoutError = timeoutError;
      this.timer = TIMER.schedule(this::expire, remaining.toNanos(), TimeUnit.NANOSECONDS);
    }

    /** Runs on the timer thread at the deadline. */
    private void expire() {
      if (state.compareAndSet(ACTIVE, TIMED_OUT)) {
        try {
          delegate.close();
        } catch (IOException e) {
          timeoutError.addSuppressed(e);
        }
      }
    }

    private void failIfExpired(IOException cause) throws IOException {
      if (state.get() == TIMED_OUT) {
        if (cause != null) {
          timeoutError.addSuppressed(cause);
        }
        throw timeoutError;
      }
    }

    private int guardedRead(Read read) throws IOException {
      failIfExpired(null);
      int count;
      try {
        count = read.read();
      } catch (IOException e) {
        failIfExpired(e);
        throw e;
      }
      if (count < 0 && !state.compareAndSet(ACTIVE, DONE)) {
        // The end of the stream may be the effect of the timer closing it.
        failIfExpired(null);
      }
      return count;
    }

    @Override
    public int read() throws IOException {
      return guardedRead(delegate::read);
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
      return guardedRead(() -> delegate.read(buffer, offset, length));
    }

    @Override
    public int available() throws IOException {
      failIfExpired(null);
      return delegate.available();
    }

    @Override
    public void close() throws IOException {
      timer.cancel(false);
      state.compareAndSet(ACTIVE, DONE);
      delegate.close();
    }
  }
}
