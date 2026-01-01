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
   if (computationRoot == null)
        throw new NullPointerException("computationRoot is null");

    // Normalize: convert n-ary +/* into left-associative binary trees everywhere
    List<ComputationNode> stack = new ArrayList<>();
    stack.add(computationRoot);
    while (!stack.isEmpty()) {
        ComputationNode cur = stack.remove(stack.size() - 1);
        if (cur.getNodeType() != ComputationNodeType.MATRIX) {
            cur.associativeNesting();
            List<ComputationNode> kids = cur.getChildren();
            if (kids != null) {
                for (ComputationNode k : kids) stack.add(k);
            }
        }
    }

    while (computationRoot.getNodeType() != ComputationNodeType.MATRIX) {
        ComputationNode resolved = computationRoot.findResolvable();
        if (resolved == null)
            throw new IllegalStateException("No resolvable node found");
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
        if (mat == null)
            throw new IllegalStateException("matrix is null");

        leftMatrix.loadRowMajor(mat);
        executor.submitAll(createNegateTasks());
        node.resolve(leftMatrix.readRowMajor());
        return;
    }

     if (type == ComputationNodeType.TRANSPOSE) {
     if (children.size() != 1)
        throw new IllegalArgumentException("Illegal operation: wrong number of operands");

     double[][] a = children.get(0).getMatrix();
     if (a == null)
        throw new IllegalStateException("matrix is null");

      leftMatrix.loadRowMajor(a);                 
     executor.submitAll(createTransposeTasks()); 
     node.resolve(leftMatrix.readRowMajor());    
     return;
    }

    if (type == ComputationNodeType.ADD) {
         if (children.size() < 2)
            throw new IllegalArgumentException("ADD expects at least two operands");
        if(children.size()!=2)
            throw new IllegalArgumentException("Illegal operation: wrong number of operands");
        double[][] acc = children.get(0).getMatrix();
         if (acc == null)
            throw new IllegalStateException("Operand matrix is null");

       for (int r = 0; r < acc.length; r++) {
             if (acc[r] == null)   
                throw new IllegalArgumentException("Matrix row is null");
        }


        for (int i = 1; i < children.size(); i++) {
            double[][] right = children.get(i).getMatrix();
            if (right == null)
                throw new IllegalStateException("Operand matrix is null");

            if (acc.length != right.length)
                throw new IllegalArgumentException("Matrix mismatch for ADD");

            int accCols = (acc.length == 0 ? 0 : acc[0].length);
            for (int rr = 0; rr < acc.length; rr++) {
                if (acc[rr] == null)
                    throw new IllegalArgumentException("Matrix row is null");
                if (acc[rr].length != accCols)
                    throw new IllegalArgumentException("Matrix must be rectangular");
            }
            int rightCols = (right.length == 0 ? 0 : right[0].length);
             for (int rr = 0; rr < right.length; rr++) {
                if (right[rr] == null)
                    throw new IllegalArgumentException("Matrix row is null");
                if (right[rr].length != rightCols)
                    throw new IllegalArgumentException("Matrix must be rectangular");
            }

            if (accCols != rightCols)
                throw new IllegalArgumentException("Matrix mismatch for ADD");

            leftMatrix.loadRowMajor(acc);
            rightMatrix.loadRowMajor(right);
            executor.submitAll(createAddTasks());
            acc = leftMatrix.readRowMajor();
        }

        node.resolve(acc);
        return;
    }

    if (type == ComputationNodeType.MULTIPLY) {
        if (children.size() < 2)
            throw new IllegalArgumentException("MULTIPLY expects at least two operands");
        if(children.size()!=2)
            throw new IllegalArgumentException("Illegal operation: wrong number of operands");

        double[][] acc = children.get(0).getMatrix();
        if (acc == null)
            throw new IllegalStateException("Operand matrix is null");

        for (int i = 1; i < children.size(); i++) {
            double[][] right = children.get(i).getMatrix();
            if (right == null)
                throw new IllegalStateException("Operand matrix is null");

            int accRows = acc.length;
            int accCols = (accRows == 0 ? 0 : acc[0].length);
            for (int rr = 0; rr < accRows; rr++) {
                if (acc[rr] == null)
                    throw new IllegalArgumentException("Matrix row is null");
                if (acc[rr].length != accCols)
                    throw new IllegalArgumentException("Matrix must be rectangular");
            }

            int rightRows = right.length;
            int rightCols = (rightRows == 0 ? 0 : right[0].length);
            for (int rr = 0; rr < rightRows; rr++) {
                if (right[rr] == null)
                    throw new IllegalArgumentException("Matrix row is null");
                if (right[rr].length != rightCols)
                    throw new IllegalArgumentException("Matrix must be rectangular");
            }

            if (accCols != rightRows)
                throw new IllegalArgumentException("Matrix mismatch for MULTIPLY");
            leftMatrix.loadRowMajor(acc);
            rightMatrix.loadColumnMajor(right);
            executor.submitAll(createMultiplyTasks());
            acc = leftMatrix.readRowMajor();
        }
        node.resolve(acc);
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
