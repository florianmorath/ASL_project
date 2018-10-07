package ch.ethz.asl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class NetThreadUnitTest {

    @InjectMocks
    private NetThread net = new NetThread("localhost", 2345, new LinkedBlockingQueue<Request>());

    @Mock
    Selector selector;

    @Mock
    ServerSocketChannel serverChannel;


    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testReadFromChannel() throws IOException {
        verify(selector, times(1)).open();

    }

    @AfterEach
    void tearDown() {
    }
}