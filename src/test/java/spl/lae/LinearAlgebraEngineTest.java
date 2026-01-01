package spl.lae;

import org.junit.jupiter.api.Test;
import parser.ComputationNode;
import parser.ComputationNodeType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LinearAlgebraEngineTest {

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
    void run_leafMatrix_returnsSameMatrix() {
        double[][] a = {{1,2},{3,4}};
        ComputationNode root = new ComputationNode(a);
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        ComputationNode res = engine.run(root);
        assertMatrixEquals(a, res.getMatrix(), 1e-9);
    }

    @Test
    void negate_2x2() {
        double[][] a = {{1,-2},{0,4}};
        ComputationNode root = new ComputationNode(
                ComputationNodeType.NEGATE,
                List.of(new ComputationNode(a))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        ComputationNode res = engine.run(root);

        double[][] expected = {{-1,2},{-0,-4}};
        assertMatrixEquals(expected, res.getMatrix(), 1e-9);
    }

    @Test
    void transpose_rectangular_2x3_to_3x2() {
        double[][] a = {{1,2,3},{4,5,6}};
        ComputationNode root = new ComputationNode(
                ComputationNodeType.TRANSPOSE,
                List.of(new ComputationNode(a))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(3);
        ComputationNode res = engine.run(root);

        double[][] expected = {{1,4},{2,5},{3,6}};
        assertMatrixEquals(expected, res.getMatrix(), 1e-9);
    }

    @Test
    void add_2x2() {
        double[][] a = {{1,2},{3,4}};
        double[][] b = {{10,20},{30,40}};
        ComputationNode root = new ComputationNode(
                ComputationNodeType.ADD,
                List.of(new ComputationNode(a), new ComputationNode(b))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        ComputationNode res = engine.run(root);

        double[][] expected = {{11,22},{33,44}};
        assertMatrixEquals(expected, res.getMatrix(), 1e-9);
    }

    @Test
    void multiply_2x3_times_3x2() {
        double[][] a = {{1,2,3},{4,5,6}};          // 2x3
        double[][] b = {{7,8},{9,10},{11,12}};     // 3x2

        ComputationNode root = new ComputationNode(
                ComputationNodeType.MULTIPLY,
                List.of(new ComputationNode(a), new ComputationNode(b))
        );

        LinearAlgebraEngine engine = new LinearAlgebraEngine(4);
        ComputationNode res = engine.run(root);

        double[][] expected = {
                {58, 64},
                {139, 154}
        };
        assertMatrixEquals(expected, res.getMatrix(), 1e-9);
    }

    @Test
    void add_dimensionMismatch_throws() {
        double[][] a = {{1,2},{3,4}};
        double[][] b = {{1,2,3},{4,5,6}};
        ComputationNode root = new ComputationNode(
                ComputationNodeType.ADD,
                List.of(new ComputationNode(a), new ComputationNode(b))
        );
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    void multiply_dimensionMismatch_throws() {
        double[][] a = {{1,2},{3,4}};        // 2x2
        double[][] b = {{1,2,3}};            // 1x3
        ComputationNode root = new ComputationNode(
                ComputationNodeType.MULTIPLY,
                List.of(new ComputationNode(a), new ComputationNode(b))
        );
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }

    @Test
    void transpose_withTwoOperands_throws() {
        double[][] a = {{1}};
        double[][] b = {{2}};
        ComputationNode root = new ComputationNode(
                ComputationNodeType.TRANSPOSE,
                List.of(new ComputationNode(a), new ComputationNode(b))
        );
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        assertThrows(IllegalArgumentException.class, () -> engine.run(root));
    }
}
