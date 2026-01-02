package spl.lae;

import org.junit.jupiter.api.Test;
import parser.*;
import scheduling.TiredThread;
import memory.*;

import java.util.List;

public class LinearAlgebraEngineBasicTests {

    /* =========================
       Helpers
       ========================= */

    private ComputationNode matrixNode(double[][] mat) {
        return new ComputationNode(mat);
    }

    private ComputationNode unaryNode(ComputationNodeType type, ComputationNode child) {
        return new ComputationNode(type, List.of(child));
    }

    private ComputationNode binaryNode(ComputationNodeType type,
                                       ComputationNode left,
                                       ComputationNode right) {
        return new ComputationNode(type, List.of(left, right));
    }

    private void checkMatrixEquals(double[][] actual,
                                    double[][] expected,
                                    String msg) {
        if (actual.length != expected.length)
            throw new RuntimeException(msg + " (row count mismatch)");

        for (int i = 0; i < actual.length; i++) {
            if (actual[i].length != expected[i].length)
                throw new RuntimeException(msg + " (column count mismatch)");

            for (int j = 0; j < actual[i].length; j++) {
                if (actual[i][j] != expected[i][j])
                    throw new RuntimeException(
                        msg + " at (" + i + "," + j + "), expected "
                        + expected[i][j] + " got " + actual[i][j]
                    );
            }
        }
    }
    
    private void assertMatrixEquals(double[][] actual, double[][] expected) {
        if (actual.length != expected.length)
            throw new RuntimeException("Row count mismatch");
        for (int i = 0; i < actual.length; i++) {
            if (actual[i].length != expected[i].length)
                throw new RuntimeException("Column count mismatch at row " + i);
            for (int j = 0; j < actual[i].length; j++) {
                if (Math.abs(actual[i][j] - expected[i][j]) > 1e-9)
                    throw new RuntimeException("Value mismatch at [" + i + "," + j + "]");
            }
        }
    }

    /* =========================
       Tests
       ========================= */

    @Test
    void testRootIsAlreadyMatrix() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        double[][] mat = {{1.0, 2.0}};
        ComputationNode root = matrixNode(mat);

