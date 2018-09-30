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
 * Set Request:         "set <key> 0 <expiry> <value length> <value>\r\n"
 * Multiget Request:    "get <key1> <key2> <key3>\r\n"
 *
 *
 */
public class Request {

    private static final Logger logger = Logger.getLogger(Request.class.getName());


    public static enum Type {
        SET,
        GET,
        MULTIGET,
        INVALID
    }

    // contains data of request
    public ByteBuffer buffer;

    // contains information about channel over which request was sent
    public SelectionKey key;

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
        //TODO: implement parsing

        // change buffer from write mode to read mode
        buffer.flip();
        String requestMessage = new String( buffer.array(), Charset.forName("UTF-8") );
        logger.info("ByteBuffer as utf-8 string");
        logger.info(requestMessage);

    }

}
