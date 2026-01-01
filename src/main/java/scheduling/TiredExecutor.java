package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
           workers=new TiredThread[numThreads];
           for(int i=0;i<numThreads;i++)
           {
                TiredThread newThread=new TiredThread(i, 0.5 + Math.random());
                workers[i]=newThread;
                idleMinHeap.add(newThread);
                newThread.start();
           }
    }

      public void submit(Runnable task) 
      {
        if (task == null) 
            throw new NullPointerException("task is null");

        final TiredThread worker;
        synchronized (this) {
            while (idleMinHeap.isEmpty()) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for idle worker", e);
                }
            }
            worker = idleMinHeap.poll();
            inFlight.incrementAndGet();
        }
        Runnable wrapped = () -> {
            try {
                task.run();
         } finally {
            synchronized (TiredExecutor.this) {
            idleMinHeap.add(worker);   
            inFlight.decrementAndGet();
            TiredExecutor.this.notifyAll();
        }
    }
        };
             try {
             worker.newTask(wrapped);
             }catch (RuntimeException e) {
                synchronized (this) { 
                idleMinHeap.add(worker);
                inFlight.decrementAndGet();
                notifyAll();
                notifyAll();
            }
            throw e;
        }
    }
 public void submitAll(Iterable<Runnable> tasks) {
        if (tasks == null)
            throw new NullPointerException("tasks is null");

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

public synchronized String getWorkerReport() {
    // return readable statistics for each worker
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