        ComputationNode result = lae.run(root);
        checkMatrixEquals(result.getMatrix(), mat, "Matrix root not returned directly");
    }

    @Test
    void testUnaryNegate() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        double[][] mat = {{1.0, -2.0}};
        ComputationNode root =
                unaryNode(ComputationNodeType.NEGATE,
                          matrixNode(mat));

        ComputationNode result = lae.run(root);

        double[][] expected = {{-1.0, 2.0}};
        checkMatrixEquals(result.getMatrix(), expected, "Negate failed");
    }

    @Test
    void testTranspose() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        double[][] mat = {
                {1.0, 2.0},
                {3.0, 4.0}
        };

        ComputationNode root =
                unaryNode(ComputationNodeType.TRANSPOSE,
                          matrixNode(mat));

        ComputationNode result = lae.run(root);

        double[][] expected = {
                {1.0, 3.0},
                {2.0, 4.0}
        };

        checkMatrixEquals(result.getMatrix(), expected, "Transpose failed");
    }

    @Test
    void testAddTwoMatrices() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        double[][] a = {{1.0, 2.0}};
        double[][] b = {{3.0, 4.0}};

        ComputationNode root =
                binaryNode(ComputationNodeType.ADD,
                           matrixNode(a),
                           matrixNode(b));

        ComputationNode result = lae.run(root);

        double[][] expected = {{4.0, 6.0}};
        checkMatrixEquals(result.getMatrix(), expected, "Add failed");
    }

    @Test
    void testMultiplyMatrices() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        double[][] a = {{1.0, 2.0}};
        double[][] b = {
                {3.0},
                {4.0}
        };

        ComputationNode root =
                binaryNode(ComputationNodeType.MULTIPLY,
                           matrixNode(a),
                           matrixNode(b));

        ComputationNode result = lae.run(root);

        double[][] expected = {{11.0}};
        checkMatrixEquals(result.getMatrix(), expected, "Multiply failed");
    }

    @Test
    void testInvalidAddDimensions() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(2);

        double[][] a = {{1.0}};
        double[][] b = {{1.0, 2.0}};

        ComputationNode root =
                binaryNode(ComputationNodeType.ADD,
                           matrixNode(a),
                           matrixNode(b));

        boolean exceptionThrown = false;
        try {
            lae.run(root);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown)
            throw new RuntimeException("Expected dimension mismatch exception for ADD");
    }

    @Test
    void testNullRoot() {
        LinearAlgebraEngine lae = new LinearAlgebraEngine(1);

        boolean exceptionThrown = false;
        try {
            lae.run(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }

        if (!exceptionThrown)
            throw new RuntimeException("Expected NullPointerException for null root");
    }

    @Test
    void testNestedAddThenMultiply() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);

        double[][] A = {{1, 2}, {3, 4}};
        double[][] B = {{5, 6}, {7, 8}};
        double[][] C = {{1}, {1}};

        ComputationNode add = new ComputationNode(
                ComputationNodeType.ADD,
                List.of(new ComputationNode(A), new ComputationNode(B))
        );

        ComputationNode root = new ComputationNode(
                ComputationNodeType.MULTIPLY,
                List.of(add, new ComputationNode(C))
        );

        ComputationNode result = engine.run(root);

        double[][] expected = {
                {14},
                {22}
        };

        assertMatrixEquals(result.getMatrix(), expected);
    }

    @Test
    void testRunCallsShutdownCleanly() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(1);

        double[][] A = {{1, -1}};
        ComputationNode root = new ComputationNode(A);

        ComputationNode result = engine.run(root);

        assertMatrixEquals(result.getMatrix(), A);
    }

    @Test
    void testTiredThreadTimeUsedAndFatigue() throws InterruptedException {
        TiredThread worker = new TiredThread(0, 1.0);
        worker.start();

        worker.newTask(() -> {
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}
        });

        Thread.sleep(100);
        worker.shutdown();
        worker.join();

        if (worker.getTimeUsed() <= 0)
            throw new RuntimeException("timeUsed was not updated");

        if (worker.getFatigue() <= 0)
            throw new RuntimeException("Fatigue should be positive");
    }

    @Test
    void testTiredThreadIdleTimeIncreases() throws InterruptedException {
        TiredThread worker = new TiredThread(1, 1.0);
        worker.start();

        Thread.sleep(50);
        worker.shutdown();
        worker.join();

        if (worker.getTimeIdle() <= 0)
            throw new RuntimeException("timeIdle was not updated");
    }

    @Test
    void testTiredThreadPoisonPillStopsThread() throws InterruptedException {
        TiredThread worker = new TiredThread(2, 1.0);
        worker.start();

        worker.shutdown();
        worker.join(500);

        if (worker.isAlive())
            throw new RuntimeException("Worker thread did not terminate");
    }

    @Test
    void testDeepNestedTreeAddMultiplyAdd() {
        LinearAlgebraEngine engine = new LinearAlgebraEngine(2);
        double[][] A = {{1, 1}};
        double[][] B = {{2, 2}};
        double[][] C = {{1}, {1}};
        double[][] D = {{1}, {1}};
        //(A + B)
        ComputationNode leftAdd = new ComputationNode(
                ComputationNodeType.ADD,
                List.of(new ComputationNode(A), new ComputationNode(B))
        );
        //(C + D)
        ComputationNode rightAdd = new ComputationNode(
                ComputationNodeType.ADD,
                List.of(new ComputationNode(C), new ComputationNode(D))
        );
        //(A + B) * (C + D)
        ComputationNode root = new ComputationNode(
                ComputationNodeType.MULTIPLY,
                List.of(leftAdd, rightAdd)
        );
        ComputationNode result = engine.run(root);
        double[][] expected = {{12}};
        assertMatrixEquals(result.getMatrix(), expected);
    }
}
