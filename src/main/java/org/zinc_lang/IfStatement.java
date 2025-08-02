package org.zinc_lang;

import static org.lwjgl.llvm.LLVMCore.*;

public class IfStatement extends StatementNode {

    public ExprNode condition;

    public StatementNode ifBody;
    public ExprNode[] elseIfConditions;
    public StatementNode[] elseIfBodies;
    public StatementNode elseBody;

    @Override
    public String toString() {
        return String.format("if (%s) %s", condition, ifBody);
    }

    @Override
    public Val codegen(long builder, long context) {

        long ifBB = LLVMAppendBasicBlock(FuncNode.function, "ifBB");
        int numElseIf = elseIfConditions.length;
        long[] elseIfs = new long[numElseIf];
        long[] elseIfTests = new long[numElseIf];
        for (int i = 0; i < numElseIf; i++) {
            elseIfs[i] = LLVMAppendBasicBlock(FuncNode.function, "elseIfBB" + i);
            elseIfTests[i] = LLVMAppendBasicBlock(FuncNode.function, "elseIfTestBB" + i);
        }
        long elseBB = 0;
        if (elseBody != null) {
            elseBB = LLVMAppendBasicBlock(FuncNode.function, "elseBB");
        }
        long mergeBB = LLVMAppendBasicBlock(FuncNode.function, "mergeBB");

        Val condCode = condition.codegen(builder, context);

        long next = numElseIf > 0 ? elseIfTests[0] : (elseBody == null ? mergeBB : elseBB);

        LLVMBuildCondBr(builder, condCode.llvmValue, ifBB, next);
        LLVMPositionBuilderAtEnd(builder, ifBB);
        ifBody.codegen(builder, context);
        LLVMBuildBr(builder, mergeBB);

        for (int i = 0; i < numElseIf; i++) {
            LLVMPositionBuilderAtEnd(builder, elseIfTests[i]);
            condCode = elseIfConditions[i].codegen(builder, context);
            LLVMBuildCondBr(builder, condCode.llvmValue, elseIfs[i], i + 1 == numElseIf ? (elseBody == null ? mergeBB : elseBB) : elseIfTests[i + 1]);
            LLVMPositionBuilderAtEnd(builder, elseIfs[i]);
            elseIfBodies[i].codegen(builder, context);
            LLVMBuildBr(builder, mergeBB);
        }

        if (elseBody != null) {
            LLVMPositionBuilderAtEnd(builder, elseBB);
            elseBody.codegen(builder, context);
            LLVMBuildBr(builder, mergeBB);
        }

        LLVMPositionBuilderAtEnd(builder, mergeBB);

        FuncNode.terminated = false;

        return null;

    }
}
