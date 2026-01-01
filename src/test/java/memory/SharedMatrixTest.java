package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SharedMatrixTest {

    static void assertMatrixEquals(double[][] expected, double[][] actual, double eps) {
        assertEquals(expected.length, actual.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i].length, actual[i].length);
            for (int j = 0; j < expected[i].length; j++) {
                assertEquals(expected[i][j], actual[i][j], eps);
            }
        }
    }

    @Test
    void loadRowMajor_setsRowMajorOrientation() {
        SharedMatrix m = new SharedMatrix();
        m.loadRowMajor(new double[][]{{1,2},{3,4}});
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
        assertEquals(2, m.length());
        assertEquals(2, m.get(0).length());
    }

    @Test
    void loadColumnMajor_setsColumnMajorOrientation() {
        SharedMatrix m = new SharedMatrix();
        m.loadColumnMajor(new double[][]{{1,2},{3,4}});
        assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());
        assertEquals(2, m.length()); // number of columns stored as vectors
        assertEquals(2, m.get(0).length()); // each column has 2 rows
    }

    @Test
    void readRowMajor_fromRowMajor_returnsSameMatrix() {
        SharedMatrix m = new SharedMatrix();
        double[][] a = {{1,2,3},{4,5,6}};
        m.loadRowMajor(a);
        assertMatrixEquals(a, m.readRowMajor(), 1e-9);
    }

    @Test
    void readRowMajor_fromColumnMajor_returnsRowMajorMatrix() {
        SharedMatrix m = new SharedMatrix();
        double[][] a = {{1,2,3},{4,5,6}}; // 2x3
        m.loadColumnMajor(a);             // internally stored by columns
        assertMatrixEquals(a, m.readRowMajor(), 1e-9);
    }

    @Test
    void loadRowMajor_nonRectangular_throws() {
        SharedMatrix m = new SharedMatrix();
        assertThrows(IllegalArgumentException.class,
                () -> m.loadRowMajor(new double[][]{{1,2},{3}}));
    }

    @Test
    void loadColumnMajor_nonRectangular_throws() {
        SharedMatrix m = new SharedMatrix();
        assertThrows(IllegalArgumentException.class,
                () -> m.loadColumnMajor(new double[][]{{1,2},{3}}));
    }

    @Test
    void get_outOfBounds_throws() {
        SharedMatrix m = new SharedMatrix();
        m.loadRowMajor(new double[][]{{1}});
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(1));
    }

  @Test
void readRowMajor_emptyMatrix_returns0x0() {
    SharedMatrix m = new SharedMatrix();
    double[][] out = m.readRowMajor();
    assertEquals(0, out.length);
}
@Test
void getOrientation_afterLoadRowMajor_isRowMajor() {
    SharedMatrix m = new SharedMatrix();
    m.loadRowMajor(new double[][]{{1, 2}, {3, 4}});
    assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
}

@Test
void getOrientation_afterLoadColumnMajor_isColumnMajor() {
    SharedMatrix m = new SharedMatrix();
    m.loadColumnMajor(new double[][]{{1, 2}, {3, 4}});
    assertEquals(VectorOrientation.COLUMN_MAJOR, m.getOrientation());
}

@Test
void readRowMajor_afterLoadColumnMajor_returnsOriginalMatrix() {
    double[][] original = new double[][]{
            {1, 2, 3},
            {4, 5, 6}
    };

    SharedMatrix m = new SharedMatrix();
    m.loadColumnMajor(original);

    double[][] out = m.readRowMajor();
    assertEquals(2, out.length);
    assertArrayEquals(new double[]{1, 2, 3}, out[0], 1e-9);
    assertArrayEquals(new double[]{4, 5, 6}, out[1], 1e-9);
}

@Test
void readRowMajor_returnsDeepCopy() {
    double[][] original = new double[][]{
            {1, 2},
            {3, 4}
    };

    SharedMatrix m = new SharedMatrix();
    m.loadRowMajor(original);

    double[][] out1 = m.readRowMajor();
    out1[0][0] = 999;

    double[][] out2 = m.readRowMajor();
    assertEquals(1.0, out2[0][0], 1e-9);
}

}
