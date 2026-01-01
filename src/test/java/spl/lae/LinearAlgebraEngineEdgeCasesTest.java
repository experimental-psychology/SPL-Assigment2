package spl.lae;

import memory.SharedMatrix;
import org.junit.jupiter.api.Test;
import parser.ComputationNode;
import parser.ComputationNodeType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LinearAlgebraEngineEdgeCasesTest {

    private static void assertMatrixEquals(double[][] expected, double[][] actual) {
        assertNotNull(actual);
        assertEquals(expected.length, actual.length, "Row count mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertArrayEquals(expected[i], actual[i], 1e-9, "Row " + i + " mismatch");
        }
    }

    private static double[][] matMul(double[][] A, double[][] B) {
        int aRows = A.length;
        int aCols = (aRows == 0 ? 0 : A[0].length);
        int bRows = B.length;
        int bCols = (bRows == 0 ? 0 : B[0].length);
        if (aCols != bRows) throw new IllegalArgumentException("dimensions mismatch");

        double[][] out = new double[aRows][bCols];
        for (int i = 0; i < aRows; i++) {
            for (int k = 0; k < aCols; k++) {
                double aik = A[i][k];
                for (int j = 0; j < bCols; j++) {
                    out[i][j] += aik * B[k][j];
                }
            }
        }
        return out;
    }

    private static double[][] matAdd(double[][] A, double[][] B) {
        int r = A.length;
        int c = (r == 0 ? 0 : A[0].length);
        if (B.length != r || (r > 0 && B[0].length != c)) throw new IllegalArgumentException("dimensions mismatch");
        double[][] out = new double[r][c];
        for (int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++) {
                out[i][j] = A[i][j] + B[i][j];
            }
        }
        return out;
    }

    @Test
    void add_threeOperands_works() {
        double[][] A = {{1, 2}, {3, 4}};
        double[][] B = {{10, 20}, {30, 40}};
        double[][] C = {{-1, -2}, {-3, -4}};

        ComputationNode root = new ComputationNode(
                ComputationNodeType.ADD,
                List.of(new ComputationNode(A), new ComputationNode(B), new ComputationNode(C))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(4);
        ComputationNode result = engine.run(root);

        double[][] expected = matAdd(matAdd(A, B), C);
        assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
        assertMatrixEquals(expected, result.getMatrix());
    }

    @Test
    void multiply_threeOperands_works() {
        double[][] A = {
                {1, 2, 3},
                {4, 5, 6}
        }; // 2x3
        double[][] B = {
                {1, 0},
                {0, 1},
                {1, 1}
        }; // 3x2
        double[][] C = {
                {2, 0, 1},
                {1, 3, 0}
        }; // 2x3

        ComputationNode root = new ComputationNode(
                ComputationNodeType.MULTIPLY,
                List.of(new ComputationNode(A), new ComputationNode(B), new ComputationNode(C))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(4);
        ComputationNode result = engine.run(root);

        double[][] expected = matMul(matMul(A, B), C);
        assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
        assertMatrixEquals(expected, result.getMatrix());
    }

    @Test
    void transpose_returnsTrueTranspose() {
        double[][] A = {
                {1, 2, 3},
                {4, 5, 6}
        }; // 2x3

        ComputationNode root = new ComputationNode(
                ComputationNodeType.TRANSPOSE,
                List.of(new ComputationNode(A))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(3);
        ComputationNode result = engine.run(root);

        double[][] expected = {
                {1, 4},
                {2, 5},
                {3, 6}
        }; // 3x2

        assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
        assertMatrixEquals(expected, result.getMatrix());
    }

    @Test
    void negate_works() {
        double[][] A = {
                {1, -2},
                {0, 3}
        };

        ComputationNode root = new ComputationNode(
                ComputationNodeType.NEGATE,
                List.of(new ComputationNode(A))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        ComputationNode result = engine.run(root);

        double[][] expected = {
                {-1, 2},
                {-0.0, -3}
        };

        assertEquals(ComputationNodeType.MATRIX, result.getNodeType());
        assertMatrixEquals(expected, result.getMatrix());
    }

    @Test
    void add_dimensionMismatch_throws() {
        double[][] A = {{1, 2}, {3, 4}};
        double[][] B = {{1, 2, 3}, {4, 5, 6}};

        ComputationNode root = new ComputationNode(
                ComputationNodeType.ADD,
                List.of(new ComputationNode(A), new ComputationNode(B))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    void multiply_dimensionMismatch_throws() {
        double[][] A = {{1, 2, 3}, {4, 5, 6}}; // 2x3
        double[][] B = {{1, 2}, {3, 4}};       // 2x2

        ComputationNode root = new ComputationNode(
                ComputationNodeType.MULTIPLY,
                List.of(new ComputationNode(A), new ComputationNode(B))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    void transpose_withTwoOperands_throws() {
        double[][] A = {{1, 2}, {3, 4}};
        double[][] B = {{5, 6}, {7, 8}};

        ComputationNode root = new ComputationNode(
                ComputationNodeType.TRANSPOSE,
                List.of(new ComputationNode(A), new ComputationNode(B))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    void sharedMatrix_readRowMajor_empty_returns0x0() {
        SharedMatrix m = new SharedMatrix();
        double[][] out = m.readRowMajor();
        assertNotNull(out);
        assertEquals(0, out.length);
    }
}
