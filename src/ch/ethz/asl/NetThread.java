package ch.ethz.asl;

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

    private static final Logger logger = Logger.getLogger(Middleware.class.getName());

    // Networking
    private Selector selector;
    private ServerSocketChannel serverChannel;

    private LinkedBlockingQueue<Request> requestQueue;


    public NetThread(String myIp, int myPort, LinkedBlockingQueue<Request> requestQueue) {

        this.requestQueue = requestQueue;

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
        logger.info("net-thread started");

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
            channel.register(selector, SelectionKey.OP_READ);
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
        //TODO: a request may be encoded in multiple network packets (set requests)
        ByteBuffer buffer = ByteBuffer.allocate(4096);
        SocketChannel channel = (SocketChannel) key.channel();
        int bytesReadCount = 0;
        try {
            // read data from channel into buffer
            bytesReadCount = channel.read(buffer);
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
        }

        // create and enqueue Request
        enqueueRequest(buffer, key);
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
