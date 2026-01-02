package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads){
        if(numThreads<=0)
            throw new IllegalArgumentException("numThreads cant be under 1");
           workers=new TiredThread[numThreads];
           for(int i=0;i<numThreads;i++){
                TiredThread newThread=new TiredThread(i, 0.5 + Math.random());
                workers[i]=newThread;
                idleMinHeap.add(newThread);
                newThread.start();
           }
    }
    public void submit(Runnable task){
        if(task==null) 
            throw new NullPointerException("task is null");
        final TiredThread worker;
        synchronized(this){
            while(idleMinHeap.isEmpty()){
                try{
                    wait();
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for idle worker", e);
                }
            }
            TiredThread best=null;
            for(TiredThread w:idleMinHeap)
                if(best==null||w.compareTo(best)<0)
                    best=w;
            idleMinHeap.remove(best);
            worker=best;
            inFlight.incrementAndGet();
        }
        Runnable wrapped=()->{
            try{
                task.run();
            }finally{
                synchronized(TiredExecutor.this) {
                    idleMinHeap.add(worker);
                    inFlight.decrementAndGet();
                    TiredExecutor.this.notifyAll();
                }
            }
        };
        try{
            worker.newTask(wrapped);
        }catch (RuntimeException e){
            synchronized (this){
                idleMinHeap.add(worker);
                inFlight.decrementAndGet();
                notifyAll();
            }
            throw e;
        }
    }
    public void submitAll(Iterable<Runnable> tasks) {
        // TODO: submit tasks one by one and wait until all finish
        for(Runnable task: tasks)
            submit(task);
        synchronized(this){
            while (inFlight.get()>0){
                try {
                    wait();
                }catch (InterruptedException e){
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for task",e);
                }
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