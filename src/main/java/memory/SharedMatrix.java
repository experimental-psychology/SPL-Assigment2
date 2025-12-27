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

    public void loadRowMajor(double[][] matrix) {
        // TODO: replace internal data with new row-major matrix
    }

    public void loadColumnMajor(double[][] matrix) {
        // TODO: replace internal data with new column-major matrix
    }

    public double[][] readRowMajor() {
        // TODO: return matrix contents as a row-major double[][]
        return null;
    }

    public SharedVector get(int index) {
        // TODO: return vector at index
        return null;
    }

    public int length() {
        // TODO: return number of stored vectors
        return 0;
    }

    public VectorOrientation getOrientation() {
        // TODO: return orientation
        return null;
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
