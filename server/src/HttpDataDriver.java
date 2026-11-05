import org.bson.Document;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;


public class HttpDataDriver implements DataDriver {

    private HttpClient client;
    private String apiURL;

    public HttpDataDriver(String apiURL) {
        this.apiURL = apiURL;
        client = HttpClient.newHttpClient();
    }

    public boolean init() {
        return true;
    }

    private String checkError(Document answer) {
        int error = answer.getInteger("error");
        if (error != 0) {
            return "ERR " + answer.getString("data");
        }
        return null;
    }

    private Document postRequest(String route, String payload) {
        Document doc = null;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiURL + route))
                .header("Content-Type", "application/json")
                .method("POST", HttpRequest.BodyPublishers.ofString(payload))
                .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println(response.body());
            // parse received JSON
            doc = Document.parse(response.body());
        } catch (InterruptedException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return doc;
    }

    private Document getRequest(String route) {
        Document doc = null;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiURL + route))
                .GET()
                .build();
        try {
            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            System.out.println(response.body());
            doc = Document.parse(response.body());
        } catch (InterruptedException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
        return doc;
    }

    public synchronized String autoRegisterModule(String uc, List<String> chipsets, double longitude, double latitude) {

        String payload = "{\"uc\": \"" + uc + "\", \"chipsets\": [";

        for (int i = 0; i < chipsets.size(); i++) {
            if (i > 0) payload += ",";
            payload += "\"" + chipsets.get(i) + "\"";
        }

        payload += "], \"position\": {";
        payload += "\"type\": \"Point\",";
        payload += "\"coordinates\": [" + longitude + "," + latitude + "]";
        payload += "}}";

        Document doc = postRequest("/module/register", payload);

        if (doc == null) {
            return "ERR cannot join the API";
        }

        String err = checkError(doc);
        if (err != null) return err;

        Document data = (Document) doc.get("data");

        String name = data.getString("name");
        String shortName = data.getString("shortName");
        String key = data.getString("key");

        return "OK " + name + "," + shortName + "," + key;
    }

    public synchronized String saveMeasure(String type, String date, String value, String moduleKey) {
        System.out.println("saveMeasure: type=" + type + ", date=" + date + ", value=" + value + ", moduleKey=" + moduleKey);
        String payload = "{\"type\": \"" + type + "\", \"date\": \"" + date + "\", \"value\": \"" + value + "\", \"moduleKey\": \"" + moduleKey + "\"}";
        return sendMeasure(payload);
    }

    public synchronized String saveAnalysis(String type, String date, String value) {

        String payload = "{\"type\": \"" + type + "\", \"date\": \"" + date + "\", \"value\": \"" + value + "\"}";
        return sendMeasure(payload);
    }

    private String sendMeasure(String payload) {
        Document doc = postRequest("/measure/create", payload);
        if (doc == null) {
            return "ERR cannot join the API";
        }
        String err = checkError(doc);
        if (err != null) return err;
        return "OK";
    }

    public synchronized String getConfig(String moduleKey) {
        String key = URLEncoder.encode(moduleKey, StandardCharsets.UTF_8);
        Document doc = getRequest("/module/config?key=" + key);
        if (doc == null) {
            return "ERR cannot join the API";
        }
        String err = checkError(doc);
        if (err != null) return err;
        Document data = (Document) doc.get("data");
        return "OK " + data.getInteger("sendInterval");
    }
}