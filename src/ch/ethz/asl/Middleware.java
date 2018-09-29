package ch.ethz.asl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Starts the net-thread responsible for client connections. Starts the worker-threads responsible for server
 * connections.
 */
public class Middleware {


    private NetThread   netThread;
    private ArrayList<WorkerThread> workerThreadPool;
    private LinkedBlockingQueue<Request> requestQueue;


    public Middleware(String myIp, int myPort, List<String> mcAddresses, int numThreadsPTP, boolean readSharded) {

        requestQueue = new LinkedBlockingQueue<>();
        workerThreadPool = new ArrayList<>();

        startNetThread(myIp, myPort);

        //TODO: create and start worker threads
    }

    private void startNetThread(String myIp, int myPort) {
        netThread = new NetThread(myIp, myPort, this.requestQueue);
        netThread.start();
    }


}
