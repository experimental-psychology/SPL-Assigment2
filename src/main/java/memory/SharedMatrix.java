package memory;
//@INV: Vectors != null
//@INV: Vectors is an array of SharedVector
//@INV: All vectors have the same orientation
//@INV: Matrix is rectangular
//@INV: Access to vector contents is protected by locks
public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors
    //@PRE: None
    //@POST: length==0
    public SharedMatrix() {
        // TODO: initialize empty matrix 
        this.vectors=new SharedVector[0];
    }
    //@PRE:matrix!=null
    //@POST: All vectors are ROW_MAJOR
    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        if(matrix==null)
            throw new IllegalArgumentException("Matrix cant be null");
        if(matrix.length>0){
            if(matrix[0]==null)
                throw new IllegalArgumentException("Matrix row cant be null");
            int cols=matrix[0].length;
            for (int i=1; i<matrix.length; i++)
                if (matrix[i] == null || matrix[i].length != cols)
                    throw new IllegalArgumentException("Matrix must be rectangular");
        }
        this.vectors=new SharedVector[matrix.length];
        for (int i=0;i<matrix.length;i++)
            this.vectors[i]=new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
    }
    //@PRE: matrix!=null
    //@POST:getOrientation()==ROW_MAJOR
    public void loadRowMajor(double[][] matrix){
        // TODO: replace internal data with new row-major matrix
        if(matrix == null)
            throw new IllegalArgumentException("Matrix cant be null");
        if(matrix.length == 0){
            this.vectors = new SharedVector[0];
            return;
        }
        if(matrix[0]==null)
            throw new IllegalArgumentException("Cant be null");
        int cols=matrix[0].length;
        SharedVector[] tempVecs = new SharedVector[matrix.length];
        for(int i=0; i<matrix.length; i++){
        if (matrix[i] == null) 
            throw new IllegalArgumentException("Matrix row cant be null");
        if (matrix[i].length != cols) 
            throw new IllegalArgumentException("Matrix must be rectangular");
        tempVecs[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
        }
        this.vectors = tempVecs;   
    }
    //@PRE: matrix!=null
    //@POST: getOrientation()==COLUMN_MAJOR
    public void loadColumnMajor(double[][] matrix){
        // TODO: replace internal data with new column-major matrix
        if(matrix == null) 
            throw new IllegalArgumentException("Matrix cant be null");
        if(matrix.length==0){
        this.vectors = new SharedVector[0];
        return;
        }
        int rows=matrix.length;
        if(matrix[0]==null)
            throw new IllegalArgumentException("Cant be null");
        int cols=matrix[0].length;
        for(int i=1; i<rows; i++)
            if(matrix[i] == null || matrix[i].length != cols)
                throw new IllegalArgumentException("Matrix must be rectangular");
        SharedVector[] newVectors = new SharedVector[cols];
        for (int j = 0; j < cols; j++){
            double[] column = new double[rows];
            for (int i = 0; i < rows; i++)
                column[i] = matrix[i][j];
        newVectors[j] = new SharedVector(column, VectorOrientation.COLUMN_MAJOR);
        }
        this.vectors = newVectors;
    }
    //@PRE: vectors!=null
    //@POST: New matrix
    public double[][] readRowMajor(){
        // TODO: return matrix contents as a row-major double[][]
        if(vectors==null)
            throw new NullPointerException("Vectors is null");
        SharedVector[] local=this.vectors;
        if(local.length==0)
           return new double[0][0];
        acquireAllVectorReadLocks(local);
        try{
            VectorOrientation orientation = local[0].getOrientation(); 
            int rows, cols;
            double[][] matrix;
            if(orientation==VectorOrientation.ROW_MAJOR){
                rows=local.length;
                cols=local[0].length();
                matrix=new double[rows][cols];
                for(int i=0; i<rows; i++)
                    for (int j = 0; j < cols; j++)
                        matrix[i][j] = local[i].get(j); 
            }else{
                cols=local.length;
                rows=local[0].length();
                matrix=new double[rows][cols]; 
                for (int j = 0; j < cols; j++)
                    for (int i = 0; i < rows; i++)
                        matrix[i][j] = local[j].get(i); 
            }
            return matrix;
        }finally{
            releaseAllVectorReadLocks(local);
        }
    }
    //@PRE: 0<=index<length() & vectors!=null
    //@POST: return the value on the right index
    public SharedVector get(int index){
        // TODO: return vector at index
        if(vectors==null)
            throw new NullPointerException("Vectors is null");
        SharedVector[] local=this.vectors;
        if(index<0||index>=local.length)
            throw new IndexOutOfBoundsException("Index out of bounds");
        return local[index];
    }
    //@PRE: vectors!=null
    //@POST: return the length >= 0
    public int length() {
        // TODO: return number of stored vectors
        if(vectors==null)
            throw new NullPointerException("Vectors is null");
        return vectors.length;
    }
    //@PRE: vectors!=null
    //@POST: return the Orientation
    public VectorOrientation getOrientation() {
        // TODO: return orientation
        if(vectors==null)
            throw new NullPointerException("Vectors is null");
        SharedVector[] local=this.vectors;
        if (local.length == 0)
            return VectorOrientation.ROW_MAJOR; //defult
        return local[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        if(vecs==null)
            throw new NullPointerException("vecs is null");
        for (SharedVector vec:vecs)
            vec.readLock();

    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        if(vecs==null)
            throw new NullPointerException("vecs is null");
        for(int i=vecs.length-1;i>=0;i--)
            vecs[i].readUnlock();
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        if(vecs==null)
            throw new NullPointerException("vecs is null");
        for (SharedVector vec:vecs)
            vec.writeLock();
        
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        if(vecs==null)
            throw new NullPointerException("vecs is null");
        for(int i=vecs.length-1;i>=0;i--)
            vecs[i].writeUnlock();
    }
}
