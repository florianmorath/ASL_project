package ch.ethz.asl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;


import static org.junit.jupiter.api.Assertions.*;


class NetThreadIntegrationTest {
    public static int port = 1234;
    public static String ip = "localhost";

    NetThread net;
    TestClient client;

    @BeforeEach
    void setUp() {

        LinkedBlockingQueue<Request> queue = new LinkedBlockingQueue<>();
        net = new NetThread(ip, port, queue);
        net.start();

        client = new TestClient();
        client.start();
    }

    @Test
    void testEnqueue() throws InterruptedException {
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.put("get memtier-1234\r\n".getBytes());
        net.enqueueRequest(buf, null);

        assertTrue(net.requestQueue.size() == 1);
        assertTrue(net.requestQueue.take().buffer == buf);

    }

    @Test
    void testReadFromChannel() throws IOException, InterruptedException {
        net.requestQueue.clear();
        ByteBuffer buf = ByteBuffer.allocate(1024);
        buf.put("get memtier-1234\r\n".getBytes());
        buf.flip();
        client.channel.write(buf);
        client.sleep(100);
        assertTrue(net.requestQueue.size() == 1);
    }

    @AfterEach
    void tearDown() throws IOException {
        net.serverChannel.close();
    }
}

class TestClient extends Thread {
    public SocketChannel channel;

    @Override
    public void run() {
        super.run();
        try {
            channel = SocketChannel.open(new InetSocketAddress(NetThreadIntegrationTest.ip, NetThreadIntegrationTest.port));

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}