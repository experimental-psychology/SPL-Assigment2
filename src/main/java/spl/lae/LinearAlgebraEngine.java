package spl.lae;

import parser.*;
import memory.*;
import scheduling.*;

import java.util.ArrayList;
import java.util.List;
//@INV:leftMatrix and rightMatrix are valid SharedMatrix instances and not equals null
public class LinearAlgebraEngine {

    private SharedMatrix leftMatrix = new SharedMatrix();
    private SharedMatrix rightMatrix = new SharedMatrix();
    private TiredExecutor executor;
    //@PRE:numThreads>0
    //@POST:this.executor is initialized to a new TiredExecutor(numThreads)
    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        if(numThreads<=0)
            throw new IllegalArgumentException("numThreads must be positive");
        executor=new TiredExecutor(numThreads);
    }
    //@PRE:computationRoot!=null
    //@POST:returned node type is Matrix. Computatuion tree fully resolved
    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        if(computationRoot==null)
            throw new NullPointerException("computationRoot is null");
         try{
            while(computationRoot.getNodeType() != ComputationNodeType.MATRIX){
                ComputationNode resolve=computationRoot.findResolvable();
                if(resolve==null)
                    throw new IllegalStateException("Node not found");
                loadAndCompute(resolve);
            }
            return computationRoot;
        }finally{
            try{
                executor.shutdown();
            }catch (InterruptedException e){
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted during executor shutdown", e);
            }
        }
    }
    //@PRE:node!=null & node.getNodeType()!=null & node.getChildren()!=null
    //@POST:node resolved exactly once
    public void loadAndCompute(ComputationNode node) {
        // TODO: load operand matrices
        // TODO: create compute tasks & submit tasks to executor
        if(node==null)
            throw new NullPointerException("Node is null");
        ComputationNodeType type=node.getNodeType();
        if(type==null)
            throw new IllegalStateException("Node type is null");
        List<ComputationNode> children=node.getChildren();
        if(children==null)
            throw new IllegalStateException("Children list is null");
        if (type==ComputationNodeType.NEGATE){
            if(children.size()!=1)
                throw new IllegalArgumentException("Expected exactly one operand");
            double[][] mat = children.get(0).getMatrix();
            if(mat==null)
                throw new IllegalStateException("matrix is null");
            leftMatrix.loadRowMajor(mat);
            executor.submitAll(createNegateTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        if(type==ComputationNodeType.TRANSPOSE){
            if(children.size()!=1)
                throw new IllegalArgumentException("Expected exactly one operand");
            double[][] mat = children.get(0).getMatrix();
            if(mat==null)
                throw new IllegalStateException("matrix is null");
            leftMatrix.loadRowMajor(mat);
            double[][] original=leftMatrix.readRowMajor();
            int rows=original.length;
            int cols;
            if(rows==0)
                cols=0;
            else
                cols=original[0].length;
            double[][] transposed = new double[cols][rows];
            for(int i=0;i<rows;i++)
                for(int j=0; j<cols; j++)
                    transposed[j][i]=original[i][j];
            leftMatrix.loadRowMajor(transposed);
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        if(type==ComputationNodeType.ADD){
            if(children.size()<2)
                throw new IllegalArgumentException("ADD expects at least two operands");
            double[][] acc=children.get(0).getMatrix();
            if(acc==null)
                throw new NullPointerException("Cant use null");
            for(int i=1; i<children.size(); i++){
                double[][] next=children.get(i).getMatrix();
                if(next==null)
                    throw new NullPointerException("Cant use null");
                if(acc.length!=next.length||(acc.length>0&&next.length>0&&acc[0].length!=next[0].length))
                    throw new IllegalArgumentException("Matrix dimension mismatch for ADD");
                leftMatrix.loadRowMajor(acc);
                rightMatrix.loadRowMajor(next);
                executor.submitAll(createAddTasks());
                acc=leftMatrix.readRowMajor();
            }
            node.resolve(acc);
            return;
        }
        if(type==ComputationNodeType.MULTIPLY){
            if(children.size()<2)
                throw new IllegalArgumentException("MULTIPLY expects at least two operands");
            double[][] acc=children.get(0).getMatrix();
            if(acc==null)
                throw new NullPointerException("Cant use null");
            for(int i=1; i<children.size(); i++){
                double[][] next=children.get(i).getMatrix();
                if(next==null)
                    throw new NullPointerException("Cant use null");
                if(acc.length>0 && next.length>0 && acc[0].length!=next.length)
                    throw new IllegalArgumentException("Matrix dimension mismatch for MULTIPLY");
                leftMatrix.loadRowMajor(acc);
                rightMatrix.loadColumnMajor(next);
                executor.submitAll(createMultiplyTasks());
                acc=leftMatrix.readRowMajor();
            }
            node.resolve(acc);
            return;
        }
        throw new IllegalArgumentException("Unsupported node type");
    }
    //@PRE:leftMatrix!=null & rightMatrix!=null
    public List<Runnable> createAddTasks(){
        // TODO: return tasks that perform row-wise addition
        if(leftMatrix==null||rightMatrix==null)
            throw new NullPointerException("One of the matrix (or more) is null");
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
    //@PRE:leftMatrix != null & rightMatrix != null
    public List<Runnable> createMultiplyTasks() {
        // TODO: return tasks that perform row × matrix multiplication
        if(leftMatrix==null || rightMatrix==null)
            throw new NullPointerException("One of the matrix (or more) is null");
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
    //@PRE:leftMatrix!=null 
    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        if(leftMatrix==null)
            throw new NullPointerException("The matrix is null");
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
    //@PRE:leftMatrix!=null
    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        if(leftMatrix==null)
            throw new NullPointerException("The matrix is null");
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
    //@PRE:executor!=null
    //@POST:returns executor.getWorkerReport()
    public String getWorkerReport() {
        // TODO: return summary of worker activity
        if(executor==null)
            throw new IllegalStateException("Executor not initialized");
        return executor.getWorkerReport();
    }
}
