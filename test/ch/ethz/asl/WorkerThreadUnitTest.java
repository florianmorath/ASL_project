package ch.ethz.asl;


import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class WorkerThreadUnitTest {



    @Test
    void testGetKeyCount(){

        // 3 servers and 7 keys
        assertEquals(WorkerThread.getKeyCount(0, 7, 3), 3);
        assertEquals(WorkerThread.getKeyCount(1, 7, 3), 2);
        assertEquals(WorkerThread.getKeyCount(2, 7, 3), 2);

        // 3 servers and 6 keys
        assertEquals(WorkerThread.getKeyCount(0, 6, 3), 2);
        assertEquals(WorkerThread.getKeyCount(1, 6, 3), 2);
        assertEquals(WorkerThread.getKeyCount(2, 6, 3), 2);

        // 3 servers and 1 key
        assertEquals(WorkerThread.getKeyCount(0, 1, 3), 1);
        assertEquals(WorkerThread.getKeyCount(1, 1, 3), 0);
        assertEquals(WorkerThread.getKeyCount(2, 1, 3), 0);

        // 3 servers and 2 keys
        assertEquals(WorkerThread.getKeyCount(0, 2, 3), 1);
        assertEquals(WorkerThread.getKeyCount(1, 2, 3), 1);
        assertEquals(WorkerThread.getKeyCount(2, 2, 3), 0);

    }

}