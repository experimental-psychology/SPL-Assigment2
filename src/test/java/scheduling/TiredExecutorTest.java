package scheduling;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TiredExecutorTest {

    @Test
    void submitAll_runsAllTasks() {
        TiredExecutor ex = new TiredExecutor(3);
        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            tasks.add(counter::incrementAndGet);
        }

        ex.submitAll(tasks);

        assertEquals(100, counter.get());

        assertDoesNotThrow(() -> ex.shutdown());
    }

    @Test
    void submit_nullTask_throws() {
        TiredExecutor ex = new TiredExecutor(1);
        assertThrows(NullPointerException.class, () -> ex.submit(null));
        assertDoesNotThrow(() -> ex.shutdown());
    }
}
