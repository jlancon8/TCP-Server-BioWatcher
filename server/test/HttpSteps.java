import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.bson.Document;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HttpSteps {

    private final MyWorld world;

    public HttpSteps(MyWorld world) {
        this.world = world;
    }

    private Document sentBody() {
        return Document.parse(world.lastBody.get());
    }

    @Given("une fausse API HTTP embarquée")
    public void startFakeApi() throws Exception {
        world.startFakeApi();
    }

    @Given("la fausse API répond avec le JSON :")
    public void fakeApiAnswers(String json) {
        world.nextResponse = json.trim();
    }

    @When("la fausse API est arrêtée")
    public void stopFakeApi() {
        world.api.stop(0);
    }

    @When("le driver HTTP enregistre un module {string} avec les chipsets {string} en {double} {double}")
    public void register(String uc, String chipsets, double longitude, double latitude) {
        world.driverAnswer = world.httpDriver.autoRegisterModule(uc, List.of(chipsets.split(",")), longitude, latitude);
    }

    @When("le driver HTTP sauvegarde la mesure {string} {string} {string} avec la clé {string}")
    public void saveMeasure(String type, String date, String value, String key) {
        world.driverAnswer = world.httpDriver.saveMeasure(type, date, value, key);
    }

    @When("le driver HTTP sauvegarde l'analyse {string} {string} {string}")
    public void saveAnalysis(String type, String date, String value) {
        world.driverAnswer = world.httpDriver.saveAnalysis(type, date, value);
    }

    @When("le driver HTTP demande la config de la clé {string}")
    public void getConfig(String key) {
        world.driverAnswer = world.httpDriver.getConfig(key);
    }

    @Then("le driver répond {string}")
    public void driverAnswers(String expected) {
        assertEquals(expected, world.driverAnswer);
    }

    @Then("la fausse API a reçu la requête {string}")
    public void apiReceivedRequest(String request) {
        assertEquals(request, world.lastUri.get());
    }

    @Then("le corps JSON a le champ {string} valant {string}")
    public void bodyHasField(String field, String value) {
        assertEquals(value, sentBody().getString(field));
    }

    @Then("le corps JSON a le champ {string} avec la liste {string}")
    public void bodyHasList(String field, String values) {
        assertEquals(List.of(values.split(",")), sentBody().getList(field, String.class));
    }

    @Then("le corps JSON n'a pas le champ {string}")
    public void bodyHasNoField(String field) {
        assertFalse(sentBody().containsKey(field));
    }

    @Then("la position du corps JSON est un Point en {double} {double}")
    public void bodyPosition(double longitude, double latitude) {
        Document position = sentBody().get("position", Document.class);
        assertEquals("Point", position.getString("type"));
        assertEquals(List.of(longitude, latitude), position.getList("coordinates", Double.class));
    }
}