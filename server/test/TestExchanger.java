/**
 * DataExchanger dont les drivers sont remplacés (le code de production n'est pas modifié :
 * on surcharge simplement les getters).
 */
class TestExchanger extends DataExchanger {

    private final DataDriver http;
    private final DataDriver mongo;

    TestExchanger(DataDriver http, DataDriver mongo) {
        super("http://127.0.0.1:1/unused", "mongodb://127.0.0.1:1");
        this.http = http;
        this.mongo = mongo;
    }

    @Override
    public DataDriver getHttpDriver() {
        return http;
    }

    @Override
    public DataDriver getMongoDriver() {
        return mongo;
    }
}
