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
        // TODO
    }

    public void submit(Runnable task) {
        // TODO
    }

    public void submitAll(Iterable<Runnable> tasks) {
        // TODO: submit tasks one by one and wait until all finish
        for(Runnable task: tasks)
            submit(task);
            while (inFlight.get()>0){
                try {
                    wait();
                }catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

    public void shutdown() throws InterruptedException{
        // TODO
        for(TiredThread worker:workers)
            worker.shutdown();
         for(TiredThread worker:workers)
            worker.join();
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        String report="";
        int i=1;
        for(TiredThread worker:workers){
            if(worker!=null){
                report=report+"Worker number: "+i+": ";
                report=report+"Name: "+worker.getName()+", ";
                report=report+"Id: "+worker.getWorkerId()+", ";
                report=report+"Fatigue: "+worker.getFatigue()+", ";
                report=report+"Time Used: "+worker.getTimeUsed()+", ";
                report=report+"Time Idle: "+worker.getTimeIdle();
                report=report+"\n";
            }
            i++;
        }
        return report;
    }
}
