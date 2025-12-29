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
        // TODO: create executor with given thread count
        executor=new TiredExecutor(numThreads);
    }

    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        while(computationRoot.getNodeType()!=ComputationNodeType.MATRIX){
            ComputationNode resolved=computationRoot.findResolvable();
            if(resolved==null)
                throw new IllegalStateException("Not found");
            loadAndCompute(resolved);
        }
        return computationRoot;
    }

    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        ComputationNodeType type = node.getNodeType();
        List<ComputationNode> children = node.getChildren();
        if (type == ComputationNodeType.NEGATE){
            double[][] mat = children.get(0).getMatrix();
            leftMatrix.loadRowMajor(mat);
            executor.submitAll(createNegateTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        if(type == ComputationNodeType.TRANSPOSE){
            double[][] mat = children.get(0).getMatrix();
            leftMatrix.loadRowMajor(mat);
            executor.submitAll(createTransposeTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        if(type == ComputationNodeType.ADD){
            double[][] left = children.get(0).getMatrix();
            double[][] right = children.get(1).getMatrix();
            leftMatrix.loadRowMajor(left);
            rightMatrix.loadRowMajor(right);
            executor.submitAll(createAddTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        if (type == ComputationNodeType.MULTIPLY) {
            double[][] left = children.get(0).getMatrix();
            double[][] right = children.get(1).getMatrix();
            leftMatrix.loadRowMajor(left);
            rightMatrix.loadColumnMajor(right);
            executor.submitAll(createMultiplyTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        throw new IllegalArgumentException("Invalid node");
    }

    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        List<Runnable> tasks=new ArrayList<>();
        int length=leftMatrix.length();
        for(int i=0; i<length;i++){
            final int rowIndex=i;
            Runnable task=()->{
                SharedVector rowLeft=leftMatrix.get(rowIndex);
                SharedVector rowRight=rightMatrix.get(rowIndex);
                rowLeft.add(rowRight);
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        List<Runnable> tasks=new ArrayList<>();
        int length=leftMatrix.length();
        for(int i=0; i<length;i++){
            final int rowIndex=i;
            Runnable task=()->{
                SharedVector rowLeft=leftMatrix.get(rowIndex);
                rowLeft.vecMatMul(rightMatrix);
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        List<Runnable> tasks=new ArrayList<>();
        int length=leftMatrix.length();
        for(int i=0; i<length;i++){
            final int rowIndex=i;
            Runnable task=()->{
                SharedVector row=leftMatrix.get(rowIndex);
                row.negate();
            };
            tasks.add(task);
        }
        return tasks;
    }

    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        List<Runnable> tasks = new ArrayList<>();
        int length = leftMatrix.length();
        for(int i=0; i<length; i++){
            final int index = i;
            Runnable task=()->{
                SharedVector vec = leftMatrix.get(index);
                vec.transpose();
            };
            tasks.add(task);
        }
        return tasks;
    }

    public String getWorkerReport() {
        // TODO: return summary of worker activity
        return executor.getWorkerReport();
    }
}
