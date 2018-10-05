package ch.ethz.asl;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.charset.Charset;
import java.util.logging.Logger;

/**
 * Represents a requests from a client.
 *
 * Structure of different kind of Memtier requests:
 * Get Request:         "get <key>\r\n"
 * Set Request:         "set <key> <flag> <expiry> <value length> \r\n <value>\r\n"
 * Multiget Request:    "get <key1> <key2> <key3>\r\n"
 *
 */
public class Request {

    private static final Logger logger = Logger.getLogger(Request.class.getName());


    public enum Type {
        SET,
        GET,
        INVALID
    }

    // contains data of request
    public ByteBuffer buffer;

    // contains information about channel over which request was sent
    public SelectionKey key;

    // request type
    public Type type = Type.INVALID;

    public Request(ByteBuffer buffer, SelectionKey key){
        this.buffer = buffer;
        this.key = key;

        parseRequest();
    }


    /**
     * Checks if request corresponds to get resp. set request and checks if request ends with \r\n.
     * Sets the Type of the Request instance. (INVALID will be discarded by worker thread)
     * Parses the components of a request.
     *
     */
    private void parseRequest(){

        // log received message (debugging purpose -> remove for efficiency)
        String requestMessage = new String(buffer.array(), Charset.forName("UTF-8"));
        logger.info(requestMessage);


        if (!Request.validBuffer(buffer)) {
            // incomplete message
            type = Type.INVALID;
            logger.warning("Incomplete request warning. Missing the two end of line bytes.");
        }

        // check and assign type
        String requestType = new String(buffer.array(), 0, 3, Charset.forName("UTF-8"));
        if(requestType.equals("get")){
            type = Type.GET;
            logger.info("Detected Get request");
        } else if(requestType.equals("set")){
            logger.info("Detected Set request");
            type = Type.SET;
        } else {
            logger.warning("Request invalid");
            type = Type.INVALID;
            logger.warning("Request does not start with set or get command");
        }


    }

    public static boolean validBuffer(ByteBuffer buffer) {

        //check \r\n at end of request
        byte slash_r = (byte)'\r';
        byte slash_n = (byte)'\n';

        // position of last written byte
        int lastPosition = buffer.position();

        if(!(buffer.array()[lastPosition-1] == slash_n && buffer.array()[lastPosition-2] == slash_r)) {
           return false;
        } else {
            return true;
        }
    }

}
