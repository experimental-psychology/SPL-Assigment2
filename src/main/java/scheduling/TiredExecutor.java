package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);
    //@PRE:
    //@POST:workers.length==numThreads & inFlight==0
    //@POST:all workers are started and idleMinHeap contains all workers
    public TiredExecutor(int numThreads) {
        // TODO
    }
    //@PRE:
    //@POST:task is eventually executed
    //@POST:inFlight increased by 1 before execution
    //@POST:inFlight decreased by 1 after execution
    public void submit(Runnable task) {
        // TODO
    }
    //@PRE:tasks!=null
    //@POST:all tasks have completed execution
    public void submitAll(Iterable<Runnable> tasks) {
        // TODO: submit tasks one by one and wait until all finish
        if(tasks==null)
            throw new NullPointerException("Tasks cant be null");
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
    //@PRE:None
    //@POST:all workers are shut down and threads are terminated
    public void shutdown() throws InterruptedException{
        // TODO
        if(workers==null)
            throw new NullPointerException("Workers is null");
        for(TiredThread worker:workers)
            worker.shutdown();
         for(TiredThread worker:workers)
            worker.join();
    }
    //@PRE:None
    //@POST:returns a readable report of all workers
    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        if(workers==null)
            throw new NullPointerException("Workers is null");
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
