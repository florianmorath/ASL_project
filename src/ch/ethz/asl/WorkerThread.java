package ch.ethz.asl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * A worker-thread will connect to all memcached servers and process requests from the request queue of the net-thread.
 */
public class WorkerThread extends Thread{

    private static final Logger logger = Logger.getLogger(WorkerThread.class.getName());

    private LinkedBlockingQueue<Request> requestQueue;
    private boolean readSharded;

    // networking
    private ArrayList<SocketChannel> socketChannels = new ArrayList<>();



    public WorkerThread(List<String> mcAddresses, boolean readSharded, LinkedBlockingQueue<Request> requestQueue) {
        this.requestQueue = requestQueue;
        this.readSharded = readSharded;

        connectionSetup(mcAddresses);

    }


    private void connectionSetup(List<String> mcAddresses) {
        for(String mcAddress: mcAddresses) {

            String serverIp = mcAddress.split(":")[0];
            int serverPort = Integer.parseInt(mcAddress.split(":")[1]);

            try {
                SocketChannel serverConnection;
                serverConnection = SocketChannel.open(new InetSocketAddress(serverIp, serverPort));
                serverConnection.configureBlocking(true); // we want a blocking channel
                socketChannels.add(serverConnection);
                logger.info( "Worker thread " + String.valueOf(this.getId()) +  " connected to " +
                        serverConnection.getRemoteAddress().toString() + " (" + serverConnection.isConnected() + ")");
            } catch (IOException ex) {
                logger.warning("Worker-thread " + this.getId() + " failed connection setup.");
                ex.printStackTrace();
            }

        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     */
    @Override
    public void run() {
        logger.info("worker-thread" + " " + String.valueOf(this.getId()) + " " + "running.");


    }
}
