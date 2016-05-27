package com.github.dmtk;

import java.net.SocketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {
private final static Logger log = LogManager.getLogger(SensorNetworkServer.class);
    public static void main(String[] args) {

        try {

            // create server
            SensorNetworkServer server = new SensorNetworkServer();
            server.start();

        } catch (SocketException e) {
            log.error("Failed to initialize server: " + e.getMessage());
        }

    }
}


