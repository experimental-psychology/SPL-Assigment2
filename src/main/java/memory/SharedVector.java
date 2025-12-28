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
        if(vector==null)
            throw new NullPointerException("Vector cant be null");
        if (orientation==null)
            throw new NullPointerException("Orientation cant be null");
        this.vector=vector.clone();
        this.orientation=orientation;
    }

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
        if(this.orientation!=other.orientation)
            throw new IllegalArgumentException("Orientation mismatch");
        if (other.length() != this.length())
            throw new IllegalArgumentException("Dimensions mismatch");
        if (this == other) {
            writeLock();
            try {
                for (int i = 0; i < vector.length; i++)
                    vector[i]=vector[i]*2;
            } finally {
                writeUnlock();
            }
            return;
        }
        SharedVector first=this;
        SharedVector second=other;
        boolean thisIsFirst=true;
        if(System.identityHashCode(this)>System.identityHashCode(other)){
            first=other;
            second=this;
            thisIsFirst=false;
        }
        if(thisIsFirst){
            first.writeLock();
            second.readLock();
            try{
                for(int i=0; i<vector.length;i++)
                    this.vector[i]=this.vector[i]+other.vector[i];
            } finally{
                second.readUnlock();
                first.writeUnlock();
            }
        }
        else{
            first.readLock();
            second.writeLock();
            try{
                for(int i=0; i<vector.length;i++)
                    this.vector[i]=this.vector[i]+other.vector[i];
            } finally{
                second.writeUnlock();
                first.readUnlock();
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
        if(other==null)
            throw new NullPointerException("Other cant be Null");
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
                throw new IllegalArgumentException("You must to choose one row and one column");
            if(first.vector.length!=second.vector.length) 
                throw new IllegalArgumentException("Vectors length not equal");
            double dot=0.0;
            for(int i=0;i<first.vector.length;i++)
                dot=dot+(first.vector[i]*second.vector[i]);
            return dot;
        }
        finally{
            second.readUnlock();
            first.readUnlock();
        }
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        if(matrix == null) 
            throw new IllegalArgumentException("no such argument");
        writeLock();
        try{
            if(this.orientation!=VectorOrientation.ROW_MAJOR)
                throw new IllegalArgumentException("Vector must be row-major");
            int rows = matrix.length();
            if(rows == 0){
                this.vector=new double[0];
                return;
            }
            VectorOrientation matOrientation = matrix.getOrientation();
            int thisLen = this.vector.length; 
            int resultLength;
            if(matOrientation==VectorOrientation.ROW_MAJOR){
                if(rows!=thisLen)
                    throw new IllegalArgumentException("Length mismatch");
                resultLength = matrix.get(0).length();
            }
            else{
                if(matrix.get(0).length()!=thisLen)
                    throw new IllegalArgumentException("Length mismatch");
                resultLength = rows;
            }
            double[] result=new double[resultLength];
            if(matOrientation==VectorOrientation.ROW_MAJOR){
                for (int i=0; i<rows; i++){
                    SharedVector row = matrix.get(i);
                    row.readLock();
                    try{
                        double scalar = this.vector[i];
                        for (int j=0; j<resultLength; j++)
                            result[j]=result[j]+(scalar*row.vector[j]); 
                    }finally{
                        row.readUnlock();
                    }
                }
            } else{
                for(int i=0;i<rows; i++){
                    SharedVector col=matrix.get(i);
                    col.readLock();
                    try{
                        double sum=0.0;
                        for(int j=0; j<thisLen; j++)
                            sum=sum+(this.vector[j]*col.vector[j]);
                        result[i]=sum;
                    }finally{
                        col.readUnlock();
                    }
                }
            }
            this.vector = result; 
            } finally {
                writeUnlock();
        }
    }
}
