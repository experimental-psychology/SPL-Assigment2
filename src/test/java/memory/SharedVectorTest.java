package memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SharedVectorTest {

    private double[] data;
    private SharedVector rowVec;
    private SharedVector colVec;

    @BeforeEach
    void init() {
        data = new double[]{1.0, 2.0, 3.0};
        rowVec = new SharedVector(data, VectorOrientation.ROW_MAJOR);
        colVec = new SharedVector(data, VectorOrientation.COLUMN_MAJOR);
    }

    /* =========================
       Constructor
       ========================= */

    @Test
    void testConstructorCreatesCopy() {
        data[0] = 100.0;
        double value = rowVec.get(0);
        if (value != 1.0) {
            throw new RuntimeException("Vector was not copied in constructor");
        }
    }

    @Test
    void testConstructorNullArray() {
        boolean exceptionThrown = false;
        try {
            new SharedVector(null, VectorOrientation.ROW_MAJOR);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for null array");
        }
    }

    @Test
    void testConstructorNullOrientation() {
        boolean exceptionThrown = false;
        try {
            new SharedVector(new double[]{1.0}, null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for null orientation");
        }
    }

    /* =========================
       Basic methods
       ========================= */

    @Test
    void testLength() {
        int len = rowVec.length();
        if (len != 3) {
            throw new RuntimeException("Length returned wrong value");
        }
    }

    @Test
    void testGetValidIndex() {
        double value = rowVec.get(1);
        if (value != 2.0) {
            throw new RuntimeException("get returned wrong value");
        }
    }

    @Test
    void testGetInvalidIndex() {
        boolean exceptionThrown = false;
        try {
            rowVec.get(-1);
        } catch (IndexOutOfBoundsException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for negative index");
        }

        exceptionThrown = false;
        try {
            rowVec.get(3);
        } catch (IndexOutOfBoundsException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for index out of range");
        }
    }

    @Test
    void testOrientation() {
        if (rowVec.getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new RuntimeException("Wrong orientation for row vector");
        }
        if (colVec.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new RuntimeException("Wrong orientation for column vector");
        }
    }

    /* =========================
       Transpose
       ========================= */

    @Test
    void testTranspose() {
        rowVec.transpose();
        if (rowVec.getOrientation() != VectorOrientation.COLUMN_MAJOR) {
            throw new RuntimeException("Transpose did not change orientation");
        }

        rowVec.transpose();
        if (rowVec.getOrientation() != VectorOrientation.ROW_MAJOR) {
            throw new RuntimeException("Second transpose failed");
        }
    }

    /* =========================
       Negate
       ========================= */

    @Test
    void testNegate() {
        rowVec.negate();

        if (rowVec.get(0) != -1.0 ||
            rowVec.get(1) != -2.0 ||
            rowVec.get(2) != -3.0) {
            throw new RuntimeException("Negate did not change signs correctly");
        }
    }

    /* =========================
       Add
       ========================= */

    @Test
    void testAddNormal() {
        SharedVector other =
                new SharedVector(new double[]{4.0, 5.0, 6.0},
                                 VectorOrientation.ROW_MAJOR);

        rowVec.add(other);

        if (rowVec.get(0) != 5.0 ||
            rowVec.get(1) != 7.0 ||
            rowVec.get(2) != 9.0) {
            throw new RuntimeException("Add produced wrong result");
        }
    }

    @Test
    void testAddSelf() {
        rowVec.add(rowVec);

        if (rowVec.get(0) != 2.0 ||
            rowVec.get(1) != 4.0 ||
            rowVec.get(2) != 6.0) {
            throw new RuntimeException("Adding vector to itself failed");
        }
    }

    @Test
    void testAddWrongOrientation() {
        boolean exceptionThrown = false;
        try {
            rowVec.add(colVec);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for orientation mismatch");
        }
    }

    @Test
    void testAddDifferentLengths() {
        SharedVector v1 =
                new SharedVector(new double[]{1.0, 2.0, 3.0},
                                 VectorOrientation.ROW_MAJOR);
        SharedVector v2 =
                new SharedVector(new double[]{4.0, 5.0},
                                 VectorOrientation.ROW_MAJOR);

        boolean exceptionThrown = false;
        try {
            v1.add(v2);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for vectors of different lengths");
        }
    }

    /* =========================
       Dot product
       ========================= */

    @Test
    void testDotProduct() {
        double result = rowVec.dot(colVec);
        if (result != 14.0) {
            throw new RuntimeException("Dot product returned wrong value");
        }
    }

    @Test
    void testDotSameOrientation() {
        boolean exceptionThrown = false;
        try {
            rowVec.dot(rowVec);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }
        if (!exceptionThrown) {
            throw new RuntimeException("Expected exception for same orientation");
        }
    }

    /* =========================
       Concurrency
       ========================= */

    @Test
    void testConcurrentAccessDoesNotCorruptVector() {
        SharedVector v =new SharedVector(new double[]{1.0, 1.0, 1.0},VectorOrientation.ROW_MAJOR);
        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++)
                v.negate();
        });
        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++)
                v.negate();
        });
        t1.start();
        t2.start();
        try {
            t1.join();
            t2.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Thread was interrupted");
        }
        if (v.get(0) != 1.0 || v.get(1) != 1.0 || v.get(2) != 1.0)
            throw new RuntimeException("Concurrent access corrupted vector values");
    }
}
