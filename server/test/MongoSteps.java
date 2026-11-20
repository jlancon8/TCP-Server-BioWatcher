import com.mongodb.client.MongoClients;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;
import org.junit.jupiter.api.Assumptions;

import java.util.List;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;
import static org.junit.jupiter.api.Assertions.*;

public class MongoSteps {

    private final MyWorld world;

    public MongoSteps(MyWorld world) {
        this.world = world;
    }

    @Given("une pile TCP branchée sur un vrai MongoDB")
    public void startStack() throws Exception {
        String mongoUrl = System.getenv("MONGO_URL");
        Assumptions.assumeTrue(mongoUrl != null && !mongoUrl.isBlank(), "MONGO_URL non défini : scénario ignoré");

        world.mongoClient = MongoClients.create(mongoUrl);
        world.db = world.mongoClient.getDatabase("weatherapi");

        MongoDataDriver mongoDriver = new MongoDataDriver(mongoUrl);
        assertTrue(mongoDriver.init());
        world.startServer(world.http, mongoDriver);
    }

    @Given("un module existe en base avec un intervalle d'envoi de {int}")
    public void moduleExists(int interval) {
        world.moduleKey = UUID.randomUUID().toString();
        world.db.getCollection("modules").insertOne(new Document("name", "it module")
                .append("shortName", "it").append("key", world.moduleKey).append("uc", "esp32")
                .append("chipsets", List.of()).append("sendInterval", interval));
    }

    @When("le client demande la config de ce module")
    public void getConfig() throws Exception {
        world.lastAnswer = world.send("GETCONFIG " + world.moduleKey);
    }

    @When("le client demande la config d'une clé inconnue")
    public void getConfigUnknown() throws Exception {
        world.lastAnswer = world.send("GETCONFIG " + UUID.randomUUID());
    }

    @When("le client stocke une analyse unique de valeur {string}")
    public void storeAnalysis(String value) throws Exception {
        world.analysisType = "it-analysis-" + UUID.randomUUID();
        world.lastAnswer = world.send("STOREANALYSIS " + world.analysisType + " 2026-11-04T19:51:27 " + value);
    }

    @Then("la réponse du serveur commence par {string}")
    public void answerStartsWith(String prefix) {
        assertTrue(world.lastAnswer.startsWith(prefix), world.lastAnswer);
    }

    @Then("la réponse du serveur contient {string}")
    public void answerContains(String text) {
        assertTrue(world.lastAnswer.contains(text), world.lastAnswer);
    }

    @Then("la mesure est enregistrée en base avec la valeur {string}")
    public void measureStored(String value) {
        Document stored = world.db.getCollection("measures").find(eq("type", world.analysisType)).first();
        assertNotNull(stored, "la mesure doit être en base");
        assertEquals(value, stored.getString("value"));
    }
}