package memory;

import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.ReadWriteLock;

//@INV:vector!=null & orienation!=null & vector.length>=0
//@INV:all accesses to vector and orientation are protected by the appropriate lock
public class SharedVector 
{

    private double[] vector;
    private VectorOrientation orientation;
    private ReadWriteLock lock = new java.util.concurrent.locks.ReentrantReadWriteLock();

    //@PRE:vector!=null & oriention!=null
    //@POST:this.vector is a deep copy of vector by clone & this.orientation=orientation.
    public SharedVector(double[] vector, VectorOrientation orientation) {
        // TODO: store vector data and its orientation
        if(vector==null)
            throw new NullPointerException("Vector cant be null");
        if (orientation==null)
            throw new NullPointerException("Orientation cant be null");
        this.vector=vector.clone();
        this.orientation=orientation;
    }

    //@PRE: 0 <= index < vector.length
    //@POST: returns the value stored at position index
    public double get(int index) {
        // TODO: return element at index (read-locked)
        readLock();
        try{
            if(index<0 || index>=vector.length)
                throw new IndexOutOfBoundsException("Index out of bounds");
            return vector[index];
        }finally{
            readUnlock();
        }
       
    }
    //@PRE: None
    //@POST: returns vector.length
    public int length() {
        // TODO: return vector length
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }
    //@PRE: None
    //@POST: returns the current orientation of the vector
    public VectorOrientation getOrientation() {
        // TODO: return vector orientation
        readLock();
        try {
            return this.orientation;
        } finally {
            readUnlock();
    }
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
    //@PRE: None
    //@POST: orientation is toggled-
    //if old orientation was ROW_MAJOR, new is COLUMN_MAJOR
    //if old orientation was COLUMN_MAJOR, new is ROW_MAJOR
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
    //@PRE: other != null & this.orientation == other.orientation & this.length() == other.length()
    //@POST: for every i, vector[i] == (this.vector[i]) + (other.vector[i])
   public void add(SharedVector other) {
        if(other==null)
            throw new NullPointerException("Other cant be null");
        if (this==other){
            writeLock();
            try {
                for(int i=0; i<vector.length; i++)
                    vector[i]=vector[i]*2;
            } finally {
                writeUnlock();
            }
            return;
        }
        double[] otherSnapshot;
        other.readLock();
        try {
            if(this.orientation!=other.orientation)
                throw new IllegalArgumentException("Orientation mismatch");
            otherSnapshot=other.vector.clone();
        }finally{
            other.readUnlock();
        }
        writeLock();
        try{
            if(this.vector.length!=otherSnapshot.length)
                throw new IllegalArgumentException("Dimensions mismatch");
            for(int i=0; i<vector.length;i++)
                this.vector[i]=this.vector[i]+otherSnapshot[i];
        }finally{
            writeUnlock();
        }
    }
        //@PRE: None
        //@POST: for every i, vector[i] == -vector[i]
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
    //@PRE: other != null & this.orientation != other.orientation
    //@POST: returns \sigma of (this[i] * other[i])
    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        if(other==null)
            throw new NullPointerException("Other cant be Null");
        double[] a;
        double[] b;
        readLock();
        try{
            a=this.vector.clone();
        }finally{
            readUnlock();
        }
        other.readLock();
        try{
            b=other.vector.clone();
        }finally{
            other.readUnlock();
        }
        if(this.orientation==other.orientation)
            throw new IllegalArgumentException("Need one row and one column");
        if(a.length!=b.length)
            throw new IllegalArgumentException("Vectors length mismatch");
        double sum=0.0;
        for(int i=0; i<a.length; i++)
            sum=sum+(a[i]*b[i]);
        return sum;
    }
    // @PRE: matrix != null & this.orientation == ROW_MAJOR & 
    // dimensions are compatible: if matrix is ROW_MAJOR: matrix.length() == this.length()
    // if matrix is COLUMN_MAJOR: matrix.get(0).length() == this.length()
    // @POST: this.vector is replaced with the result of row-vector × matrix & this.orientation == ROW_MAJOR
    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        if(matrix == null) 
            throw new IllegalArgumentException("no such argument");
        double[] snapshot;
        readLock();
        try{
            if(this.orientation!=VectorOrientation.ROW_MAJOR)
                throw new IllegalArgumentException("Vector must be row-major");
            snapshot=this.vector.clone();
        }finally{
            readUnlock();
        }
        int cols=matrix.length();
        double[] result = new double[cols];
        for(int j=0; j<cols; j++){
            SharedVector col = matrix.get(j);
            col.readLock();
            try {
                if(col.getOrientation()!=VectorOrientation.COLUMN_MAJOR)
                    throw new IllegalArgumentException("Matrix must be COLUMN_MAJOR");
                if(col.length()!=snapshot.length)
                    throw new IllegalArgumentException("Dimension mismatch");
                double sum=0.0;
                for(int i=0; i <snapshot.length; i++)
                    sum=sum+(snapshot[i]*col.get(i));
                result[j]=sum;
            }finally{
                col.readUnlock();
            }
        }
        writeLock();
        try{
            this.vector=result;
            this.orientation=VectorOrientation.ROW_MAJOR;
        }finally{
            writeUnlock();
        }
    }
}
