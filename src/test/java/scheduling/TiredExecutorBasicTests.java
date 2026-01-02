package scheduling;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutorBasicTests {

    @Test
    void testSingleTaskExecution() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);
        AtomicInteger counter = new AtomicInteger(0);

        Runnable task = () -> counter.incrementAndGet();

        executor.submit(task);
        executor.submitAll(new ArrayList<>());

        if (counter.get() != 1) {
            throw new RuntimeException("Single task was not executed");
        }

        executor.shutdown();
    }

    @Test
    void testSubmitAllExecutesAllTasks() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(3);
        AtomicInteger counter = new AtomicInteger(0);

        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            tasks.add(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
                counter.incrementAndGet();
            });
        }

        executor.submitAll(tasks);

        if (counter.get() != 5) {
            throw new RuntimeException("Not all tasks were executed in submitAll");
        }

        executor.shutdown();
    }

    /* =========================
       Fairness & fatigue
       ========================= */

    @Test
    void testLessFatiguedWorkerGetsTask() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);

        // Heavy task to fatigue one worker
        Runnable heavyTask = () -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
        };

        executor.submit(heavyTask);
        executor.submitAll(new ArrayList<>());

        // Light task
        executor.submit(() -> {});

        executor.submitAll(new ArrayList<>());

        String report = executor.getWorkerReport();

        if (!report.contains("Fatigue")) {
            throw new RuntimeException("Worker report does not contain fatigue info");
        }

        executor.shutdown();
    }

    @Test
    void testPriorityQueueUpdatesAfterTaskCompletion() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);

        Runnable task = () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        };

        executor.submit(task);
        executor.submitAll(new ArrayList<>());

        String reportAfter = executor.getWorkerReport();

        if (reportAfter == null || reportAfter.isEmpty()) {
            throw new RuntimeException("Worker report not updated after task completion");
        }

        executor.shutdown();
    }

    /* =========================
       Edge cases & safety
       ========================= */

    @Test
    void testSubmitNullTaskThrowsException() {
        TiredExecutor executor = new TiredExecutor(1);

        boolean exceptionThrown = false;
        try {
            executor.submit(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected NullPointerException for null task");
        }
    }

    @Test
    void testInvalidNumberOfThreads() {
        boolean exceptionThrown = false;
        try {
            new TiredExecutor(0);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for invalid number of threads");
        }
    }

    @Test
    void testBlockingWhenAllWorkersBusy() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(1);
        AtomicInteger counter = new AtomicInteger(0);

        Runnable longTask = () -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            counter.incrementAndGet();
        };

        Runnable shortTask = () -> counter.incrementAndGet();

        executor.submit(longTask);
        executor.submit(shortTask);

        executor.submitAll(new ArrayList<>());

        if (counter.get() != 2) {
            throw new RuntimeException("Tasks were not properly blocked and executed");
        }

        executor.shutdown();
    }

    /* =========================
       Shutdown & lifecycle
       ========================= */

    @Test
    void testShutdownTerminatesWorkers() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(2);

        executor.submit(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        });

        executor.submitAll(new ArrayList<>());

        executor.shutdown();

        String report = executor.getWorkerReport();
        if (report == null) {
            throw new RuntimeException("Worker report unavailable after shutdown");
        }
    }

    @Test
    void testNoDeadlockAfterShutdown() throws InterruptedException {
        TiredExecutor executor = new TiredExecutor(1);

        executor.submit(() -> {});
        executor.submitAll(new ArrayList<>());

        executor.shutdown();

        boolean exceptionThrown = false;
        try {
            executor.submit(() -> {});
        } catch (RuntimeException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected failure when submitting after shutdown");
        }
    }
    @Test
    void testMoreFatiguedWorkerIsDeprioritized() {
        TiredExecutor executor = new TiredExecutor(2);
        Runnable longTask = () -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        };
        Runnable shortTask = () -> {};
        executor.submit(longTask);
        executor.submit(shortTask);
        executor.submitAll(List.of(shortTask));
        String report = executor.getWorkerReport();
        if (!report.contains("Fatigue")) {
            throw new RuntimeException("Worker report does not include fatigue data");
        }
    }
}
