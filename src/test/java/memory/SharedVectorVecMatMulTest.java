package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SharedVectorVecMatMulTest {

    static void assertArrayEqualsEps(double[] expected, double[] actual, double eps) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], eps);
        }
    }

    @Test
    void vecMatMul_rowMajorMatrix_basic() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        SharedMatrix m = new SharedMatrix();
        m.loadRowMajor(new double[][]{
                {3, 4, 5},
                {6, 7, 8}
        }); // 2x3

        v.vecMatMul(m); // (1,2) * M = [1*3+2*6, 1*4+2*7, 1*5+2*8] = [15,18,21]

        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        assertArrayEqualsEps(new double[]{15, 18, 21}, toArray(v), 1e-9);
    }

    @Test
    void vecMatMul_columnMajorMatrix_basic() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);

        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{
                {3, 4, 5},
                {6, 7, 8}
        }); // 2x3 but stored as columns (3 columns each length 2)

        v.vecMatMul(m);

        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        assertArrayEqualsEps(new double[]{15, 18, 21}, toArray(v), 1e-9);
    }

    @Test
    void vecMatMul_emptyMatrix_resultEmptyVector() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(); // empty
        v.vecMatMul(m);
        assertEquals(0, v.length());
    }

    @Test
    void vecMatMul_vectorNotRowMajor_throws() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.COLUMN_MAJOR);
        SharedMatrix m = new SharedMatrix();
        m.loadRowMajor(new double[][]{{1,2},{3,4}});
        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    void vecMatMul_rowMajor_dimensionMismatch_throws() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);

        SharedMatrix m = new SharedMatrix();
        m.loadRowMajor(new double[][]{
                {1, 2},
                {3, 4}
        }); // 2x2 but vector length is 3 => mismatch

        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    void vecMatMul_columnMajor_dimensionMismatch_throws() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);

        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{
                {1, 2},
                {3, 4}
        }); // 2x2, in COLUMN_MAJOR: first vector length is 2, must equal v.length=3

        assertThrows(IllegalArgumentException.class, () -> v.vecMatMul(m));
    }

    @Test
    void transpose_onlyTogglesOrientation_notValues() {
        SharedVector v = new SharedVector(new double[]{9, 8, 7}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        assertArrayEqualsEps(new double[]{9,8,7}, toArray(v), 1e-9);

        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        assertArrayEqualsEps(new double[]{9,8,7}, toArray(v), 1e-9);
    }

    private static double[] toArray(SharedVector v) {
        double[] out = new double[v.length()];
        for (int i = 0; i < out.length; i++) {
            out[i] = v.get(i);
        }
        return out;
    }
    @Test
void vecMatMul_rowMajorMatrix_validMultiplication() {
    SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
    SharedMatrix m = new SharedMatrix();
    m.loadRowMajor(new double[][]{
            {3, 4, 5},
            {6, 7, 8}
    });

    row.vecMatMul(m);

    assertEquals(3, row.length());
    assertEquals(VectorOrientation.ROW_MAJOR, row.getOrientation());
    assertEquals(15.0, row.get(0), 1e-9);
    assertEquals(18.0, row.get(1), 1e-9);
    assertEquals(21.0, row.get(2), 1e-9);
}

@Test
void vecMatMul_columnMajorMatrix_validMultiplication() {
    SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
    SharedMatrix m = new SharedMatrix();
    m.loadColumnMajor(new double[][]{
            {3, 4, 5},
            {6, 7, 8}
    });

    row.vecMatMul(m);

    assertEquals(3, row.length());
    assertEquals(VectorOrientation.ROW_MAJOR, row.getOrientation());
    assertEquals(15.0, row.get(0), 1e-9);
    assertEquals(18.0, row.get(1), 1e-9);
    assertEquals(21.0, row.get(2), 1e-9);
}

}
