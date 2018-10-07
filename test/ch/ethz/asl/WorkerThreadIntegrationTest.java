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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


class WorkerThreadIntegrationTest {

    @Mock
    SelectionKey key;

    @Mock
    SocketChannel channel;


    MemcachedServer1 server1 = new MemcachedServer1();
    MemcachedServer2 server2 = new MemcachedServer2();
    WorkerThread worker;



    @BeforeEach
    void setUp() throws InterruptedException {

        MockitoAnnotations.initMocks(this);

        when(key.channel()).thenReturn(channel);


        server1.start();
        server2.start();

        Thread.sleep(100);

        LinkedBlockingQueue<Request> queue = new LinkedBlockingQueue<>();
        ArrayList<String> mcAddresses = new ArrayList<>();
        mcAddresses.add("127.0.0.1:11211");
        mcAddresses.add("127.0.0.1:11212");
        worker = new WorkerThread(mcAddresses, true, queue);

        Thread.sleep(100);
    }

    @AfterEach
    void tearDown() throws IOException {
        for (SocketChannel socket: worker.socketChannels) {
            socket.close();
        }

    }

    @Test
    void handleSetRequest() throws IOException, InterruptedException {

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
        assertEquals(response, "STORED\r\n");


        
    }

    @Test
    void handleGetRequest() {
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

