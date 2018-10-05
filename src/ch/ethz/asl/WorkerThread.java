package ch.ethz.asl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;

/**
 * A worker-thread will connect to the given memcached servers and process requests from the request queue of the
 * net-thread.
 *
 * @author Florian Morath
 */
public class WorkerThread extends Thread {

    private static final Logger logger = Logger.getLogger(WorkerThread.class.getName());

    private LinkedBlockingQueue<Request> requestQueue;
    private boolean readSharded;

    // networking
    private ArrayList<SocketChannel> socketChannels = new ArrayList<>();
    // allocate response buffer (one is enough because of synchronous connections)
    private ByteBuffer responseBuffer = ByteBuffer.allocate(11 * 4096); // Multiget receives up to 10 values

    // round-robin load balancer
    // TODO: do empirical evaluation showing that all memcached servers are under same load
    private int lastServerIndex = 0;


    /**
     * @param mcAddresses  ip:port list of memcached servers to connect to.
     * @param readSharded  shareded mode activated.
     * @param requestQueue queue containing all requests from which worker thread can take.
     */
    public WorkerThread(List<String> mcAddresses, boolean readSharded, LinkedBlockingQueue<Request> requestQueue) {
        this.requestQueue = requestQueue;
        this.readSharded = readSharded;

        connectionSetup(mcAddresses);
    }


    /**
     * For each memcached server, create a socketChannel and try to connect to it. We setup a blocking channel to each.
     *
     * @param mcAddresses memcached servers.
     */
    private void connectionSetup(List<String> mcAddresses) {

        for (String mcAddress : mcAddresses) {

            String serverIp = mcAddress.split(":")[0];
            int serverPort = Integer.parseInt(mcAddress.split(":")[1]);

            try {
                SocketChannel serverConnection;
                serverConnection = SocketChannel.open(new InetSocketAddress(serverIp, serverPort));
                serverConnection.configureBlocking(true); // we want a blocking channel

                socketChannels.add(serverConnection);
                logger.info("Worker thread " + String.valueOf(this.getId()) + " connected to " +
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
     * <p>
     * The worker thread takes out a request of the queue, and processes it depending on the type. When finished it will
     * take antother request and so forth.
     */
    @Override
    public void run() {
        logger.info("worker-thread" + " " + String.valueOf(this.getId()) + " " + "running.");

        while (true) {

            try {
                // dequeue request (blocks until a request becomes available)
                Request request = requestQueue.take();

                // handle request (send to memcached servers according to project specification)
                if (request.type == Request.Type.SET) {
                    handleSetRequest(request);
                } else if (request.type == Request.Type.GET) {
                    handleGetRequest(request);
                } else {
                    // invalid request -> ignore it
                }


            } catch (Exception e) {
                logger.warning("Worker-thread failed. Thread id = " + String.valueOf(this.getId()));
                e.printStackTrace();
            }

        }
    }


    /**
     * Process Set request. Send it to each server and wait for response of all of them. If one server or multiple
     * responds with an error message, one of them will be forwarded to the client. If no error occurs, one of the
     * success messages is forwarded to the client.
     *
     * @param request a set request
     * @throws IOException
     */
    private void handleSetRequest(Request request) throws IOException {
        logger.info("handle set request");

        request.buffer.flip(); // sets limit to position and position to 0 (read mode)

        // send request to each server
        for (SocketChannel socketChannel : socketChannels) {
            socketChannel.write(request.buffer);
            request.buffer.rewind(); // make buffer readable again (set position to 0)
        }

        ArrayList<String> errorMessages = new ArrayList<>();

        // handle response from servers
        for (SocketChannel socketChannel : socketChannels) {
            // read data from server
            responseBuffer.clear();
            readDataFromSocket(socketChannel);

            // copying buffer is ok, because response to a set request is only a few bytes
            String response = new String(Arrays.copyOfRange(responseBuffer.array(), 0, responseBuffer.position()),
                    Charset.forName("UTF-8"));

            if (!response.equals("STORED\r\n")) {
                logger.info("received non-success message from server after set request");
                logger.info(response);
                errorMessages.add(response);
            }
        }

        // respond to client
        request.buffer.clear(); // allows client to send new request
        SocketChannel clientSocketChannel = (SocketChannel) request.key.channel();

        if (errorMessages.isEmpty()) {
            logger.info("set-request successfully executed on all servers");
            responseBuffer.flip(); // change from write into read mode
            clientSocketChannel.write(responseBuffer);
        } else {
            logger.info("set-request not successfully executed on all servers");

            // choose first error message
            ByteBuffer errorResponse = ByteBuffer.wrap(errorMessages.get(0).getBytes(Charset.forName("UTF-8")));
            clientSocketChannel.write(errorResponse);
        }
    }

    /**
     * Process Get request. If we are in non sharded mode, no mather if Get or Multiget, the request is forwarded to
     * one server (round robin) and the response will be forwarded to the client.
     *
     * @param request a Get request.
     * @throws IOException
     */
    private void handleGetRequest(Request request) throws IOException {
        logger.info("handle get request");

        if (!readSharded) {
            // note: does not matter if Get or Multiget request

            // round-robin load balancer
            lastServerIndex = (lastServerIndex + 1) % socketChannels.size();
            SocketChannel socketChannel = socketChannels.get(lastServerIndex);

            // forward request to chosen server
            request.buffer.flip();
            socketChannel.write(request.buffer);

            // read response
            responseBuffer.clear();
            int bytesReadCount = readDataFromSocket(socketChannel);

            // debugging purpose
            logger.info("Byte read count from server: ");
            logger.info(String.valueOf(bytesReadCount));

            // Debugging purpose (remove for efficiency)
            // String response = new String(Arrays.copyOfRange(responseBuffer.array(), 0, responseBuffer.position()),
            //        Charset.forName("UTF-8"));
            // logger.info(response);


            // send response to client
            SocketChannel clientSocketChannel = (SocketChannel) request.key.channel();
            responseBuffer.flip(); // change from write into read mode
            request.buffer.clear(); // allows client to send new request
            clientSocketChannel.write(responseBuffer);
        } else {
            // sharded mode
        }
    }

    /**
     * We might not be able to read the whole response in one go. If this is the case, then another blocking read is
     * invoked on the socketChannel until a new line is read.
     *
     * @param socketChannel socket channel to read from.
     * @return the number of bytes read in total.
     * @throws IOException
     */
    private int readDataFromSocket(SocketChannel socketChannel) throws IOException {
        // read data until we have the whole response
        int bytesReadCount = 0;

        do {
            bytesReadCount += socketChannel.read(responseBuffer);

        } while (!Request.endOfLineExists(responseBuffer));

        return bytesReadCount;
    }

}
