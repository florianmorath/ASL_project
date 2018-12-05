package ch.ethz.asl;

import java.util.concurrent.LinkedBlockingQueue;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.net.InetSocketAddress;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * The net-thread is responsible for client connections. It will parse requests from clients and put them into a
 * queue.
 *
 * @author Florian Morath
 */
public class NetThread extends Thread {

    private static final Logger logger = LogManager.getLogger(NetThread.class.getName());

    /**
     * The selector is used to handle multiple client connections simultaneously by a single thread.
     */
    public Selector selector;

    /**
     * This channel is used to accept new client connections.
     */
    public ServerSocketChannel serverChannel;

    /**
     * The queue into which the net-thread puts its requests.
     */
    public LinkedBlockingQueue<Request> requestQueue;


    public NetThread(String myIp, int myPort, LinkedBlockingQueue<Request> requestQueue) {

        this.requestQueue = requestQueue;
        connectionSetup(myIp, myPort);
    }

    /**
     * Net-thread creates a selector and a server socket channel to which it binds to.
     * Clients can connect to this socket to establish new connections.
     *
     * @param myIp   ip of net-thread.
     * @param myPort port of net-thread.
     */
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
            logger.error("Error during Connection setup.");
            ex.printStackTrace();
        }
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The main-loop of the net-thread. The select call on the selector blocks until a new I/O event occurred.
     */
    @Override
    public void run() {
        logger.info("net-thread" + " " + String.valueOf(this.getId()) + " " + "running.");


        while (true) {
            try {
                // blocks until at least one channel is ready for I/O operations
                selector.select();
            } catch (IOException ex) {
                logger.error("Selected keys could not be updated.");
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
                    logger.error("Invalid SelectionKey");
                }

                iterator.remove();
            }
        }

    }

    /**
     * Called whenever an accept event occurred i.e a client wants to establish a new connection. A new socket channel
     * is created exclusively for the connection to the new client.
     */
    private void acceptClientConnection() {
        try {
            SocketChannel channel = serverChannel.accept();
            channel.configureBlocking(false);

            // selector listens for read events
            SelectionKey key = channel.register(selector, SelectionKey.OP_READ);

            // create a Buffer once for every client socket (will be cleared before client sends new request)
            ByteBuffer buffer = ByteBuffer.allocate(1024 * 5); // max 16B key, 4096B value for set request
            key.attach(buffer);

            logger.info("Client connection accepted and added to the selector.");

        } catch (ClosedChannelException ex) {
            logger.error("Channel closed.");
            ex.printStackTrace();

        } catch (IOException ex) {
            logger.error("Error while accepting client connection");
            ex.printStackTrace();
        }
    }

    /**
     * Called whenever a read event occurred i.e a client send some data which is now ready to be read.
     *
     * @param key the selection key returned by the selector.
     */
    public void readFromChannel(SelectionKey key) {

        long firstReadTime = System.nanoTime();

        ByteBuffer buffer = (ByteBuffer) key.attachment();

        SocketChannel channel = (SocketChannel) key.channel();
        int bytesReadCount = 0;
        try {

            // read data from channel into buffer
            bytesReadCount = channel.read(buffer);
            logger.info("Byte read count: ");
            logger.info(String.valueOf(bytesReadCount));


        } catch (IOException ex) {
            logger.error("Error while reading data from client");
            ex.printStackTrace();
        }

        // Channel has reached end of stream
        if (bytesReadCount == -1) {
            logger.info("Connection closure request by client.");
            try {
                key.channel().close();
            } catch (IOException ex) {
                logger.error("Could not close channel after client request connection closure.");
                ex.printStackTrace();
            }
            // Cancel registration of the channel to the selector
            key.cancel();
            return;
        }

        // check if whole request is in the buffer
        if (Request.endOfLineExists(buffer)) {
            // create and enqueue Request
            enqueueRequest(buffer, key, firstReadTime);
        } else {
            logger.info("Did not receive whole request yet.");
        }
    }

    /**
     * Enqueue new request.
     * @param buffer buffer containing the request.
     * @param key    key representing the connection over which the request was sent.
     * @param firstReadTime timestamp where first byte of request was read
     */
    public void enqueueRequest(ByteBuffer buffer, SelectionKey key, long firstReadTime) {
        Request req = new Request(buffer, key);
        req.timeFirstByte = firstReadTime;
        req.timeEnqueued = System.nanoTime();

        try {
            requestQueue.put(req);
            logger.info("request enqueued");
        } catch (Exception ex) {
            logger.error("Could not enqueue request.");
            ex.printStackTrace();
        }

    }

}
