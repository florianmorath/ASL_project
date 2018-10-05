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
 * A worker-thread will connect to all memcached servers and process requests from the request queue of the net-thread.
 */
public class WorkerThread extends Thread{

    private static final Logger logger = Logger.getLogger(WorkerThread.class.getName());

    private LinkedBlockingQueue<Request> requestQueue;
    private boolean readSharded;

    // networking
    private ArrayList<SocketChannel> socketChannels = new ArrayList<>();

    // round-robin load balancer
    private int lastServerIndex = 0;


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

        while(true){

            try {
                // dequeue request (blocks until a request becomes available)
                Request request = requestQueue.take();

                // handle request (send to memcached servers according to project specification)
                if (request.type == Request.Type.SET){
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

    private void handleSetRequest(Request request) throws IOException {
        logger.info("handle set request");

        request.buffer.flip();

        // send request to each server
        for (SocketChannel socketChannel: socketChannels){
            socketChannel.write(request.buffer);
            request.buffer.rewind(); // make buffer readable

        }

        ArrayList<String> errorMessages = new ArrayList<>();
        //TODO: determine required capacity
        ByteBuffer responseBuffer = ByteBuffer.allocate(100);

        // handle response from servers
        for (SocketChannel socketChannel: socketChannels) {
            // make buffer ready to be written to
            responseBuffer.clear();
            // read data from server
            socketChannel.read(responseBuffer);

            String response = new String(Arrays.copyOfRange(responseBuffer.array(), 0, responseBuffer.position()),
                    Charset.forName("UTF-8"));

            if(!response.equals("STORED\r\n")){
                logger.info("received non-success message from server after set request");
                logger.info(response);
                errorMessages.add(response);
            }
        }

        // clear request buffer
        request.buffer.clear();

        // respond to client
        SocketChannel clientSocketChannel = (SocketChannel)request.key.channel();

        if (errorMessages.isEmpty()){
            logger.info("set-request successfully executed on all servers");
            responseBuffer.flip(); // change from write into read mode
            clientSocketChannel.write(responseBuffer);
        } else {
            logger.info("set-request not successfully executed on all servers");

            // choose first error message
            ByteBuffer errorResponse = ByteBuffer.wrap(errorMessages.get(0).getBytes(Charset.forName("UTF-8" )));
            clientSocketChannel.write(errorResponse);
        }
    }

    private void handleGetRequest(Request request) throws IOException {
        logger.info("handle get request");

        if (!readSharded){
            // note: does not matter if Get or Multiget request

            // round-robin load balancer
            lastServerIndex = (lastServerIndex + 1) % socketChannels.size();
            SocketChannel serverSocketChannel = socketChannels.get(lastServerIndex);

            // forward request to chosen server
            request.buffer.flip();
            serverSocketChannel.write(request.buffer);

            // read response
            // TODO: determine capacity
            ByteBuffer responseBuffer = ByteBuffer.allocate(11*4096);
            int bytesReadCount = serverSocketChannel.read(responseBuffer);

            // debugging purpose
            logger.info("Byte read count from server: ");
            logger.info(String.valueOf(bytesReadCount));


            String response = new String(Arrays.copyOfRange(responseBuffer.array(), 0, responseBuffer.position()),
                    Charset.forName("UTF-8"));
            logger.info(response);

            // clear request buffer
            request.buffer.clear();

            // send response to client
            SocketChannel clientSocketChannel = (SocketChannel)request.key.channel();
            responseBuffer.flip(); // change from write into read mode
            clientSocketChannel.write(responseBuffer);
        } else {
            // sharded mode
        }
    }
}
