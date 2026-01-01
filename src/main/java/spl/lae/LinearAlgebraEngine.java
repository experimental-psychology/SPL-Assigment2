package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;

public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;

    public LinearAlgebraEngine(int numThreads) {
        if (numThreads <= 0)
            throw new IllegalArgumentException("numThreads must be positive");
        executor = new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        if (computationRoot == null)
            throw new NullPointerException("computationRoot is null");

        try {
            while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {
                ComputationNode resolved = computationRoot.findResolvable();
                if (resolved == null)
                    throw new IllegalStateException("Not found");
                loadAndCompute(resolved);
            }
            return computationRoot;
        } finally {
            // חשוב כדי שה־JAR ייסגר וה־tester לא יהרוג את התהליך לפני הדוח
            try {
                executor.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during executor shutdown", e);
            }
        }
    }

    public void loadAndCompute(ComputationNode node) {
        if (node == null)
            throw new NullPointerException("Node is null");

        ComputationNodeType type = node.getNodeType();
        if (type == null)
            throw new IllegalStateException("Node type is null");

        List<ComputationNode> children = node.getChildren();
        if (children == null)
            throw new IllegalStateException("Children list is null");

        if (type == ComputationNodeType.NEGATE) {
            if (children.size() != 1)
                throw new IllegalArgumentException("Expected exactly one operand");
            double[][] mat = children.get(0).getMatrix();
            leftMatrix.loadRowMajor(mat);
            executor.submitAll(createNegateTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }

        if (type == ComputationNodeType.TRANSPOSE) {
            if (children.size() != 1)
                throw new IllegalArgumentException("Expected exactly one operand");
            double[][] mat = children.get(0).getMatrix();
            leftMatrix.loadRowMajor(mat);
            executor.submitAll(createTransposeTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }

        // --- כאן התיקון הגדול: תומך ב-n-ary בלי associativeNesting
        if (type == ComputationNodeType.ADD) {
            if (children.size() < 2)
                throw new IllegalArgumentException("ADD expects at least two operands");

            double[][] acc = children.get(0).getMatrix();
            for (int i = 1; i < children.size(); i++) {
                double[][] next = children.get(i).getMatrix();

                if (acc.length != next.length || (acc.length > 0 && next.length > 0 && acc[0].length != next[0].length))
                    throw new IllegalArgumentException("Matrix mismatch for ADD");

                leftMatrix.loadRowMajor(acc);
                rightMatrix.loadRowMajor(next);
                executor.submitAll(createAddTasks());
                acc = leftMatrix.readRowMajor();
            }

            node.resolve(acc);
            return;
        }

        if (type == ComputationNodeType.MULTIPLY) {
            if (children.size() < 2)
                throw new IllegalArgumentException("MULTIPLY expects at least two operands");

            double[][] acc = children.get(0).getMatrix();
            for (int i = 1; i < children.size(); i++) {
                double[][] next = children.get(i).getMatrix();

                if (acc.length > 0 && next.length > 0 && acc[0].length != next.length)
                    throw new IllegalArgumentException("Matrix mismatch for MULTIPLY");

                leftMatrix.loadRowMajor(acc);
                rightMatrix.loadColumnMajor(next);
                executor.submitAll(createMultiplyTasks());
                acc = leftMatrix.readRowMajor();
            }

            node.resolve(acc);
            return;
        }

        throw new IllegalArgumentException("Invalid node");
    }

    public List<Runnable> createAddTasks() {
        if (leftMatrix == null || rightMatrix == null)
            throw new NullPointerException("One of the matrix (or more) is null");
        List<Runnable> tasks = new ArrayList<>();
        int length = leftMatrix.length();
        for (int i = 0; i < length; i++) {
            final int rowIndex = i;
            Runnable task = () -> {
                SharedVector rowLeft = leftMatrix.get(rowIndex);
                SharedVector rowRight = rightMatrix.get(rowIndex);
                rowLeft.add(rowRight);
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        if (leftMatrix == null || rightMatrix == null)
            throw new NullPointerException("One of the matrix (or more) is null");
        List<Runnable> tasks = new ArrayList<>();
        int length = leftMatrix.length();
        for (int i = 0; i < length; i++) {
            final int rowIndex = i;
            Runnable task = () -> {
                SharedVector rowLeft = leftMatrix.get(rowIndex);
                rowLeft.vecMatMul(rightMatrix);
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        if (leftMatrix == null || rightMatrix == null)
            throw new NullPointerException("One of the matrix (or more) is null");
        List<Runnable> tasks = new ArrayList<>();
        int length = leftMatrix.length();
        for (int i = 0; i < length; i++) {
            final int rowIndex = i;
            Runnable task = () -> {
                SharedVector row = leftMatrix.get(rowIndex);
                row.negate();
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        if (leftMatrix == null || rightMatrix == null)
            throw new NullPointerException("One of the matrix (or more) is null");
        List<Runnable> tasks = new ArrayList<>();
        int length = leftMatrix.length();
        for (int i = 0; i < length; i++) {
            final int index = i;
            Runnable task = () -> {
                SharedVector vec = leftMatrix.get(index);
                vec.transpose();
            };
            tasks.add(task);
        }
        return tasks;
    }

    public String getWorkerReport() {
        if (executor == null)
            throw new IllegalStateException("Executor not initialized");
        return executor.getWorkerReport();
    }
}
