package my.home.server;

import java.io.IOException;

public class ServerApp {
    public static void main(String[] args) {
        ConnectionProcessor cp = new ConnectionProcessor();
        cp.start();
    }
}
