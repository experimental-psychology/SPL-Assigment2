package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SharedVectorEdgeCasesTest {

    @Test
    void add_nullOther_throws() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(NullPointerException.class, () -> v.add(null));
    }

    @Test
    void add_sameInstance_doublesVector() {
        SharedVector v = new SharedVector(new double[]{1, -2, 3.5}, VectorOrientation.ROW_MAJOR);
        v.add(v);
        assertEquals(2.0, v.get(0), 1e-9);
        assertEquals(-4.0, v.get(1), 1e-9);
        assertEquals(7.0, v.get(2), 1e-9);
    }

    @Test
    void add_orientationMismatch_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{3, 4}, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void add_lengthMismatch_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{4, 5}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.add(b));
    }

    @Test
    void negate_flipsSigns() {
        SharedVector v = new SharedVector(new double[]{1, -2, 0}, VectorOrientation.ROW_MAJOR);
        v.negate();
        assertEquals(-1.0, v.get(0), 1e-9);
        assertEquals(2.0, v.get(1), 1e-9);
        assertEquals(-0.0, v.get(2), 1e-9);
    }

    @Test
    void transpose_togglesOrientation() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.COLUMN_MAJOR, v.getOrientation());
        v.transpose();
        assertEquals(VectorOrientation.ROW_MAJOR, v.getOrientation());
    }

    @Test
    void dot_nullOther_throws() {
        SharedVector row = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(NullPointerException.class, () -> row.dot(null));
    }

    @Test
    void dot_sameOrientation_throws() {
        SharedVector a = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        SharedVector b = new SharedVector(new double[]{3, 4}, VectorOrientation.ROW_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> a.dot(b));
    }

    @Test
    void dot_lengthMismatch_throws() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{4, 5}, VectorOrientation.COLUMN_MAJOR);
        assertThrows(IllegalArgumentException.class, () -> row.dot(col));
    }

    @Test
    void dot_validRowCol_computesCorrectly() {
        SharedVector row = new SharedVector(new double[]{1, 2, 3}, VectorOrientation.ROW_MAJOR);
        SharedVector col = new SharedVector(new double[]{4, -1, 2}, VectorOrientation.COLUMN_MAJOR);
        double res = row.dot(col);
        assertEquals(1 * 4 + 2 * (-1) + 3 * 2, res, 1e-9);
    }

    @Test
    void get_outOfBounds_throws() {
        SharedVector v = new SharedVector(new double[]{1, 2}, VectorOrientation.ROW_MAJOR);
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> v.get(2));
    }
}
