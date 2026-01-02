package scheduling;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TiredSchedulingTest {

    @Test
    void tiredThread_newTaskAfterShutdown_throws() throws Exception {
        TiredThread t = new TiredThread(0, 1.0);
        t.start();
        t.shutdown();
        t.join();

        assertThrows(IllegalStateException.class, () -> t.newTask(() -> {}));
    }

    @Test
    void tiredThread_compareTo_null_throws() {
        TiredThread t = new TiredThread(0, 1.0);
        assertThrows(NullPointerException.class, () -> t.compareTo(null));
    }

    @Test
    void executor_submit_runsTask() throws Exception {
        TiredExecutor ex = new TiredExecutor(2);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            ex.submit(latch::countDown);

            // נחכה שהמשימה רצה (אין await בתוך האקסקיוטר שלך)
            assertTrue(latch.await(2, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void executor_submitAll_runsAllTasksAndWaitsForCompletion() throws Exception {
        // הטסט הזה משקף את ההתנהגות הנדרשת: submitAll צריך גם להמתין עד שכל המשימות הסתיימו.
        // כרגע אצלך יש סיכוי שיזרק IllegalMonitorStateException בגלל wait() בלי synchronized.
        TiredExecutor ex = new TiredExecutor(3);
        try {
            AtomicInteger counter = new AtomicInteger(0);
            List<Runnable> tasks = List.of(
                    counter::incrementAndGet,
                    counter::incrementAndGet,
                    counter::incrementAndGet,
                    counter::incrementAndGet
            );

            assertDoesNotThrow(() -> ex.submitAll(tasks));
            assertEquals(4, counter.get());
        } finally {
            ex.shutdown();
        }
    }

    @Test
    void executor_getWorkerReport_containsWorkerLines() throws Exception {
        TiredExecutor ex = new TiredExecutor(2);
        try {
            String report = ex.getWorkerReport();
            assertTrue(report.contains("Worker number: 1"));
            assertTrue(report.contains("Worker number: 2"));
            assertTrue(report.contains("Fatigue"));
            assertTrue(report.contains("Time Used"));
            assertTrue(report.contains("Time Idle"));
        } finally {
            ex.shutdown();
        }
    }
}
