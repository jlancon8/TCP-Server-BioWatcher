import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ProtocolSteps {

    private final MyWorld world;

    public ProtocolSteps(MyWorld world) {
        this.world = world;
    }

    @Given("un serveur TCP avec de faux drivers")
    public void startServer() throws Exception {
        world.startServer(world.http, world.mongo);
    }

    @Given("l'API répond {string}")
    public void apiAnswers(String answer) {
        world.http.answer = answer;
    }

    @Given("la base répond {string}")
    public void mongoAnswers(String answer) {
        world.mongo.answer = answer;
    }

    @When("le client envoie {string}")
    public void clientSends(String line) throws Exception {
        world.lastAnswer = world.send(line);
    }

    @When("le client envoie une ligne vide")
    public void clientSendsEmptyLine() {
        world.out.println("");
    }

    @Then("le serveur répond {string}")
    public void serverAnswers(String expected) {
        assertEquals(expected, world.lastAnswer);
    }

    @Then("l'API a reçu uniquement l'appel {string}")
    public void apiReceivedOnly(String call) {
        assertEquals(List.of(call), world.http.calls);
    }

    @Then("l'API n'a reçu aucun appel")
    public void apiReceivedNothing() {
        assertTrue(world.http.calls.isEmpty());
    }

    @Then("la base a reçu uniquement l'appel {string}")
    public void mongoReceivedOnly(String call) {
        assertEquals(List.of(call), world.mongo.calls);
    }

    @Then("la base n'a reçu aucun appel")
    public void mongoReceivedNothing() {
        assertTrue(world.mongo.calls.isEmpty());
    }

    @Then("la base a reçu {int} appels")
    public void mongoReceivedCount(int count) {
        assertEquals(count, world.mongo.calls.size());
    }

    @Then("le thread du serveur se termine")
    public void threadEnds() throws Exception {
        world.thread.join(2000);
        assertFalse(world.thread.isAlive());
    }
}