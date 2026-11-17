import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test d'intégration : protocole TCP ({@link ThreadServer}) + vrai {@link MongoDataDriver}
 * branché sur un MongoDB réel (variable MONGO_URL). Ignoré si MONGO_URL n'est pas défini.
 *
 * <p>En local : {@code docker run -d -p 27017:27017 mongo:7} puis
 * {@code MONGO_URL=mongodb://localhost:27017 mvn verify}.</p>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MongoIT {

    private MongoClient mongo;
    private MongoDatabase db;
    private ServerSocket listener;
    private Socket client;
    private BufferedReader in;
    private PrintStream out;

    @BeforeAll
    void startStack() throws Exception {
        String mongoUrl = System.getenv("MONGO_URL");
        Assumptions.assumeTrue(mongoUrl != null && !mongoUrl.isBlank(), "MONGO_URL non défini : tests d'intégration ignorés");

        mongo = MongoClients.create(mongoUrl);
        db = mongo.getDatabase("weatherapi");

        MongoDataDriver mongoDriver = new MongoDataDriver(mongoUrl);
        assertTrue(mongoDriver.init());

        listener = new ServerSocket(0);
        client = new Socket("127.0.0.1", listener.getLocalPort());
        client.setSoTimeout(10000);
        new ThreadServer(1, listener.accept(), new TestExchanger(new FakeDataDriver(), mongoDriver)).start();
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintStream(client.getOutputStream(), true);
    }

    @AfterAll
    void stopStack() throws Exception {
        if (client != null) client.close();
        if (listener != null) listener.close();
        if (mongo != null) mongo.close();
    }

    private String send(String line) throws Exception {
        out.println(line);
        return in.readLine();
    }

    @Test
    void getConfigReadsSendIntervalFromMongo() throws Exception {
        String key = UUID.randomUUID().toString();
        db.getCollection("modules").insertOne(new Document("name", "it module")
                .append("shortName", "it").append("key", key).append("uc", "esp32")
                .append("chipsets", List.of()).append("sendInterval", 42));

        String answer = send("GETCONFIG " + key);

        assertTrue(answer.startsWith("OK"), answer);
        assertTrue(answer.contains("42"), answer);
    }

    @Test
    void getConfigWithUnknownKeyFails() throws Exception {
        assertTrue(send("GETCONFIG " + UUID.randomUUID()).startsWith("ERR"));
    }

    @Test
    void storeAnalysisIsWrittenInMongo() throws Exception {
        String type = "it-analysis-" + UUID.randomUUID();

        assertEquals("OK", send("STOREANALYSIS " + type + " 2026-11-04T19:51:27 7"));

        Document stored = db.getCollection("measures").find(eq("type", type)).first();
        assertNotNull(stored, "la mesure doit être en base");
        assertEquals("7", stored.getString("value"));
    }
}
