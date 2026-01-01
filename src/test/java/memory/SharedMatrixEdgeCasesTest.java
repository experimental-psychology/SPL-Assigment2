package memory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SharedMatrixEdgeCasesTest {

    @Test
    void constructor_nullMatrix_throws() {
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(null));
    }

    @Test
    void constructor_nonRectangular_throws() {
        double[][] bad = new double[][]{
                new double[]{1, 2},
                new double[]{3}
        };
        assertThrows(IllegalArgumentException.class, () -> new SharedMatrix(bad));
    }

    @Test
    void loadRowMajor_null_throws() {
        SharedMatrix m = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> m.loadRowMajor(null));
    }

    @Test
    void loadRowMajor_nonRectangular_throws() {
        SharedMatrix m = new SharedMatrix();
        double[][] bad = new double[][]{
                new double[]{1, 2},
                new double[]{3}
        };
        assertThrows(IllegalArgumentException.class, () -> m.loadRowMajor(bad));
    }

    @Test
    void loadColumnMajor_null_throws() {
        SharedMatrix m = new SharedMatrix();
        assertThrows(IllegalArgumentException.class, () -> m.loadColumnMajor(null));
    }

    @Test
    void loadColumnMajor_nonRectangular_throws() {
        SharedMatrix m = new SharedMatrix();
        double[][] bad = new double[][]{
                new double[]{1, 2},
                new double[]{3}
        };
        assertThrows(IllegalArgumentException.class, () -> m.loadColumnMajor(bad));
    }

    @Test
    void readRowMajor_empty_returns0x0() {
        SharedMatrix m = new SharedMatrix();
        double[][] out = m.readRowMajor();
        assertNotNull(out);
        assertEquals(0, out.length);
    }

    @Test
    void get_outOfBounds_throws() {
        SharedMatrix m = new SharedMatrix(new double[][]{
                new double[]{1, 2}
        });
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> m.get(1));
    }

    @Test
    void loadColumnMajor_thenReadRowMajor_reconstructsOriginal() {
        SharedMatrix m = new SharedMatrix();
        double[][] a = new double[][]{
                new double[]{1, 2, 3},
                new double[]{4, 5, 6}
        };
        m.loadColumnMajor(a);
        double[][] out = m.readRowMajor();
        assertArrayEquals(a, out);
    }

    @Test
    void loadRowMajor_thenReadRowMajor_sameData() {
        SharedMatrix m = new SharedMatrix();
        double[][] a = new double[][]{
                new double[]{1, 2},
                new double[]{3, 4},
                new double[]{5, 6}
        };
        m.loadRowMajor(a);
        double[][] out = m.readRowMajor();
        assertArrayEquals(a, out);
    }

    @Test
    void getOrientation_defaultEmpty_isRowMajor() {
        SharedMatrix m = new SharedMatrix();
        assertEquals(VectorOrientation.ROW_MAJOR, m.getOrientation());
    }
}
