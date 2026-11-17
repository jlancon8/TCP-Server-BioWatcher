import java.util.ArrayList;
import java.util.List;

/** Driver factice : enregistre les appels et renvoie une réponse configurable. */
class FakeDataDriver implements DataDriver {

    final List<String> calls = new ArrayList<>();
    String answer = "OK";
    boolean initResult = true;

    @Override
    public boolean init() {
        calls.add("init");
        return initResult;
    }

    @Override
    public synchronized String saveMeasure(String type, String date, String value, String moduleKey) {
        calls.add("saveMeasure " + type + " " + date + " " + value + " " + moduleKey);
        return answer;
    }

    @Override
    public synchronized String saveAnalysis(String type, String date, String value) {
        calls.add("saveAnalysis " + type + " " + date + " " + value);
        return answer;
    }

    @Override
    public synchronized String autoRegisterModule(String uc, List<String> chipsets, double longitude, double latitude) {
        calls.add("autoRegister " + uc + " " + chipsets + " " + longitude + " " + latitude);
        return answer;
    }

    @Override
    public synchronized String getConfig(String moduleKey) {
        calls.add("getConfig " + moduleKey);
        return answer;
    }
}
