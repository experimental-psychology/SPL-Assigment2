package memory;

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
        }
        finally{
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

    }

    public void writeUnlock() {
        // TODO: release write lock
    }

    public void readLock() {
        // TODO: acquire read lock
    }

    public void readUnlock() {
        // TODO: release read lock
    }

    public void transpose() {
        // TODO: transpose vector
        writeLock();
        try{
            if(this.orientation== VectorOrientation.ROW_MAJOR)
                 this.orientation=VectorOrientation.COLUMN_MAJOR;
            else
                   this.orientation= VectorOrientation.ROW_MAJOR ;
        }
        finally{
            writeUnlock();
        }
    }

   public void add(SharedVector other) {
    if (other.length() != this.length()) {
        throw new IllegalArgumentException("Dimensions mismatch: " + this.length() + " vs " + other.length());
    }

    if (this == other) {
        writeLock();
        try {
            for (int i = 0; i < vector.length; i++) {
                vector[i] *= 2;
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
    } else {
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

    public void negate() {
        // TODO: negate vector
    }

    public double dot(SharedVector other) {
        // TODO: compute dot product (row · column)
        return 0;
    }

    public void vecMatMul(SharedMatrix matrix) {
        // TODO: compute row-vector × matrix
        int culomnnumber=0;
        if(matrix==null)
            throw new IllegalArgumentException("no such argument");
        if(matrix.getOrientation()==VectorOrientation.ROW_MAJOR){
             culomnnumber=matrix.get(0).length();
            if(matrix.length()!=vector.length)
                  throw new IllegalArgumentException("Multiplication cannot be performed due to length mismatch.");

        }
        else
        {
             culomnnumber=matrix.length();
               if(matrix.get(0).length()!=vector.length)
                  throw new IllegalArgumentException("Multiplication cannot be performed due to length mismatch.");

        }
        double[] newv=new double[culomnnumber];
              readLock();    
        try{
            
             if(matrix.getOrientation()==VectorOrientation.ROW_MAJOR)
            {
                for(int i=0;i<matrix.length();i++)
                {
                    SharedVector vec = matrix.get(i); // שמירה במשתנה עזר ליעילות
                    vec.readLock();
                    try{
                    double result=0;
                    for(int j=0;j<culomnnumber;j++)
                    {
                         result=this.vector[i]*vec.vector[j];
                         newv[j]=newv[j]+result;
                    }
                }
                finally{
                     vec.readUnlock();
                }
                }
            }
            else{  
                for(int i=0;i<matrix.length();i++)
                {
                     SharedVector vec = matrix.get(i); // שמירה במשתנה עזר ליעילות
                    vec.readLock();
                    try{
                    double result=0;
                    for(int j=0;j<matrix.get(0).length();j++)
                    {
                         result=this.vector[j]*vec.vector[j]+result;
                    }
                    newv[i]=result;
                }
                finally{
                     vec.readUnlock();
                }



            }
        }
    }
            finally{
                    readUnlock();
            }
        writeLock();
        try{
            vector=newv;
        }
        finally{
            writeUnlock();
        }
      
    }
}
