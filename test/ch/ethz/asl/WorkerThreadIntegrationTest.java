package ch.ethz.asl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.LinkedBlockingQueue;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class WorkerThreadIntegrationTest {

    @Mock
    SelectionKey key;

    @Mock
    SocketChannel channel;


    MemcachedServer1 server1 = new MemcachedServer1();
    MemcachedServer2 server2 = new MemcachedServer2();
    MemcachedServer3 server3 = new MemcachedServer3();
    WorkerThread worker;


    @BeforeEach
    void setUp() throws InterruptedException {

        MockitoAnnotations.initMocks(this);

        when(key.channel()).thenReturn(channel);


        server1.start();
        server2.start();
        server3.start();

        Thread.sleep(100);

        LinkedBlockingQueue<Request> queue = new LinkedBlockingQueue<>();
        ArrayList<String> mcAddresses = new ArrayList<>();
        mcAddresses.add("127.0.0.1:11211");
        mcAddresses.add("127.0.0.1:11212");
        mcAddresses.add("127.0.0.1:11213");
        worker = new WorkerThread(mcAddresses, false, queue);

        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() throws IOException {
        for (SocketChannel socket : worker.socketChannels) {
            socket.close();
        }

        server1.interrupt();
        server2.interrupt();
        server3.interrupt();
        worker.interrupt();

    }

    @Test
    void handleSetRequest1() throws IOException, InterruptedException {

        // normal set request
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("set memtier-123 0 900 10\r\nxxxxxxxxxx\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleSetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);

        verify(channel).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals("STORED\r\n", response);


    }

    @Test
    void handleSetRequest2() throws IOException, InterruptedException {

        // invalid set request
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("memtier-123 0 900 10\r\nxxxxxxxxxx\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleSetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertTrue("ERROR\r\nERROR\r\n".equals(response) || "ERROR\r\n".equals(response));


    }

    @Test
    void handleSetRequest3() throws IOException, InterruptedException {

        // invalid set request
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("memtier-123 0 900 10 xxxxxxxxxx\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleSetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals("ERROR\r\n", response);

    }

    @Test
    void handleGetRequest1() throws IOException, InterruptedException {
        // valid non sharded, get request with one key
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("get random_key\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleGetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals("END\r\n", response);

    }

    @Test
    void handleGetRequest2() throws IOException, InterruptedException {
        // valid non sharded, multiget request with two key
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("get random_key random_key2\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleGetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals("END\r\n", response);

    }

    @Test
    void handleGetRequest3() throws IOException, InterruptedException {
        // invalid non sharded, get request
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("random_key random_key2\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleGetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals("ERROR\r\n", response);

    }

    @Test
    void handleGetRequest4() throws IOException, InterruptedException {
        // valid sharded, get request
        worker.readSharded = true;

        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("get random_key random_key2\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleGetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals("END\r\n", response);

    }

    @Test
    void handleGetRequest5() throws IOException, InterruptedException {
        // valid sharded, multiget request
        worker.readSharded = true;

        // first fill the cache with key-value pairs
        ByteBuffer requestBuffer1 = ByteBuffer.allocate(1024);
        requestBuffer1.put("set set_key1 0 9000 10\r\n1111111111\r\n".getBytes());
        Request req1 = new Request(requestBuffer1, key);
        worker.handleSetRequest(req1);

        Thread.sleep(300);

        ByteBuffer requestBuffer2 = ByteBuffer.allocate(1024);
        requestBuffer2.put("set set_key2 0 9000 10\r\n2222222222\r\n".getBytes());
        Request req2 = new Request(requestBuffer2, key);
        worker.handleSetRequest(req2);

        Thread.sleep(300);

        // test multiget
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("get set_key1 set_key2\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleGetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel, atLeast(1)).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals("VALUE set_key1 0 10\r\n" +
                "1111111111\r\n" +
                "VALUE set_key2 0 10\r\n" +
                "2222222222\r\n" +
                "END\r\n", response);

    }

    @Test
    void handleGetRequest6() throws IOException, InterruptedException {
        // valid sharded, multiget request
        worker.readSharded = true;

        // first fill the cache with key-value pairs
        ByteBuffer requestBuffer1 = ByteBuffer.allocate(1024);
        requestBuffer1.put("set set_key1 0 9000 10\r\n1111111111\r\n".getBytes());
        Request req1 = new Request(requestBuffer1, key);
        worker.handleSetRequest(req1);

        Thread.sleep(300);

        ByteBuffer requestBuffer2 = ByteBuffer.allocate(1024);
        requestBuffer2.put("set set_key2 0 9000 10\r\n2222222222\r\n".getBytes());
        Request req2 = new Request(requestBuffer2, key);
        worker.handleSetRequest(req2);

        Thread.sleep(300);

        // test multiget
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("get set_key2 set_key1\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleGetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel, atLeast(1)).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals("VALUE set_key2 0 10\r\n" +
                "2222222222\r\n" +
                "VALUE set_key1 0 10\r\n" +
                "1111111111\r\n" +
                "END\r\n", response);
    }

    @Test
    void handleGetRequest7() throws IOException, InterruptedException {
        // valid sharded, multiget request
        worker.readSharded = true;

        // first fill the cache with key-value pairs
        ByteBuffer requestBuffer1 = ByteBuffer.allocate(1024);
        requestBuffer1.put("set set_key1 0 9000 10\r\n1111111111\r\n".getBytes());
        Request req1 = new Request(requestBuffer1, key);
        worker.handleSetRequest(req1);

        Thread.sleep(300);

        ByteBuffer requestBuffer2 = ByteBuffer.allocate(1024);
        requestBuffer2.put("set set_key2 0 9000 10\r\n2222222222\r\n".getBytes());
        Request req2 = new Request(requestBuffer2, key);
        worker.handleSetRequest(req2);

        Thread.sleep(300);

        // test multiget
        ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
        requestBuffer.put("get set_key2 set_key1 set_key2 set_key1 set_key2\r\n".getBytes());
        Request req = new Request(requestBuffer, key);

        worker.handleGetRequest(req);

        Thread.sleep(300);

        ArgumentCaptor<ByteBuffer> captor = ArgumentCaptor.forClass(ByteBuffer.class);
        verify(channel, atLeast(1)).write(captor.capture());
        ByteBuffer argument = captor.getValue();
        String response = new String(Arrays.copyOfRange(argument.array(), 0, argument.limit()),
                Charset.forName("UTF-8"));

        System.out.println(response);
        assertEquals(
                "VALUE set_key2 0 10\r\n" +
                        "2222222222\r\n" +
                        "VALUE set_key1 0 10\r\n" +
                        "1111111111\r\n" +
                        "VALUE set_key2 0 10\r\n" +
                        "2222222222\r\n" +
                        "VALUE set_key1 0 10\r\n" +
                        "1111111111\r\n" +
                        "VALUE set_key2 0 10\r\n" +
                        "2222222222\r\n" +
                        "END\r\n", response);
    }


}

class MemcachedServer1 extends Thread {
    @Override
    public void run() {
        super.run();
        try {
            Runtime.getRuntime().exec("memcached --threads=1 --port 11211 -vv");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

class MemcachedServer2 extends Thread {
    @Override
    public void run() {
        super.run();
        try {
            Runtime.getRuntime().exec("memcached --threads=1 --port 11212 -vv");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

class MemcachedServer3 extends Thread {
    @Override
    public void run() {
        super.run();
        try {
            Runtime.getRuntime().exec("memcached --threads=1 --port 11213 -vv");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}

