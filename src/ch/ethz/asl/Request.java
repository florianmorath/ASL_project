package ch.ethz.asl;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * Represents a requests from a Memtier client.
 * <p>
 * Structure of different kind of Memtier requests:
 * Get Request:         "get <key>\r\n"
 * Set Request:         "set <key> <flag> <expiry> <value length> \r\n <value>\r\n"
 * Multiget Request:    "get <key1> <key2> <key3>\r\n"
 *
 * @author Florian Morath
 */
public class Request {


    private static final Logger logger = Logger.getLogger(Request.class.getName());

    /**
     * A request is either a Get (includes Multiget), a Set or an invalid request.
     */
    public enum Type {
        SET,
        GET,
        INVALID
    }

    /**
     * Contains data of request.
     */
    public ByteBuffer buffer;

    /**
     * Contains information about the channel over which request was sent. Is needed to be able to return response to
     * the client.
     */
    public SelectionKey key;

    /**
     * Request type.
     */
    public Type type = Type.INVALID;


    public Request(ByteBuffer buffer, SelectionKey key) {
        this.buffer = buffer;
        this.key = key;

        parseRequest();
    }


    /**
     * Checks if request corresponds to a Get or a Set request and checks if request ends with \r\n.
     * Sets the Type of the Request instance.
     * <p>
     * note: INVALID requests will be discarded by the worker thread.
     */
    private void parseRequest() {

        // log received message (debugging purpose -> remove for efficiency)
        String requestMessage = new String(buffer.array(), Charset.forName("UTF-8"));
        logger.info(requestMessage);

        if (!Request.endOfLineExists(buffer)) {
            // incomplete message
            type = Type.INVALID;
            logger.warning("Incomplete request warning. Missing the two end of line bytes.");
        }

        // check and assign type
        String requestType = new String(buffer.array(), 0, 3, Charset.forName("UTF-8"));
        if (requestType.equals("get")) {
            type = Type.GET;
            logger.info("Detected Get request");
        } else if (requestType.equals("set")) {
            logger.info("Detected Set request");
            type = Type.SET;
        } else {
            logger.warning("Request invalid");
            type = Type.INVALID;
            logger.warning("Request does not start with set or get command");
        }

    }

    /**
     * Checks if buffer contains a new line at last position written to it.
     * <p>
     * note: this only works if the buffer has not been flipped yet i.e position must be at last written byte
     *
     * @param buffer the ByteBuffer which we check.
     * @return true if there is a new line at the end of request.
     */
    public static boolean endOfLineExists(ByteBuffer buffer) {

        //check \r\n at end of request
        byte slash_r = (byte) '\r';
        byte slash_n = (byte) '\n';

        // position of last written byte
        int lastPosition = buffer.position();

        return (buffer.get(lastPosition - 1) == slash_n && buffer.get(lastPosition - 2) == slash_r);
    }

}
