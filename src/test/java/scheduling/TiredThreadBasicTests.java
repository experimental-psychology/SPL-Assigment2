package scheduling;

import org.junit.jupiter.api.Test;

public class TiredThreadBasicTests {

    /* =========================
       Constructor & basic getters
       ========================= */

    @Test
    void testConstructorInitialState() {
        TiredThread worker = new TiredThread(0, 1.0);

        if (worker.getWorkerId() != 0) {
            throw new RuntimeException("Worker id not initialized correctly");
        }

        if (worker.getTimeUsed() != 0) {
            throw new RuntimeException("timeUsed should start at 0");
        }

        if (worker.getTimeIdle() != 0) {
            throw new RuntimeException("timeIdle should start at 0");
        }

        if (worker.getFatigue() != 0.0) {
            throw new RuntimeException("Initial fatigue should be 0");
        }
    }

    @Test
    void testConstructorInvalidArguments() {
        boolean exceptionThrown = false;
        try {
            new TiredThread(-1, 1.0);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for negative id");
        }

        exceptionThrown = false;
        try {
            new TiredThread(0, -0.5);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for negative fatigueFactor");
        }
    }

    /* =========================
       Task execution & handoff
       ========================= */

    @Test
    void testSingleTaskExecutionAndReturnToIdle() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        worker.start();

        Runnable task = () -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        };

        worker.newTask(task);

        Thread.sleep(100);

        if (worker.getTimeUsed() <= 0) {
            throw new RuntimeException("timeUsed was not updated after task execution");
        }

        if (worker.isBusy()) {
            throw new RuntimeException("Worker should not be busy after task completion");
        }

        worker.shutdown();
        worker.join();
    }

    /* =========================
       Idle time measurement
       ========================= */

   @Test
    void testIdleTimeMeasuredBetweenTasks() throws InterruptedException {
        TiredThread worker = new TiredThread(0, 1.0);
        worker.start();
        worker.newTask(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {}
        });
        Thread.sleep(50); 
        worker.newTask(() -> {
            try {
                Thread.sleep(20);
            } catch (InterruptedException ignored) {}
        });
        Thread.sleep(100);
        worker.shutdown();
        worker.join();
        if (worker.getTimeIdle() <= 0)
        throw new RuntimeException("timeIdle was not updated between tasks");
    }


    /* =========================
       Fatigue calculation
       ========================= */

    @Test
    void testFatigueReflectsTimeUsed() throws InterruptedException {
        double factor = 1.2;
        TiredThread worker = new TiredThread(3, factor);
        worker.start();

        Runnable task = () -> {
            try {
                Thread.sleep(60);
            } catch (InterruptedException ignored) {
            }
        };

        worker.newTask(task);
        Thread.sleep(100);

        long used = worker.getTimeUsed();
        double fatigue = worker.getFatigue();

        if (used <= 0) {
            throw new RuntimeException("timeUsed should be positive after task");
        }

        if (fatigue <= 0) {
            throw new RuntimeException("Fatigue should be positive after work");
        }

        if (Math.abs(fatigue - factor * used) > factor * used * 0.2) {
            throw new RuntimeException("Fatigue does not reflect timeUsed correctly");
        }

        worker.shutdown();
        worker.join();
    }

    /* =========================
       newTask edge cases
       ========================= */

    @Test
    void testNewTaskNullThrowsException() {
        TiredThread worker = new TiredThread(4, 1.0);

        boolean exceptionThrown = false;
        try {
            worker.newTask(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for null task");
        }
    }

    @Test
    void testNewTaskAfterShutdownThrowsException() throws InterruptedException {
        TiredThread worker = new TiredThread(5, 1.0);
        worker.start();

        worker.shutdown();
        worker.join();

        boolean exceptionThrown = false;
        try {
            worker.newTask(() -> {});
        } catch (IllegalStateException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception when assigning task to shutdown worker");
        }
    }

    /* =========================
       Poison pill & shutdown
       ========================= */

    @Test
    void testPoisonPillTerminatesThread() throws InterruptedException {
        TiredThread worker = new TiredThread(6, 1.0);
        worker.start();

        worker.shutdown();
        worker.join();

        if (worker.isAlive()) {
            throw new RuntimeException("Worker thread is still alive after shutdown");
        }
    }

    /* =========================
       Comparable logic
       ========================= */

    @Test
    void testCompareToByFatigueThenId() throws InterruptedException {
        TiredThread w1 = new TiredThread(1, 1.0);
        TiredThread w2 = new TiredThread(2, 1.0);

        w1.start();
        w2.start();

        w1.newTask(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {
            }
        });

        Thread.sleep(100);

        if (w1.compareTo(w2) <= 0) {
            throw new RuntimeException("More fatigued worker should compare as greater");
        }

        w1.shutdown();
        w2.shutdown();
        w1.join();
        w2.join();
    }
}
