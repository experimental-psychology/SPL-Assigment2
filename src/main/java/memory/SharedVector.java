package memory;

import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector {

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        this.vector=vector.clone();
        this.orientation=orientation;
    }

    public double get(int index) {
        // TODO: return element at index (read-locked)
        readLock();
        try{
            return vector[index];
        }finally{
            readUnlock();
        }
    }

    public int length() {
        // TODO: return vector length
        return vector.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        return this.orientation;
    }

    public void writeLock() {
        // TODO: acquire write lock
        lock.writeLock().lock();
    }

    public void writeUnlock() {
        // TODO: release write lock
        lock.writeLock().unlock();
    }

    public void readLock() {
        // TODO: acquire read lock
        lock.readLock().lock();
    }

    public void readUnlock() {
        // TODO: release read lock
        lock.readLock().unlock();
    }

    public void transpose() {
        // TODO: transpose vector
       writeLock();
       try{
        if(this.orientation==VectorOrientation.ROW_MAJOR)
           this.orientation=VectorOrientation.COLUMN_MAJOR;
        else
            this.orientation=VectorOrientation.ROW_MAJOR;
       }
       finally{
        writeUnlock();
       }
    }

    public void add(SharedVector other) {
        // TODO: add two vectors

    }

    public void negate() {
        // TODO: negate vector
        this.writeLock();
        try{
            for(int i=0;i<vector.length;i++)
                this.vector[i]=-this.vector[i];
        }
        finally{
            this.writeUnlock();
        }
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        double dot=0;
        if(other==null)
            throw new NullPointerException("Other cant be Null");
        if(this.orientation==other.orientation)
            throw new IllegalArgumentException("You must to choose one row and one calumn");
        if(other.length()!=this.length() ) 
            throw new IllegalArgumentException("Vectors length not equal");
        if (this.length() == 0) //if this=0 so other=0 also, otherwise there is an Exception 
            return 0;
        SharedVector first=this;
        SharedVector second=other;
        if(System.identityHashCode(this)>System.identityHashCode(other)){
           first=other;
           second=this;
        }
        first.readLock();
        second.readLock();
        try{
            for(int i=0;i<vector.length;i++)
                dot=dot+(vector[i]*other.vector[i]);
        }
        finally{
            second.readUnlock();
            first.readUnlock();
        }
        return dot;
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
    }
}
