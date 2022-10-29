import server.Server;
import service.Service;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Service service = new Service();

        Server server = new Server(service);
        server.serverStart();
    }
}
