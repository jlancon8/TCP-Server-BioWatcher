import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires du protocole TCP : on branche un vrai socket local sur un
 * {@link ThreadServer} dont les drivers sont remplacés par des faux.
 */
class ThreadServerTest {

    private FakeDataDriver http;
    private FakeDataDriver mongo;
    private ServerSocket listener;
    private Socket client;
    private ThreadServer thread;
    private BufferedReader in;
    private PrintStream out;

    @BeforeEach
    void setUp() throws Exception {
        http = new FakeDataDriver();
        mongo = new FakeDataDriver();
        listener = new ServerSocket(0);
        client = new Socket("127.0.0.1", listener.getLocalPort());
        client.setSoTimeout(5000);
        Socket accepted = listener.accept();
        thread = new ThreadServer(1, accepted, new TestExchanger(http, mongo));
        thread.start();
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintStream(client.getOutputStream(), true);
    }

    @AfterEach
    void tearDown() throws Exception {
        client.close();
        listener.close();
        thread.join(2000);
    }

    private String send(String line) throws Exception {
        out.println(line);
        return in.readLine();
    }

    @Test
    void unknownCommandIsRejected() throws Exception {
        assertEquals("ERR unknown command", send("HELLO"));
    }

    @Test
    void autoRegisterForwardsUcChipsetsAndCoordinatesToApi() throws Exception {
        http.answer = "OK module 3,mod3,abc-123";
        assertEquals("OK module 3,mod3,abc-123", send("AUTOREGISTER esp32 lm35 bme280 2.3522 48.8566"));
        assertEquals(List.of("autoRegister esp32 [lm35, bme280] 2.3522 48.8566"), http.calls);
        assertTrue(mongo.calls.isEmpty());
    }

    @Test
    void autoRegisterRequiresAtLeastOneChipset() throws Exception {
        assertEquals("ERR invalid number of parameters", send("AUTOREGISTER esp32 2.35 48.85"));
        assertTrue(http.calls.isEmpty());
    }

    @Test
    void autoRegisterRejectsNonNumericCoordinates() throws Exception {
        assertEquals("ERR invalid coordinates", send("AUTOREGISTER esp32 lm35 abc 48.85"));
        assertTrue(http.calls.isEmpty());
    }

    @Test
    void autoRegisterRejectsOutOfRangeCoordinates() throws Exception {
        assertEquals("ERR coordinates out of range", send("AUTOREGISTER esp32 lm35 200 48.85"));
        assertEquals("ERR coordinates out of range", send("AUTOREGISTER esp32 lm35 2.35 -91"));
        assertTrue(http.calls.isEmpty());
    }

    @Test
    void storeMeasureGoesThroughApi() throws Exception {
        assertEquals("OK", send("STOREMEASURE temperature 2026-11-04T19:51:27 23.1 key-1"));
        assertEquals(List.of("saveMeasure temperature 2026-11-04T19:51:27 23.1 key-1"), http.calls);
    }

    @Test
    void storeMeasureForwardsApiError() throws Exception {
        http.answer = "ERR invalid module key";
        assertEquals("ERR invalid module key", send("STOREMEASURE temperature 2026-11-04T19:51:27 23.1 bad"));
    }

    @Test
    void storeMeasureChecksParameterCount() throws Exception {
        assertEquals("ERR invalid number of parameters", send("STOREMEASURE temperature 23.1 key-1"));
        assertTrue(http.calls.isEmpty());
    }

    @Test
    void storeAnalysisGoesDirectlyToMongo() throws Exception {
        assertEquals("OK", send("STOREANALYSIS smoke 2026-11-04T19:51:27 1"));
        assertEquals(List.of("saveAnalysis smoke 2026-11-04T19:51:27 1"), mongo.calls);
        assertTrue(http.calls.isEmpty());
    }

    @Test
    void storeAnalysisChecksParameterCount() throws Exception {
        assertEquals("ERR invalid number of parameters", send("STOREANALYSIS smoke 1"));
    }

    @Test
    void getConfigGoesDirectlyToMongo() throws Exception {
        mongo.answer = "OK 60";
        assertEquals("OK 60", send("GETCONFIG key-1"));
        assertEquals(List.of("getConfig key-1"), mongo.calls);
    }

    @Test
    void getConfigChecksParameterCount() throws Exception {
        assertEquals("ERR invalid number of parameters", send("GETCONFIG"));
        assertEquals("ERR invalid number of parameters", send("GETCONFIG a b"));
    }

    @Test
    void severalRequestsOnTheSameConnection() throws Exception {
        assertEquals("OK", send("STOREANALYSIS a 2026-01-01T00:00:00 1"));
        assertEquals("OK", send("STOREANALYSIS b 2026-01-01T00:00:00 2"));
        assertEquals(2, mongo.calls.size());
    }

    @Test
    void emptyLineEndsTheThread() throws Exception {
        out.println("");
        thread.join(2000);
        assertFalse(thread.isAlive());
    }
}
