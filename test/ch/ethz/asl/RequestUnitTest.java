package ch.ethz.asl;

import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.*;

class RequestUnitTest {

    @Test
    void endOfLineExists() {

        ByteBuffer b1 = ByteBuffer.allocate(100);
        b1.put("get key1 key2\r\n".getBytes());

        ByteBuffer b2 = ByteBuffer.allocate(100);
        b2.put("set key1 0 500 30\r\nxxxxxxxxxxxxxxxxxxxx\r\n".getBytes());

        ByteBuffer b3 = ByteBuffer.allocate(100);
        b3.put("set key1 0 500 30\r\nxxxx\r\nxxxxxxxxxxxxxxxx\r\n".getBytes());

        ByteBuffer b4 = ByteBuffer.allocate(100);
        b4.put("get key1 key2".getBytes());

        ByteBuffer b5 = ByteBuffer.allocate(100);
        b5.put("set key1 0 500 30\r\nxxxx\r\nxxxxxxxxxxxxxxxx".getBytes());


        assertTrue(Request.endOfLineExists(b1));
        assertTrue(Request.endOfLineExists(b2));
        assertTrue(Request.endOfLineExists(b3));
        assertFalse(Request.endOfLineExists(b4));
        assertFalse(Request.endOfLineExists(b5));

    }
}