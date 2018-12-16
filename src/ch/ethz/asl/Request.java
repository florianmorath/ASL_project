package ch.ethz.asl;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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


    private static final Logger logger = LogManager.getLogger(Request.class.getName());
    private static final Logger requestLogger = LogManager.getLogger("request_logger");

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

    /**
     *  Instrumentation values
     */
    public long timeFirstByte;
    public long timeEnqueued;
    public long timeDequeued;
    public long timeMemcachedSent;
    public long timeMemcachedReceived;
    public long timeCompleted;
    public int queueLength;


    public Request(ByteBuffer buffer, SelectionKey key) {
        this.buffer = buffer;
        this.key = key;

        parseRequest();
    }


    /**
     * Checks if request corresponds to a Get or a Set request and checks if request ends with \r\n.
     * Sets the Type of the Request instance.
     */
    private void parseRequest() {

        if (!Request.endOfLineExists(buffer)) {
            // incomplete message
            type = Type.INVALID;
            logger.error("Incomplete request warning. Missing the two end of line bytes.");
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
            logger.error("Request invalid");
            type = Type.INVALID;
            logger.error("Request does not start with set or get command");
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

    public void writeLogLine() {
        requestLogger.debug(String.format("%s,%d,%d,%d,%d,%d,%d,%d", type, timeFirstByte, timeEnqueued, timeDequeued,
                timeMemcachedSent, timeMemcachedReceived, timeCompleted, queueLength));
    }

}
