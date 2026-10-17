package ai.smritea.sdk;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

import ai.smritea.sdk._internal.autogen.model.CreateMemoryResponse;
import ai.smritea.sdk._internal.autogen.model.MemoryResponse;
import ai.smritea.sdk.errors.SmriteaError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@WireMockTest
class SmriteaTraceRequestIdTest {

  private static final String ADD_PATH = "/api/v1/sdk/memories";
  private static final String INVALID_TRACE_ID_MESSAGE =
      "trace_id must be 8-100 characters of A-Z, a-z, 0-9, dot, underscore, colon or hyphen";
  private static final ObjectMapper MAPPER = new ObjectMapper();

  // ---------------------------------------------------------------------------
  // Test helpers
  // ---------------------------------------------------------------------------

  static Stream<String> validTraceIds() {
    return Stream.of("abcdefgh", "a.b_c:d-e", "a".repeat(100));
  }

  static Stream<String> invalidTraceIds() {
    return Stream.of("abcdefg", "a".repeat(101), "abc defgh", "");
  }

  private static String memCreateJson(String id, String content) {
    CreateMemoryResponse resp =
        new CreateMemoryResponse()
            .addMemoriesItem(new MemoryResponse().id(id).content(content).appId("app-test"));
    try {
      return MAPPER.writeValueAsString(resp);
    } catch (Exception e) {
      throw new RuntimeException("memCreateJson failed", e);
    }
  }

  private SmriteaClient clientFor(WireMockRuntimeInfo wm, String traceId) {
    return new SmriteaClient("test-key", "app-test", wm.getHttpBaseUrl(), 2, traceId);
  }

  // Zero retries so a 429 stub surfaces immediately instead of sleeping through backoff.
  private SmriteaClient noRetryClientFor(WireMockRuntimeInfo wm) {
    return new SmriteaClient("test-key", "app-test", wm.getHttpBaseUrl(), 0);
  }

  // -----------------------------------------------------------------------
  // X-Trace-ID header
  // -----------------------------------------------------------------------

  @ParameterizedTest(name = "valid trace id #{index}")
  @MethodSource("validTraceIds")
  void testTraceId_ValidValueSentAsHeader(String traceId, WireMockRuntimeInfo wm) {
    stubFor(post(ADD_PATH).willReturn(okJson(memCreateJson("mem-trace", "content"))));

    clientFor(wm, traceId).add("x", null);

    verify(1, postRequestedFor(urlEqualTo(ADD_PATH)).withHeader("X-Trace-ID", equalTo(traceId)));
  }

  @ParameterizedTest(name = "null trace id sends no header")
  @NullSource
  void testTraceId_NullSendsNoHeader(String traceId, WireMockRuntimeInfo wm) {
    stubFor(post(ADD_PATH).willReturn(okJson(memCreateJson("mem-null", "content"))));

    clientFor(wm, traceId).add("x", null);

    verify(1, postRequestedFor(urlEqualTo(ADD_PATH)).withHeader("X-Trace-ID", absent()));
  }

  @ParameterizedTest(name = "invalid trace id #{index}")
  @MethodSource("invalidTraceIds")
  void testTraceId_InvalidRejectedAtConstructionWithoutRequest(
      String traceId, WireMockRuntimeInfo wm) {
    IllegalArgumentException err =
        assertThrows(IllegalArgumentException.class, () -> clientFor(wm, traceId));

    assertEquals(INVALID_TRACE_ID_MESSAGE, err.getMessage());
    verify(0, anyRequestedFor(anyUrl()));
  }

  // -----------------------------------------------------------------------
  // getRequestId() on mapped errors
  // -----------------------------------------------------------------------

  @ParameterizedTest(name = "status {0}: body request_id is returned")
  @ValueSource(ints = {400, 401, 402, 403, 404, 409, 422, 429, 500})
  void testGetRequestId_BodyRequestIdPreferredOverHeader(int status, WireMockRuntimeInfo wm) {
    stubFor(
        post(ADD_PATH)
            .willReturn(
                aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("X-Request-ID", "req-header-" + status)
                    .withBody(
                        "{\"message\":\"boom\",\"code\":\"TEST_CODE\",\"request_id\":\"req-body-"
                            + status
                            + "\"}")));

    SmriteaError err = assertThrows(SmriteaError.class, () -> noRetryClientFor(wm).add("x", null));

    assertEquals(status, err.getHttpStatus());
    assertEquals("req-body-" + status, err.getRequestId());
  }

  @ParameterizedTest(name = "status {0}: X-Request-ID header is used when body has none")
  @ValueSource(ints = {400, 401, 402, 403, 404, 409, 422, 429, 500})
  void testGetRequestId_HeaderUsedWhenBodyHasNoRequestId(int status, WireMockRuntimeInfo wm) {
    stubFor(
        post(ADD_PATH)
            .willReturn(
                aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withHeader("X-Request-ID", "req-header-" + status)
                    .withBody("{\"message\":\"boom\",\"code\":\"TEST_CODE\"}")));

    SmriteaError err = assertThrows(SmriteaError.class, () -> noRetryClientFor(wm).add("x", null));

    assertEquals(status, err.getHttpStatus());
    assertEquals("req-header-" + status, err.getRequestId());
  }

  @ParameterizedTest(name = "status {0}: no request id when neither body nor header has one")
  @ValueSource(ints = {400, 401, 402, 403, 404, 409, 422, 429, 500})
  void testGetRequestId_NullWhenNeitherBodyNorHeaderHasOne(int status, WireMockRuntimeInfo wm) {
    stubFor(
        post(ADD_PATH)
            .willReturn(
                aResponse()
                    .withStatus(status)
                    .withHeader("Content-Type", "application/json")
                    .withBody("{\"message\":\"boom\",\"code\":\"TEST_CODE\"}")));

    SmriteaError err = assertThrows(SmriteaError.class, () -> noRetryClientFor(wm).add("x", null));

    assertEquals(status, err.getHttpStatus());
    assertNull(err.getRequestId());
  }
}
