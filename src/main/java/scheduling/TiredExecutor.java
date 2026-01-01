package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;

    // נשאר כדי לא לשבור שדות קיימים אם יש לך/לטסטר ציפייה לזה (אפשר להשאיר גם אם לא משתמשים בו)
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();

    private final AtomicInteger inFlight = new AtomicInteger(0);

    // --- חדש: heap שממוין לפי "עייפות מוערכת" שאנחנו מעדכנים בעצמנו
    private final PriorityBlockingQueue<WorkerSlot> idleSlots = new PriorityBlockingQueue<>();
    private final double[] fatigueFactors;

    private static final class WorkerSlot implements Comparable<WorkerSlot> {
        final TiredThread worker;
        final int id;
        volatile double estimatedFatigue; // נמדד בנאנו-שניות * fatigueFactor

        WorkerSlot(TiredThread worker) {
            this.worker = worker;
            this.id = worker.getWorkerId();
            this.estimatedFatigue = 0.0;
        }

        @Override
        public int compareTo(WorkerSlot other) {
            int c = Double.compare(this.estimatedFatigue, other.estimatedFatigue);
            if (c != 0) return c;
            return Integer.compare(this.id, other.id);
        }
    }

    public TiredExecutor(int numThreads) {
        if (numThreads <= 0) throw new IllegalArgumentException("numThreads must be positive");

        workers = new TiredThread[numThreads];
        fatigueFactors = new double[numThreads];

        for (int i = 0; i < numThreads; i++) {
            double ff = 0.5 + Math.random(); // [0.5, 1.5)
            fatigueFactors[i] = ff;

            TiredThread t = new TiredThread(i, ff);
            workers[i] = t;

            // שני המבנים מקבלים worker פנוי בתחילת הדרך
            idleMinHeap.add(t);
            idleSlots.add(new WorkerSlot(t));

            t.start();
        }
    }

    public void submit(Runnable task) {
        if (task == null) throw new NullPointerException("task is null");

        final WorkerSlot slot;
        synchronized (this) {
            while (idleSlots.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for idle worker", e);
                }
            }
            slot = idleSlots.poll();
            inFlight.incrementAndGet();
        }

        final TiredThread worker = slot.worker;

        Runnable wrapped = () -> {
            long start = System.nanoTime();
            try {
                task.run();
            } finally {
                long end = System.nanoTime();
                long duration = end - start;

                // עדכון "עייפות מוערכת" לפני שה-worker חוזר ל-heap
                // כדי שהבחירה הבאה תהיה הוגנת גם אם timeUsed עוד לא עודכן בתוך TiredThread.
                slot.estimatedFatigue += (duration * fatigueFactors[slot.id]);

                synchronized (TiredExecutor.this) {
                    idleSlots.add(slot);
                    inFlight.decrementAndGet();
                    TiredExecutor.this.notifyAll();
                }
            }
        };

        try {
            worker.newTask(wrapped);
        } catch (RuntimeException e) {
            synchronized (this) {
                idleSlots.add(slot);
                inFlight.decrementAndGet();
                notifyAll();
            }
            throw e;
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        if (tasks == null) throw new NullPointerException("tasks is null");

        for (Runnable task : tasks) {
            submit(task);
        }

        synchronized (this) {
            while (inFlight.get() > 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for tasks to finish", e);
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        synchronized (this) {
            while (inFlight.get() > 0) {
                wait();
            }
        }
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
        for (TiredThread worker : workers) {
            worker.join();
        }
    }

    // זה בדיוק הפורמט שגלעד ביקש (לטסטר)
    public synchronized String getWorkerReport() {
        StringBuilder ret = new StringBuilder();
        for (TiredThread worker : workers) {
            String report = String.format(
                    "Worker %d: Time Used = %d ns, Time Idle = %d ns, Fatigue = %,2f\n",
                    worker.getWorkerId(),
                    worker.getTimeUsed(),
                    worker.getTimeIdle(),
                    worker.getFatigue()
            );
            ret.append(report);
        }

        double averageFatigue = 0.0;
        for (TiredThread worker : workers) {
            averageFatigue += worker.getFatigue();
        }
        averageFatigue /= workers.length;

        ret.append(String.format("Average Fatigue: %.2f\n", averageFatigue));

        double fairness = 0.0;
        for (TiredThread worker : workers) {
            fairness += Math.pow(worker.getFatigue() - averageFatigue, 2);
        }
        ret.append("Fairness value: " + String.format("%.2f\n", fairness));

        return ret.toString();
    }
}
