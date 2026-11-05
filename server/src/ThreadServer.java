import org.bson.types.ObjectId;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;

class ThreadServer extends Thread {

    BufferedReader br;
    PrintStream ps;
    Socket sock;
    DataExchanger exchanger;
    int idThread;

    public ThreadServer(int idThread, Socket sock, DataExchanger data) {
        this.sock = sock;
        this.idThread = idThread;
        this.exchanger = data;
    }

    public void run() {

        try {
            br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            ps = new PrintStream(sock.getOutputStream());
        } catch (IOException e) {
            System.err.println("Thread " + idThread + ": cannot create streams. Aborting.");
            return;
        }
        requestLoop();
        System.out.println("end of thread " + idThread);
    }

    public void requestLoop() {

        String req = "";
        String idReq;
        String[] reqParts;

        try {
            while (true) {
                req = br.readLine();
                System.out.println("reçu: [" + req + "]");
                if ((req == null) || (req.isEmpty())) {
                    break;
                }

                reqParts = req.split(" ");
                idReq = reqParts[0];

                if ("AUTOREGISTER".equals(idReq)) {
                    requestAutoRegister(reqParts);
                } else if ("STOREMEASURE".equals(idReq)) {
                    requestStoreMeasure(reqParts);
                } else if ("STOREANALYSIS".equals(idReq)) {
                    requestStoreAnalysis(reqParts);
                } else if ("GETCONFIG".equals(idReq)) {
                    requestGetConfig(reqParts);
                } else {
                    ps.println("ERR unknown command");
                }
            }
            System.out.println("end of request loop");
        } catch (IOException e) {
            System.out.println("problem with receiving request: " + e.getMessage());
        }
    }

    protected void requestAutoRegister(String[] params) throws IOException {

        System.out.println("processing request AUTO REGISTER");

        if (params.length < 5) {
            ps.println("ERR invalid number of parameters");
            return;
        }

        List<String> lst = new ArrayList<>();

        for (int i = 2; i < params.length - 2; i++) {
            lst.add(params[i]);
        }

        double longitude;
        double latitude;

        try {
            longitude = Double.parseDouble(params[params.length - 2]);
            latitude = Double.parseDouble(params[params.length - 1]);
            System.out.println("UC : " + params[1]);
            System.out.println("CHIPSETS : " + lst);
            System.out.println("LONGITUDE : " + longitude);
            System.out.println("LATITUDE : " + latitude);

            if (longitude < -180 || longitude > 180 ||
                    latitude < -90 || latitude > 90) {
                ps.println("ERR coordinates out of range");
                return;
            }

        } catch (NumberFormatException e) {
            ps.println("ERR invalid coordinates");
            return;
        }

        String answer = exchanger.getHttpDriver()
                .autoRegisterModule(params[1], lst, longitude, latitude);

        System.out.println(answer);
        ps.println(answer);
    }

    protected void requestStoreMeasure(String[] params) throws IOException {
        System.out.println("processing request STORE MEASURE");

        if (params.length != 5) {
            ps.println("ERR invalid number of parameters");
            return;
        }
        // (un)comment to choose direct mongo access or through the node API
        //String answer = exchanger.getMongoDriver().saveMeasure(params[1], params[2], params[3], params[4]);
        String answer = exchanger.getHttpDriver().saveMeasure(params[1], params[2], params[3], params[4]);
        System.out.println(answer);
        ps.println(answer);
    }

    protected void requestStoreAnalysis(String[] params) throws IOException {
        System.out.println("processing request STORE ANALYSIS");

        if (params.length != 4) {
            ps.println("ERR invalid number of parameters");
            return;
        }
        // (un)comment to choose direct mongo access or through the node API
        String answer = exchanger.getMongoDriver().saveAnalysis(params[1], params[2], params[3]);
        //String answer = exchanger.getHttpDriver().saveAnalysis(params[1], params[2], params[3]);
        System.out.println(answer);
        ps.println(answer);
    }

    protected void requestGetConfig(String[] params) throws IOException {
        System.out.println("processing request GET CONFIG");

        if (params.length != 2) {
            ps.println("ERR invalid number of parameters");
            return;
        }

        String answer = exchanger.getMongoDriver().getConfig(params[1]);
        //String answer = exchanger.getHttpDriver().getConfig(params[1]);
        System.out.println(answer);
        ps.println(answer);
    }

}


