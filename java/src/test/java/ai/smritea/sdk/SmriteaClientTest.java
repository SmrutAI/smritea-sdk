package ai.smritea.sdk;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

import static org.junit.jupiter.api.Assertions.*;

import ai.smritea.sdk.errors.SmriteaAuthError;
import ai.smritea.sdk.errors.SmriteaDeserializationError;
import ai.smritea.sdk.errors.SmriteaNotFoundError;
import ai.smritea.sdk.errors.SmriteaQuotaError;
import ai.smritea.sdk.errors.SmriteaRateLimitError;
import ai.smritea.sdk.errors.SmriteaValidationError;
import ai.smritea.sdk.model.AddOptions;
import ai.smritea.sdk.model.Memory;
import ai.smritea.sdk.model.SearchResult;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;

import org.junit.jupiter.api.Test;

import java.util.List;

@WireMockTest
class SmriteaClientTest {

    private SmriteaClient clientFor(WireMockRuntimeInfo wm) {
        return new SmriteaClient("test-key", "app-test", wm.getHttpBaseUrl(), 2);
    }

    // -----------------------------------------------------------------------
    // Add
    // -----------------------------------------------------------------------

    @Test
    void testAdd_Success(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories")
                        .willReturn(
                                okJson(
                                        "{\"id\":\"mem-1\",\"content\":\"hello world\",\"app_id\":\"app-test\"}")));

        SmriteaClient client = clientFor(wm);
        Memory mem = client.add("hello world", null);

