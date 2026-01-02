package scheduling;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
//@INV:id>=0
//@INV:fatigueFactor>=0
//@INV:alive==true implies thread may accept tasks
//@INV:alive==false implies worker will eventually terminate
//@INV:handoff!=null && handoff.capacity()==1
//@INV:timeUsed>=0
//@INV:timeIdle>=0
//@INV:idleStartTime>=0
public class TiredThread extends Thread implements Comparable<TiredThread> {

    private static final Runnable POISON_PILL = () -> {}; // Special task to signal shutdown

    private final int id; // Worker index assigned by the executor
    private final double fatigueFactor; // Multiplier for fatigue calculation

    private final AtomicBoolean alive = new AtomicBoolean(true); // Indicates if the worker should keep running

    // Single-slot handoff queue; executor will put tasks here
    private final BlockingQueue<Runnable> handoff = new ArrayBlockingQueue<>(1);

    private final AtomicBoolean busy = new AtomicBoolean(false); // Indicates if the worker is currently executing a task

    private final AtomicLong timeUsed = new AtomicLong(0); // Total time spent executing tasks
    private final AtomicLong timeIdle = new AtomicLong(0); // Total time spent idle
    private final AtomicLong idleStartTime = new AtomicLong(0); // Timestamp when the worker became idle
    //@PRE:id>=0 & fatigueFactor>=0
    //@POST:this.id == id
    //@POST:this.fatigueFactor == fatigueFactor
    //@POST:alive == true
    //@POST:busy == false
    //@POST:timeUsed == 0
    //@POST:timeIdle == 0
    //@POST:idleStartTime initialized
    public TiredThread(int id, double fatigueFactor) {
        if(id<0)
            throw new IllegalArgumentException("id must be non-negative");
        if(fatigueFactor<0)
            throw new IllegalArgumentException("fatigueFactor must be non-negative");
        this.id = id;
        this.fatigueFactor = fatigueFactor;
        this.idleStartTime.set(System.nanoTime());
        setName(String.format("FF=%.2f", fatigueFactor));
    }

    public int getWorkerId() {
        return id;
    }

    public double getFatigue() {
        return fatigueFactor * timeUsed.get();
    }

    public boolean isBusy() {
        return busy.get();
    }

    public long getTimeUsed() {
        return timeUsed.get();
    }

    public long getTimeIdle() {
        return timeIdle.get();
    }

    /**
     * Assign a task to this worker.
     * This method is non-blocking: if the worker is not ready to accept a task,
     * it throws IllegalStateException.
     */
    //@PRE:task!=null & alive ==true
    //@POST:task is placed in handoff queue
    public void newTask(Runnable task) {   
        if(task==null)
            throw new NullPointerException("Task cannot be null");
        if(!alive.get()) 
            throw new IllegalStateException("Worker is shut down");
        handoff.add(task); 
    }

    /**
     * Request this worker to stop after finishing current task.
     * Inserts a poison pill so the worker wakes up and exits.
     */
    //@PRE:None
    //@POST:POISON_PILL is eventually consumed by the worker
    //@POST:run() will terminate
    public void shutdown(){
        alive.set(false);
        try {
            handoff.put(POISON_PILL);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

   @Override
    //@PRE:Thread has been started
    //@POST:worker repeatedly executes tasks until POISON_PILL is received
    public void run() {
        try {
            while (true) {
                Runnable task = handoff.take(); 
                long now = System.nanoTime();
                long idleStart = idleStartTime.get();
                timeIdle.addAndGet(now - idleStart);
                if (task == POISON_PILL)
                    break;
                busy.set(true);
                long start = System.nanoTime();
                try {
                    task.run();
                } catch (Throwable t) {
                    System.err.println("Worker " + id + " failed to execute task: " + t.getMessage());
                } finally {
                    long end = System.nanoTime();
                    timeUsed.addAndGet(end - start);
                    busy.set(false);
                    idleStartTime.set(end); 
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }finally {
            alive.set(false);
        }
    }
    @Override
    //@PRE:other!=null
    //@POST:returns<0 if this worker is less fatigued than other
    //@POST:returns>0 if this worker is more fatigued than other
    //@POST:returns 0 iff fatigue and id are equal
    public int compareTo(TiredThread other){
        if(other==null)
            throw new NullPointerException("Other is null");
        int result = Double.compare(this.getFatigue(), other.getFatigue());
        if (result == 0)
            return Integer.compare(this.id, other.getWorkerId());
        return result;
    }
}