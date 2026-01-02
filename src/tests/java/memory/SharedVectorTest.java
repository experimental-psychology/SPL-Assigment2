package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SharedVectorTest {

    @Test
    void ctor_makesDeepCopy() {
        double[] arr = {1,2,3};
        SharedVector v = new SharedVector(arr, VectorOrientation.ROW_MAJOR);
        arr[0] = 99;
        assertEquals(1, v.get(0));
    }

    @Test
    void transpose_togglesOrientation() {
        SharedVector v = new SharedVector(new double[]{1,2}, VectorOrientation.ROW_MAJOR);
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void add_validAddsElementwise() {
        SharedVector a = new SharedVector(new double[]{1,2,3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{4,5,6}, VectorOrientation.ROW_MAJOR);

        a.add(b);

        assertEquals(5, a.get(0));
        assertEquals(7, a.get(1));
        assertEquals(9, a.get(2));
    }

    @Test
    void add_sameInstance_doublesVector() {
        SharedVector a = new SharedVector(new double[]{1,2,3}, VectorOrientation.ROW_MAJOR);
        a.add(a);
        assertEquals(2, a.get(0));
        assertEquals(4, a.get(1));
        assertEquals(6, a.get(2));
    }

    @Test
    void add_orientationMismatch_throws() {
        SharedVector a = new SharedVector(new double[]{1,2}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{3,4}, VectorOrientation.COLUMN_MAJOR);

        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void dot_requiresDifferentOrientationAndSameLength() {
        SharedVector row = new SharedVector(new double[]{1,2,3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{4,5,6}, VectorOrientation.COLUMN_MAJOR);

        assertEquals(32.0, row.dot(col), 1e-9);
    }

    @Test
    void dot_sameOrientation_throws() {
        SharedVector a = new SharedVector(new double[]{1,2}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{3,4}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.dot(b));
    }

    @Test
    void negate_inPlace() {
        SharedVector v = new SharedVector(new double[]{1,-2,3}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-1, v.get(0));
        assertEquals(2, v.get(1));
        assertEquals(-3, v.get(2));
    }

    @Test
    void vecMatMul_rowMajorMatrix_success() {
        SharedVector row = new SharedVector(new double[]{1,2}, VectorOrientation.ROW_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][]{
                {3,4,5},
                {6,7,8}
        }); // ROW_MAJOR

        row.vecMatMul(m); // [1,2] * m = [1*3+2*6, 1*4+2*7, 1*5+2*8] = [15,18,21]

        assertEquals(VectorOrientation.ROW_MAJOR, row.getOrientation());
        assertEquals(3, row.length());
        assertEquals(15, row.get(0), 1e-9);
        assertEquals(18, row.get(1), 1e-9);
        assertEquals(21, row.get(2), 1e-9);
    }

    @Test
    void vecMatMul_requiresRowMajorVector() {
        SharedVector col = new SharedVector(new double[]{1,2}, VectorOrientation.COLUMN_MAJOR);
        SharedMatrix m = new SharedMatrix(new double[][]{{1,2},{3,4}});
        assertThrows(IllegalArgumentException.class, () -> col.vecMatMul(m));
    }
}
