import io.cucumber.java.After;

public class Hooks {

    private final MyWorld world;

    public Hooks(MyWorld world) {
        this.world = world;
    }

    @After
    public void tearDown() throws Exception {
        world.close();
    }
}