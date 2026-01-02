package memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SharedMatrixBasicTests {

    private double[][] matrix2x2;
    private double[][] matrix2x3;
    private double[][] emptyMatrix;
    private SharedMatrix mat;

    @BeforeEach
    void init() {
        matrix2x2 = new double[][]{
                {1.0, 2.0},
                {3.0, 4.0}
        };

        matrix2x3 = new double[][]{
                {1.0, 2.0, 3.0},
                {4.0, 5.0, 6.0}
        };

        emptyMatrix = new double[0][0];

        mat = new SharedMatrix(matrix2x2);
    }

    /* =========================
       Constructors
       ========================= */

    @Test
    void testEmptyConstructor() {
        SharedMatrix m = new SharedMatrix();
        if (m.length() != 0) {
            throw new RuntimeException("Empty constructor should create empty matrix");
        }
    }

    @Test
    void testConstructorDeepCopy() {
        matrix2x2[0][0] = 999.0;

        double[][] result = mat.readRowMajor();
        if (result[0][0] != 1.0) {
            throw new RuntimeException("Constructor did not perform deep copy");
        }
    }

    @Test
    void testConstructorNullMatrix() {
        boolean exceptionThrown = false;
        try {
            new SharedMatrix(null);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for null matrix");
        }
    }

    @Test
    void testConstructorNonRectangularMatrix() {
        double[][] bad = {
                {1.0, 2.0},
                {3.0}
        };

        boolean exceptionThrown = false;
        try {
            new SharedMatrix(bad);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for non-rectangular matrix");
        }
    }

    /* =========================
       loadRowMajor
       ========================= */

    @Test
    void testLoadRowMajorValid() {
        mat.loadRowMajor(matrix2x3);

        if (mat.length() != 2) {
            throw new RuntimeException("loadRowMajor loaded wrong number of rows");
        }

        if (mat.getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new RuntimeException("loadRowMajor should result in ROW_MAJOR matrix");
        }
    }

    @Test
    void testLoadRowMajorEmpty() {
        mat.loadRowMajor(emptyMatrix);

        if (mat.length() != 0) {
            throw new RuntimeException("Loading empty matrix should result in empty matrix");
        }
    }

    @Test
    void testLoadRowMajorNull() {
        boolean exceptionThrown = false;
        try {
            mat.loadRowMajor(null);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for null matrix");
        }
    }

    /* =========================
       loadColumnMajor
       ========================= */

    @Test
    void testLoadColumnMajorValid() {
        mat.loadColumnMajor(matrix2x2);

        if (mat.length() != 2) {
            throw new RuntimeException("loadColumnMajor created wrong number of vectors");
        }

        if (mat.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new RuntimeException("loadColumnMajor should result in COLUMN_MAJOR matrix");
        }
    }

    @Test
    void testLoadColumnMajorEmpty() {
        mat.loadColumnMajor(emptyMatrix);

        if (mat.length() != 0) {
            throw new RuntimeException("Loading empty column-major matrix should result in empty matrix");
        }
    }

    @Test
    void testLoadColumnMajorNull() {
        boolean exceptionThrown = false;
        try {
            mat.loadColumnMajor(null);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for null matrix");
        }
    }

    /* =========================
       In-place modification
       ========================= */

    @Test
    void testInPlaceVectorModificationAffectsMatrix() {
        SharedVector firstRow = mat.get(0);
        firstRow.negate();

        double[][] result = mat.readRowMajor();
        if (result[0][0] != -1.0 || result[0][1] != -2.0) {
            throw new RuntimeException("In-place modification did not affect matrix");
        }
    }

    /* =========================
       readRowMajor
       ========================= */

    @Test
    void testReadRowMajorFromRowMajor() {
        double[][] result = mat.readRowMajor();

        if (result.length != 2 || result[0].length != 2) {
            throw new RuntimeException("readRowMajor returned wrong dimensions");
        }

        if (result[1][1] != 4.0) {
            throw new RuntimeException("readRowMajor returned wrong values");
        }
    }

    @Test
    void testReadRowMajorFromColumnMajor() {
        mat.loadColumnMajor(matrix2x2);
        double[][] result = mat.readRowMajor();

        if (result[0][1] != 2.0 || result[1][0] != 3.0) {
            throw new RuntimeException("readRowMajor failed for column-major matrix");
        }
    }

    /* =========================
       Concurrency
       ========================= */

    @Test
    void testConcurrentModificationOnDifferentRows() {
        SharedVector row0 = mat.get(0);
        SharedVector row1 = mat.get(1);

        SharedVector addVec0 =
                new SharedVector(new double[]{1.0, 1.0},
                                 VectorOrientation.ROW_MAJOR);

        SharedVector addVec1 =
                new SharedVector(new double[]{2.0, 2.0},
                                 VectorOrientation.ROW_MAJOR);

        Thread t1 = new Thread(() -> {
            row0.add(addVec0);
        });

        Thread t2 = new Thread(() -> {
            row1.add(addVec1);
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread interrupted");
        }

        double[][] result = mat.readRowMajor();

        if (result[0][0] != 2.0 || result[0][1] != 3.0) {
            throw new RuntimeException("Concurrent update failed on row 0");
        }

        if (result[1][0] != 5.0 || result[1][1] != 6.0) {
            throw new RuntimeException("Concurrent update failed on row 1");
        }
    }

    /* =========================
       get / length / orientation
       ========================= */

    @Test
    void testGetInvalidIndex() {
        boolean exceptionThrown = false;
        try {
            mat.get(-1);
        } catch (IndexOutOfBoundsException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for invalid index");
        }
    }

    @Test
    void testGetOrientationEmptyMatrix() {
        SharedMatrix m = new SharedMatrix();
        if (m.getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new RuntimeException("Empty matrix orientation should default to ROW_MAJOR");
        }
    }
}
