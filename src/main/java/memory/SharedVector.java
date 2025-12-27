package memory;

import java.util.ConcurrentModificationException;
import java.util.concurrent.locks.ReadWriteLock;

public class SharedVector 
{

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
        readLock();
        try {
            return vector.length;
        } finally {
            readUnlock();
        }
    }

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
    if(other==null)
        throw new NullPointerException("Other cant be null");
    if (other.length() != this.length())
        throw new IllegalArgumentException("Dimensions mismatch: " + this.length() + " vs " + other.length());
    if (this == other) {
        writeLock();
        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i]=vector[i]*2;
            }
        } finally {
            writeUnlock();
        }

        if (this == other) {
            writeLock();
            try {
                for (int i = 0; i < vector.length; i++) {
                    vector[i]=vector[i]+other.vector[i];
                }
            } finally {
                writeUnlock();
            }
            return;
        }
        int myHash = System.identityHashCode(this);
        int otherHash = System.identityHashCode(other);

        if (myHash < otherHash) {
            
            writeLock();
            try {
                other.readLock();
                try {
                    for (int i = 0; i < vector.length; i++) {
                        vector[i] += other.vector[i];
                    }
                } finally {
                    other.readUnlock();
                }
            } finally {
                writeUnlock();
            }
        } else
             {
            other.readLock();
            try {
                writeLock();
                try {
                    for (int i = 0; i < vector.length; i++) {
                        vector[i] += other.vector[i];
                    }
                } finally {
                    writeUnlock();
                }
            } finally {
                other.readUnlock();
            }
        }
    }
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
        double dot=0.0;
        if(other==null)
            throw new NullPointerException("Other cant be Null");
        if (this.vector.length == 0) //if this=0 so other=0 also, otherwise there is an Exception 
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
            if(first.orientation==second.orientation)
                throw new IllegalArgumentException("You must to choose one row and one calumn");
            if(first.vector.length!=second.vector.length) 
                throw new IllegalArgumentException("Vectors length not equal");
            for(int i=0;i<first.vector.length;i++)
                dot=dot+(first.vector[i]*second.vector[i]);
        }
        finally{
            second.readUnlock();
            first.readUnlock();
        }
        return dot;
    }

    public void vecMatMul(SharedMatrix matrix) 
 {
        // TODO: compute row-vector × matrix
     if (matrix == null) {
        throw new IllegalArgumentException("no such argument");
      }

    int columnNumber = 0;

    if (matrix.getOrientation() == VectorOrientation.ROW_MAJOR) {
        columnNumber = matrix.get(0).length();
        if (matrix.length() != vector.length) {
            throw new IllegalArgumentException("Multiplication cannot be performed due to length mismatch.");
        }
    } else {
        columnNumber = matrix.length();
        if (matrix.get(0).length() != vector.length) {
            throw new IllegalArgumentException("Multiplication cannot be performed due to length mismatch.");
        }
    }

    double[] newv = new double[columnNumber];

    writeLock();
    try {
        if (matrix.getOrientation() == VectorOrientation.ROW_MAJOR) {
            for (int i = 0; i < matrix.length(); i++) {
                SharedVector vec = matrix.get(i);
                vec.readLock();
                try {
                    double myScalar = this.vector[i];
                    for (int j = 0; j < columnNumber; j++) {
                        newv[j] += (myScalar * vec.vector[j]);
                    }
                } finally {
                    vec.readUnlock();
                }
            }
        } else {
            for (int i = 0; i < matrix.length(); i++) {
                SharedVector vec = matrix.get(i);
                vec.readLock();
                try {
                    double result = 0;
                    for (int j = 0; j < this.vector.length; j++) {
                        result += (this.vector[j] * vec.vector[j]);
                    }
                    newv[i] = result;
                } finally {
                    vec.readUnlock();
                }
            }
        }
       this.vector = newv; 
    } finally {
       writeUnlock();
    }

}
}
