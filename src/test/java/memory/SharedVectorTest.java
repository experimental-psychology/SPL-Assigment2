package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SharedVectorTest {

    @Test
    void transpose_togglesOrientation() {
        SharedVector v = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void negate_inPlace() {
        SharedVector v = new SharedVector(new double[]{1, -2, 0}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-1, v.get(0), 1e-9);
        assertEquals(2, v.get(1), 1e-9);
        assertEquals(-0, v.get(2), 1e-9);
    }

    @Test
    void add_sameOrientationSameLength() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{10, 20, 30}, VectorOrientation.ROW_MAJOR);
        a.add(b);
        assertEquals(11, a.get(0), 1e-9);
        assertEquals(22, a.get(1), 1e-9);
        assertEquals(33, a.get(2), 1e-9);
    }

    @Test
    void add_lengthMismatch_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void dot_rowWithColumn_ok() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{10, 20, 30}, VectorOrientation.COLUMN_MAJOR);
        assertEquals(140, row.dot(col), 1e-9);
    }

    @Test
    void dot_sameOrientation_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{10, 20, 30}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.dot(b));
    }
    @Test
void transpose_rowToCol_toRow() {
    SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
    assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());

    v.transpose();
    assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());

    v.transpose();
    assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
}

@Test
void dot_validRowDotColumn_returnsCorrectValue() {
    SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
    SharedVector col = new SharedVector(new double[]{4, 5, 6}, VectorOrientation.COLUMN_MAJOR);
    assertEquals(32.0, row.dot(col), 1e-9);
}

@Test
void constructor_deepCopy_inputArrayChangeDoesNotAffectVector() {
    double[] arr = new double[]{1, 2, 3};
    SharedVector v = new SharedVector(arr, VectorOrientation.ROW_MAJOR);
    arr[0] = 999;
    assertEquals(1.0, v.get(0), 1e-9);
}

}
