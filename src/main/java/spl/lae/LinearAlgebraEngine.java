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
    //@POST:executor!=null
    //@POST:this.executor is initialized to a new TiredExecutor(numThreads)
    public LinearAlgebraEngine(int numThreads) {
        // TODO: create executor with given thread count
        if(numThreads<=0)
            throw new IllegalArgumentException("numThreads must be positive");
        executor=new TiredExecutor(numThreads);
    }
    //@PRE:computationRoot!=null
    //@POST:r.getNodeType()==MATRIX
    //@POST:The computation tree rooted at r is fully resolved (no pending operations)
    //@POST:For every node that was resolved during execution, node.resolve(resultMatrix) was called exactly once
    public ComputationNode run(ComputationNode computationRoot) {
        // TODO: resolve computation tree step by step until final matrix is produced
        if(computationRoot==null)
            throw new NullPointerException("computationRoot is null");
        while(computationRoot.getNodeType()!=ComputationNodeType.MATRIX){
            ComputationNode resolved=computationRoot.findResolvable();
            if(resolved==null)
                throw new IllegalStateException("Not found");
            loadAndCompute(resolved);
        }
        return computationRoot;
    }
    //@PRE:node != null & node.getNodeType()!=null & node.getChildren()!=null
    //@PRE:If node.getNodeType()==NEGATE or TRANSPOSE: node.getChildren().size()==1
    //@PRE:If node.getNodeType()==ADD or MULTIPLY: node.getChildren().size()==2
    //@PRE:node.getChildren().get(0).getMatrix()!=null
    //@PRE:If type==ADD: left and right matrices have equal dimensions
    //@PRE:If type==MULTIPLY: left[0].length==right.length
    //@POST:For NEGATE: result is the element-wise negation of the input matrix
    //@POST:For TRANSPOSE: result equals the input matrix with vector orientations toggled as specified by SharedVector.transpose()
    //@POST:For ADD: result is element-wise sum of the two operand matrices
    //@POST:For MULTIPLY: result is row-vector × matrix multiplication (A×B) with compatible dimensions
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
        if(type == ComputationNodeType.TRANSPOSE){
            if(children.size()!=1)
                throw new IllegalArgumentException("Expected exactly one operand");
            double[][] mat = children.get(0).getMatrix();
            if(mat==null)
                throw new IllegalStateException("matrix is null");
            leftMatrix.loadRowMajor(mat);
            executor.submitAll(createTransposeTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        if(type == ComputationNodeType.ADD){
            if(children.size()!=2)
                throw new IllegalArgumentException("ADD expects two operands");
            double[][] left = children.get(0).getMatrix();
            double[][] right = children.get(1).getMatrix();
            if(left==null||right==null)
                throw new IllegalStateException("One or more matrix is null");
            if(left.length!=right.length || left[0].length!=right[0].length)
                throw new IllegalArgumentException("Matrix dismatch for ADD");
            leftMatrix.loadRowMajor(left);
            rightMatrix.loadRowMajor(right);
            executor.submitAll(createAddTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        if (type == ComputationNodeType.MULTIPLY) {
            if(children.size()!=2)
                throw new IllegalArgumentException("MULTIPLY expects two operands");
            double[][] left = children.get(0).getMatrix();
            double[][] right = children.get(1).getMatrix();
            if(left==null||right==null)
                throw new IllegalStateException("One or more matrix is null");
            if(left[0].length!=right.length)
                throw new IllegalArgumentException("Matrix dismatch for MULTIPLY");
            leftMatrix.loadRowMajor(left);
            rightMatrix.loadColumnMajor(right);
            executor.submitAll(createMultiplyTasks());
            node.resolve(leftMatrix.readRowMajor());
            return;
        }
        throw new IllegalArgumentException("Invalid node");
    }
    //@PRE:leftMatrix != null & rightMatrix != null & 
    //@POST:returns a mutable list tasks of size leftMatrix.length()
    public List<Runnable> createAddTasks() {
        // TODO: return tasks that perform row-wise addition
        if(leftMatrix==null || rightMatrix==null)
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
    //@PRE:leftMatrix != null & rightMatrix != null & 
    //@POST: returns a mutable list tasks of size leftMatrix.length()
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
    //@PRE:leftMatrix != null & rightMatrix != null & 
    //@POST: returns a mutable list tasks of size leftMatrix.length()
    public List<Runnable> createNegateTasks() {
        // TODO: return tasks that negate rows
        if(leftMatrix==null || rightMatrix==null)
            throw new NullPointerException("One of the matrix (or more) is null");
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
    //@PRE:leftMatrix != null
    //@POST: returns a mutable list tasks of size leftMatrix.length()
    public List<Runnable> createTransposeTasks() {
        // TODO: return tasks that transpose rows
        if(leftMatrix==null || rightMatrix==null)
            throw new NullPointerException("One of the matrix (or more) is null");
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
