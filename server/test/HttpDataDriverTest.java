import com.sun.net.httpserver.HttpServer;
import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Tests du driver HTTP contre une fausse API Node embarquée. */
class HttpDataDriverTest {

    private HttpServer api;
    private HttpDataDriver driver;
    private final AtomicReference<String> lastBody = new AtomicReference<>();
    private final AtomicReference<String> lastUri = new AtomicReference<>();
    private volatile String nextResponse = "{\"error\":0,\"status\":200,\"data\":null}";

    @BeforeEach
    void setUp() throws Exception {
        api = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        api.createContext("/", exchange -> {
            lastUri.set(exchange.getRequestMethod() + " " + exchange.getRequestURI());
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = nextResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        api.start();
        driver = new HttpDataDriver("http://127.0.0.1:" + api.getAddress().getPort() + "/api");
    }

    @AfterEach
    void tearDown() {
        api.stop(0);
    }

    @Test
    void autoRegisterPostsGeoJsonAndReturnsModuleIdentity() {
        nextResponse = "{\"error\":0,\"status\":201,\"data\":{\"name\":\"module 4\",\"shortName\":\"mod4\",\"key\":\"k-4\"}}";

        String answer = driver.autoRegisterModule("esp32", List.of("lm35", "bme280"), 2.35, 48.85);

        assertEquals("OK module 4,mod4,k-4", answer);
        assertEquals("POST /api/modules/register", lastUri.get());
        Document sent = Document.parse(lastBody.get());
        assertEquals("esp32", sent.getString("uc"));
        assertEquals(List.of("lm35", "bme280"), sent.getList("chipsets", String.class));
        Document position = sent.get("position", Document.class);
        assertEquals("Point", position.getString("type"));
        assertEquals(List.of(2.35, 48.85), position.getList("coordinates", Double.class));
    }

    @Test
    void apiErrorIsTurnedIntoErrLine() {
        nextResponse = "{\"error\":1,\"status\":400,\"data\":\"invalid module key\"}";
        assertEquals("ERR invalid module key", driver.saveMeasure("temperature", "2026-01-01T00:00:00", "21", "bad"));
    }

    @Test
    void saveMeasurePostsAllFields() {
        assertEquals("OK", driver.saveMeasure("temperature", "2026-01-01T00:00:00", "21.5", "k-1"));
        assertEquals("POST /api/measures", lastUri.get());
        Document sent = Document.parse(lastBody.get());
        assertEquals("temperature", sent.getString("type"));
        assertEquals("2026-01-01T00:00:00", sent.getString("date"));
        assertEquals("21.5", sent.getString("value"));
        assertEquals("k-1", sent.getString("moduleKey"));
    }

    @Test
    void saveAnalysisHasNoModuleKey() {
        assertEquals("OK", driver.saveAnalysis("smoke", "2026-01-01T00:00:00", "1"));
        assertFalse(Document.parse(lastBody.get()).containsKey("moduleKey"));
    }

    @Test
    void getConfigEncodesKeyAndReturnsInterval() {
        nextResponse = "{\"error\":0,\"status\":200,\"data\":{\"sendInterval\":30}}";
        assertEquals("OK 30", driver.getConfig("a b&c"));
        assertEquals("GET /api/modules/config?key=a+b%26c", lastUri.get());
    }

    @Test
    void unreachableApiGivesExplicitError() {
        api.stop(0);
        assertEquals("ERR cannot join the API", driver.saveAnalysis("smoke", "2026-01-01T00:00:00", "1"));
    }
}
