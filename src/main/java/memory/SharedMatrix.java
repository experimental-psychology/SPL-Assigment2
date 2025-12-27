package memory;

public class SharedMatrix {

    private volatile SharedVector[] vectors = {}; // underlying vectors

    public SharedMatrix() {
        // TODO: initialize empty matrix 
        this.vectors=new SharedVector[0];
    }
    public SharedMatrix(double[][] matrix) {
        // TODO: construct matrix as row-major SharedVectors
        if(matrix==null)
            throw new IllegalArgumentException("Matrix cant be null");
        this.vectors=new SharedVector[matrix.length];
        for (int i=0;i<matrix.length;i++)
            this.vectors[i]=new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
    }

    public void loadRowMajor(double[][] matrix) 
    {
        // TODO: replace internal data with new row-major matrix
          if (matrix == null) {
        throw new IllegalArgumentException("Matrix cant be null");
    }

    if (matrix.length == 0) {
        this.vectors = new SharedVector[0];
        return;
    }
    int cols = matrix[0].length;
    SharedVector[] tempVecs = new SharedVector[matrix.length];
    for (int i = 0; i < matrix.length; i++) {
        if (matrix[i] == null) {
            throw new IllegalArgumentException("Matrix row cant be null");
        }
        if (matrix[i].length != cols) {
            throw new IllegalArgumentException("Matrix must be rectangular");
        }

        tempVecs[i] = new SharedVector(matrix[i], VectorOrientation.ROW_MAJOR);
    }
    this.vectors = tempVecs;
         
    }
    


    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
   
    if (matrix == null) {
        throw new IllegalArgumentException("Matrix cant be null");
    }
    if (matrix.length == 0) {
        this.vectors = new SharedVector[0];
        return;
    }

    int rows = matrix.length;
    int cols = matrix[0].length;

    for (int i = 1; i < rows; i++) {
        if (matrix[i] == null || matrix[i].length != cols) {
            throw new IllegalArgumentException("Matrix must be rectangular");
        }
    }

    SharedVector[] newVectors = new SharedVector[cols];

    for (int j = 0; j < cols; j++) {
        double[] column = new double[rows];
        for (int i = 0; i < rows; i++) {
            column[i] = matrix[i][j];
        }
        newVectors[j] = new SharedVector(column, VectorOrientation.COLUMN_MAJOR);
    }
    this.vectors = newVectors;
    }

    public double[][] readRowMajor() 
    {
        // TODO: return matrix contents as a row-major double[][]
       if (vectors.length == 0)
           return new double[0][0];
        double [][]matrix;
      if (getOrientation() == VectorOrientation.ROW_MAJOR)
        {
          int rows = vectors.length;
          int cols = vectors[0].length();
          matrix = new double[rows][cols];  
           for(int i=0;i<rows;i++)
           {
            for(int j=0;j<cols;j++)
            {
                matrix[i][j]=vectors[i].get(j);
            }
           }

        }
        else{
           int cols = vectors.length;
          int rows = vectors[0].length();

        matrix = new double[rows][cols];
           for (int i = 0; i < cols; i++)
             {
               for (int j = 0; j < rows; j++) {
                matrix[j][i] = vectors[i].get(j);
               }
        }
        }
        
        return matrix;
    }
    

    public SharedVector get(int index) {
        // TODO: return vector at index
        return vectors[index];
    }

    public int length() {
        // TODO: return number of stored vectors
        return vectors.length;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        if (vectors.length == 0)
            return VectorOrientation.ROW_MAJOR; //defult
        return vectors[0].getOrientation();
    }

    private void acquireAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: acquire read lock for each vector
        for (SharedVector vec:vecs)
            vec.readLock();

    }

    private void releaseAllVectorReadLocks(SharedVector[] vecs) {
        // TODO: release read locks
        for(int i=vecs.length-1;i>=0;i--)
            vecs[i].readUnlock();
    }

    private void acquireAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: acquire write lock for each vector
        for (SharedVector vec:vecs)
            vec.writeLock();
        
    }

    private void releaseAllVectorWriteLocks(SharedVector[] vecs) {
        // TODO: release write locks
        for(int i=vecs.length-1;i>=0;i--)
            vecs[i].writeUnlock();
    }
}
