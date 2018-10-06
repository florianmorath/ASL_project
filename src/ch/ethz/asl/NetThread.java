package ch.ethz.asl;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Logger;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.io.IOException;



/**
 * The net-thread is responsible for client connections. It will parse requests from clients and put them into a
 * queue.
 *
 */
public class NetThread extends Thread {

    private static final Logger logger = Logger.getLogger(NetThread.class.getName());

    // Networking
    private Selector selector;
    private ServerSocketChannel serverChannel;

    private LinkedBlockingQueue<Request> requestQueue;


    public NetThread(String myIp, int myPort, LinkedBlockingQueue<Request> requestQueue) {

        this.requestQueue = requestQueue;
        connectionSetup(myIp, myPort);
    }

    private void connectionSetup(String myIp, int myPort) {
        try {
            // Create Selector
            selector = Selector.open();

            // Create and configure ServerSocket
            serverChannel = ServerSocketChannel.open();
            serverChannel.socket().bind(new InetSocketAddress(myIp, myPort));
            serverChannel.configureBlocking(false);

            // register the channel for accept events
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException ex) {
            logger.warning("Error during Connection setup.");
            ex.printStackTrace();
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
        logger.info("net-thread" + " " + String.valueOf(this.getId()) + " " + "running.");


        while (true) {
            try {
                // blocks until at least one channel is ready for I/O operations
                selector.select();
            } catch (IOException ex) {
                logger.warning("Selected keys could not be updated.");
                ex.printStackTrace();
            }

            // Iterate over selected keys of Selector
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();

                if (key.isAcceptable()) {
                    logger.info("client requests a connection");
                    acceptClientConnection();

                } else if (key.isReadable()) {
                    logger.info("data ready to read from");
                    readFromChannel(key);

                } else {
                    logger.warning("Invalid SelectionKey");
                }

                iterator.remove();
            }
        }

    }

    private void acceptClientConnection() {
        try {
            SocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);

            // selector listens for read events
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);

            // create a Buffer once for every client socket (will be cleared before client sends new request)
            ByteBuffer buffer = ByteBuffer.allocate(1024*5); // max 16B key, 4096B value for set request
            key.attach(buffer);

            logger.info("Client connection accepted and added to the selector.");

        } catch(ClosedChannelException ex){
            logger.warning("Channel closed.");
            ex.printStackTrace();

        } catch (IOException ex) {
            logger.warning("Error while accepting client connection");
            ex.printStackTrace();
        }
    }

    private void readFromChannel(SelectionKey key) {
        ByteBuffer buffer = (ByteBuffer) key.attachment();

        SocketChannel channel = (SocketChannel) key.channel();
        int bytesReadCount = 0;
        try {
            // debugging purpose -> remove
            String requestMsg = new String(Arrays.copyOfRange(buffer.array(), 0, buffer.position()),
                    Charset.forName("UTF-8"));
            logger.info("buffer before client read : " + requestMsg);

            // read data from channel into buffer
            bytesReadCount = channel.read(buffer);
            logger.info("Byte read count: ");
            logger.info(String.valueOf(bytesReadCount));

            // debugging purpose -> remove
            String requestMsg2 = new String(Arrays.copyOfRange(buffer.array(), 0, buffer.position()),
                    Charset.forName("UTF-8"));
            logger.info("bytes read from client: " + requestMsg2);

        } catch (IOException ex) {
            logger.warning("Error while reading data from client");
            ex.printStackTrace();
        }

        // Channel has reached end of stream
        if (bytesReadCount == -1) {
            logger.info("Connection closure request by client.");
            try {
                key.channel().close();
            } catch (IOException ex) {
                logger.warning("Could not close channel after client request connection closure.");
                ex.printStackTrace();
            }
            // Cancel registration of the channel to the selector
            key.cancel();
            return;
        }

        // check if whole request is in the buffer
        if (Request.endOfLineExists(buffer)) {
            // create and enqueue Request
            enqueueRequest(buffer, key);
        } else {
            logger.info("Did not receive whole request yet.");
        }
    }

    private void enqueueRequest(ByteBuffer buffer, SelectionKey key){
        Request req = new Request(buffer, key);

        try{
            requestQueue.put(req);
            logger.info("request enqueued");
        } catch (Exception ex){
            logger.warning("Could not enqueue request.");
            ex.printStackTrace();
        }

    }

}
