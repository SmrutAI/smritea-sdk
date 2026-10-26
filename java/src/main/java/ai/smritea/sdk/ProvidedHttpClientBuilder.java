package ai.smritea.sdk;

import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/**
 * An {@link HttpClient.Builder} whose {@link #build()} returns the client that the caller passed to
 * {@link SmriteaClient}. The client is complete, so every setter fails.
 */
final class ProvidedHttpClientBuilder implements HttpClient.Builder {
  static final String CONFIGURED = "configure the HttpClient before you pass it to SmriteaClient";

  private final HttpClient client;

  ProvidedHttpClientBuilder(HttpClient client) {
    this.client = Objects.requireNonNull(client, "httpClient must not be null");
  }

  @Override
  public HttpClient.Builder cookieHandler(CookieHandler cookieHandler) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder connectTimeout(Duration duration) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder sslContext(SSLContext sslContext) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder sslParameters(SSLParameters sslParameters) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder executor(Executor executor) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder followRedirects(HttpClient.Redirect policy) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder version(HttpClient.Version version) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder priority(int priority) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder proxy(ProxySelector proxySelector) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient.Builder authenticator(Authenticator authenticator) {
    throw new UnsupportedOperationException(CONFIGURED);
  }

  @Override
  public HttpClient build() {
    return client;
  }
}
