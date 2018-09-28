package ch.ethz.asl;

import java.util.List;
import java.util.logging.Logger;

/**
 * Starts the net-thread responsible for client connections. Starts the worker-threads responsible for server
 * connections.
 */
public class Middleware {

    private static final Logger logger = Logger.getLogger(Middleware.class.getName());

    public Middleware(String myIp, int myPort, List<String> mcAddresses, int numThreadsPTP, boolean readSharded) {
    }

    public void run() {
        logger.info("Middleware running");

    }
}
