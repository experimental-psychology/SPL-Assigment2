package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SharedMatrixTest {

    @Test
    void emptyCtor_createsEmptyRowMajorByDefault() {
        SharedMatrix m = new SharedMatrix();
        assertEquals(0, m.length());
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
        assertArrayEquals(new double[0][0], m.readRowMajor());
    }

    @Test
    void ctor_rectangularRequired() {
        assertThrows(IllegalArgumentException.class, () ->
                new SharedMatrix(new double[][]{{1,2},{3}}));
    }

    @Test
    void loadRowMajor_thenReadRowMajor_sameContent() {
        SharedMatrix m = new SharedMatrix();
        double[][] a = {{1,2},{3,4}};

        m.loadRowMajor(a);
        double[][] out = m.readRowMajor();

        assertArrayEquals(new double[]{1,2}, out[0]);
        assertArrayEquals(new double[]{3,4}, out[1]);
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
    }

    @Test
    void loadColumnMajor_setsColumnVectorsAndReadRowMajorStillReturnsOriginalMatrix() {
        SharedMatrix m = new SharedMatrix();
        double[][] a = {
                {1,2,3},
                {4,5,6}
        };

        m.loadColumnMajor(a);

        assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());
        double[][] out = m.readRowMajor();

        assertEquals(2, out.length);
        assertArrayEquals(new double[]{1,2,3}, out[0]);
        assertArrayEquals(new double[]{4,5,6}, out[1]);
    }

    @Test
    void get_outOfBounds_throws() {
        SharedMatrix m = new SharedMatrix(new double[][]{{1}});
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(-1));
    }
}
