package ch.ethz.asl;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Entry point of application. Parses the arguments and starts the Middleware.
 */
public class RunMW {

    /**
     * IP-address of the net-thread (memtier client connections).
     */
    static String myIp = null;

    /**
     * Port to listen on by net-thread (memtier client connections).
     */
    static int myPort = 0;

    /**
     * IP-addresses and ports (ip:port) of memchached-servers.
     */
    static List<String> mcAddresses = null;

    /**
     * Number of worker-threads in the worker thread pool.
     */
    static int numThreadsPTP = -1;

    /**
     * Sharded reads enabled.
     */
    static boolean readSharded = false;


    private static final Logger logger = LogManager.getLogger(RunMW.class.getName());


    public static void main(String[] args) throws Exception {


        logger.info("RunMW invoked");

        // -----------------------------------------------------------------------------
        // Parse and prepare arguments
        // -----------------------------------------------------------------------------

        logger.info("parse arguments");
        parseArguments(args);

        // -----------------------------------------------------------------------------
        // Start the Middleware
        // -----------------------------------------------------------------------------

        logger.info("start middleware");
        new Middleware(myIp, myPort, mcAddresses, numThreadsPTP, readSharded);
    }

    private static void parseArguments(String[] args) {
        Map<String, List<String>> params = new HashMap<>();

        List<String> options = null;
        for (int i = 0; i < args.length; i++) {
            final String a = args[i];

            if (a.charAt(0) == '-') {
                if (a.length() < 2) {
                    System.err.println("Error at argument " + a);
                    System.exit(1);
                }

                options = new ArrayList<String>();
                params.put(a.substring(1), options);
            } else if (options != null) {
                options.add(a);
            } else {
                System.err.println("Illegal parameter usage");
                System.exit(1);
            }
        }

        if (params.size() == 0) {
            printUsageWithError(null);
            System.exit(1);
        }

        if (params.get("l") != null)
            myIp = params.get("l").get(0);
        else {
            printUsageWithError("Provide this machine's external IP! (see ifconfig or your VM setup)");
            System.exit(1);
        }

        if (params.get("p") != null)
            myPort = Integer.parseInt(params.get("p").get(0));
        else {
            printUsageWithError("Provide the port, that the middleware listens to (e.g. 11212)!");
            System.exit(1);
        }

        if (params.get("m") != null) {
            mcAddresses = params.get("m");
        } else {
            printUsageWithError(
                    "Give at least one memcached backend server IP address and port (e.g. 123.11.11.10:11211)!");
            System.exit(1);
        }

        if (params.get("t") != null)
            numThreadsPTP = Integer.parseInt(params.get("t").get(0));
        else {
            printUsageWithError("Provide the number of threads for the threadpool!");
            System.exit(1);
        }

        if (params.get("s") != null)
            readSharded = Boolean.parseBoolean(params.get("s").get(0));
        else {
            printUsageWithError("Provide true/false to enable sharded reads!");
            System.exit(1);
        }

    }

    private static void printUsageWithError(String errorMessage) {
        System.err.println();
        System.err.println(
                "Usage: -l <MyIP> -p <MyListenPort> -t <NumberOfThreadsInPool> -s <readSharded> -m <MemcachedIP:Port> <MemcachedIP2:Port2> ...");
        if (errorMessage != null) {
            System.err.println();
            System.err.println("Error message: " + errorMessage);
        }

    }
}