        assertNotNull(mem);
        assertEquals("mem-1", mem.getId());
        assertEquals("hello world", mem.getContent());
        assertEquals("app-test", mem.getAppId());
    }

    @Test
    void testAdd_UserIdShorthand(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories")
                        .withRequestBody(matchingJsonPath("$.actor_id", equalTo("user-42")))
                        .withRequestBody(matchingJsonPath("$.actor_type", equalTo("user")))
                        .willReturn(
                                okJson(
                                        "{\"id\":\"mem-2\",\"content\":\"content\",\"app_id\":\"app-test\"}")));

        SmriteaClient client = clientFor(wm);
        Memory mem = client.add("content", new AddOptions().withUserId("user-42"));

        assertNotNull(mem);
        assertEquals("mem-2", mem.getId());
    }

    @Test
    void testAdd_ExplicitActorIdAndType(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories")
                        .withRequestBody(matchingJsonPath("$.actor_id", equalTo("agent-7")))
                        .withRequestBody(matchingJsonPath("$.actor_type", equalTo("agent")))
                        .willReturn(
                                okJson(
                                        "{\"id\":\"mem-3\",\"content\":\"content\",\"app_id\":\"app-test\"}")));

        SmriteaClient client = clientFor(wm);
        Memory mem =
                client.add(
                        "content", new AddOptions().withActorId("agent-7").withActorType("agent"));

        assertNotNull(mem);
        assertEquals("mem-3", mem.getId());
    }

    // -----------------------------------------------------------------------
    // Search
    // -----------------------------------------------------------------------

    @Test
    void testSearch_Success(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories/search")
                        .willReturn(
                                okJson(
                                        "{\"memories\":[{\"memory\":{\"id\":\"mem-1\",\"content\":\"found\"},\"score\":0.9}]}")));

        SmriteaClient client = clientFor(wm);
        List<SearchResult> results = client.search("found", null);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("mem-1", results.get(0).getMemory().getId());
        assertEquals("found", results.get(0).getContent());
        assertEquals(0.9, results.get(0).getScore(), 0.001);
    }

    @Test
    void testSearch_EmptyResult(WireMockRuntimeInfo wm) {
        stubFor(post("/api/v1/sdk/memories/search").willReturn(okJson("{\"memories\":[]}")));

        SmriteaClient client = clientFor(wm);
        List<SearchResult> results = client.search("nothing", null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // -----------------------------------------------------------------------
    // Get
    // -----------------------------------------------------------------------

    @Test
    void testGet_Success(WireMockRuntimeInfo wm) {
        stubFor(
                get("/api/v1/sdk/memories/mem-99")
                        .willReturn(
                                okJson(
                                        "{\"id\":\"mem-99\",\"content\":\"stored content\",\"app_id\":\"app-test\"}")));

        SmriteaClient client = clientFor(wm);
        Memory mem = client.get("mem-99");

        assertNotNull(mem);
        assertEquals("mem-99", mem.getId());
        assertEquals("stored content", mem.getContent());
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------

    @Test
    void testDelete_Success(WireMockRuntimeInfo wm) {
        stubFor(delete("/api/v1/sdk/memories/mem-99").willReturn(aResponse().withStatus(204)));

        SmriteaClient client = clientFor(wm);
        assertDoesNotThrow(() -> client.delete("mem-99"));
    }

    // -----------------------------------------------------------------------
    // Error mapping
    // -----------------------------------------------------------------------

    @Test
    void testAdd_401_MapsToAuthError(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories")
                        .willReturn(
                                aResponse()
                                        .withStatus(401)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"detail\":\"invalid api key\"}")));

        SmriteaClient client = clientFor(wm);
        SmriteaAuthError err = assertThrows(SmriteaAuthError.class, () -> client.add("x", null));

        assertEquals(401, err.getStatusCode());
    }

    @Test
    void testSearch_404_MapsToNotFoundError(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories/search")
                        .willReturn(
                                aResponse()
                                        .withStatus(404)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"detail\":\"not found\"}")));

        SmriteaClient client = clientFor(wm);
        assertThrows(SmriteaNotFoundError.class, () -> client.search("x", null));
    }

    @Test
    void testAdd_400_MapsToValidationError(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories")
                        .willReturn(
                                aResponse()
                                        .withStatus(400)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"detail\":\"content required\"}")));

        SmriteaClient client = clientFor(wm);
        assertThrows(SmriteaValidationError.class, () -> client.add("", null));
    }

    @Test
    void testAdd_402_MapsToQuotaError(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories")
                        .willReturn(
                                aResponse()
                                        .withStatus(402)
                                        .withHeader("Content-Type", "application/json")
                                        .withBody("{\"detail\":\"quota exceeded\"}")));

        SmriteaClient client = clientFor(wm);
        assertThrows(SmriteaQuotaError.class, () -> client.add("x", null));
    }

    // -----------------------------------------------------------------------
    // Retry behaviour
    // -----------------------------------------------------------------------

    @Test
    void testAdd_429_RetryThenSuccess(WireMockRuntimeInfo wm) {
        stubFor(
                post("/api/v1/sdk/memories")
                        .inScenario("retry")
                        .whenScenarioStateIs("Started")
                        .willReturn(
                                aResponse()
                                        .withStatus(429)
                                        .withHeader("Retry-After", "1")
                                        .withBody("{\"detail\":\"rate limited\"}"))
                        .willSetStateTo("retried"));
        stubFor(
                post("/api/v1/sdk/memories")
                        .inScenario("retry")
                        .whenScenarioStateIs("retried")
                        .willReturn(
                                okJson(
                                        "{\"id\":\"mem-ok\",\"content\":\"ok\",\"app_id\":\"app-test\"}")));

        SmriteaClient client = clientFor(wm);
        Memory mem = client.add("x", null);

        assertNotNull(mem);
        assertEquals("mem-ok", mem.getId());
    }

    @Test
    void testAdd_429_Exhausted_MapsToRateLimitError(WireMockRuntimeInfo wm) {
        // maxRetries=2 means 3 total attempts; all return 429 -> SmriteaRateLimitError
        stubFor(
                post("/api/v1/sdk/memories")
                        .willReturn(
                                aResponse()
                                        .withStatus(429)
                                        .withHeader("Retry-After", "1")
                                        .withBody("{\"detail\":\"rate limited\"}")));

        SmriteaClient client = clientFor(wm);
        SmriteaRateLimitError err =
                assertThrows(SmriteaRateLimitError.class, () -> client.add("x", null));

        assertNotNull(err.getRetryAfter());
        assertEquals(1, err.getRetryAfter());
    }

    // -----------------------------------------------------------------------
    // Deserialization error
    // -----------------------------------------------------------------------

    @Test
    void testAdd_NullBody_ThrowsDeserializationError(WireMockRuntimeInfo wm) {
        stubFor(post("/api/v1/sdk/memories").willReturn(okJson("null")));

        SmriteaClient client = clientFor(wm);
        assertThrows(SmriteaDeserializationError.class, () -> client.add("x", null));
    }

    @Test
    void testGet_NullBody_ThrowsDeserializationError(WireMockRuntimeInfo wm) {
        stubFor(get("/api/v1/sdk/memories/mem-null").willReturn(okJson("null")));

        SmriteaClient client = clientFor(wm);
        assertThrows(SmriteaDeserializationError.class, () -> client.get("mem-null"));
    }

    // -----------------------------------------------------------------------
    // GetAll -- not yet implemented
    // -----------------------------------------------------------------------

    @Test
    void testGetAll_NotImplemented() {
        SmriteaClient client = new SmriteaClient("test-key", "app-test");
        assertThrows(UnsupportedOperationException.class, client::getAll);
    }
}
