package ch.ethz.asl;

import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * A worker-thread will connect to all memcached servers and process requests from the request queue of the net-thread.
 */
public class WorkerThread extends Thread{

    private static final Logger logger = Logger.getLogger(WorkerThread.class.getName());


    public WorkerThread(List<String> mcAddresses, boolean readSharded, LinkedBlockingQueue<Request> requestQueue) {

    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     */
    @Override
    public void run() {
        logger.info("worker-thread" + " " + this.getName() + " " + "started.");


    }
}
