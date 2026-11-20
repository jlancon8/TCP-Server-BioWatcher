import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoDatabase;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

public class MyWorld {

    final FakeDataDriver http = new FakeDataDriver();
    final FakeDataDriver mongo = new FakeDataDriver();

    ServerSocket listener;
    Socket client;
    ThreadServer thread;
    BufferedReader in;
    PrintStream out;
    String lastAnswer;

    HttpServer api;
    HttpDataDriver httpDriver;
    final AtomicReference<String> lastBody = new AtomicReference<>();
    final AtomicReference<String> lastUri = new AtomicReference<>();
    volatile String nextResponse = "{\"error\":0,\"status\":200,\"data\":null}";
    String driverAnswer;

    MongoClient mongoClient;
    MongoDatabase db;
    String moduleKey;
    String analysisType;

    void startServer(DataDriver httpDriver, DataDriver mongoDriver) throws Exception {
        listener = new ServerSocket(0);
        client = new Socket("127.0.0.1", listener.getLocalPort());
        client.setSoTimeout(10000);
        Socket accepted = listener.accept();
        thread = new ThreadServer(1, accepted, new TestExchanger(httpDriver, mongoDriver));
        thread.start();
        in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        out = new PrintStream(client.getOutputStream(), true);
    }

    String send(String line) throws Exception {
        out.println(line);
        return in.readLine();
    }

    void startFakeApi() throws Exception {
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
        httpDriver = new HttpDataDriver("http://127.0.0.1:" + api.getAddress().getPort() + "/api");
    }

    void close() throws Exception {
        if (client != null) client.close();
        if (listener != null) listener.close();
        if (thread != null) thread.join(2000);
        if (api != null) api.stop(0);
        if (mongoClient != null) mongoClient.close();
    }
}