package org.zinc_lang;

import static org.lwjgl.llvm.LLVMCore.*;

public class ForStatement extends StatementNode {

    ASTNode init;
    ExprNode condition;
    ASTNode update;

    public StatementNode body;

    @Override
    public String toString() {
        return String.format("for (%s; %s; %s) %s", init, condition, update, body);
    }

    @Override
    public Val codegen(long builder, long context) {

        long preheaderBB = LLVMAppendBasicBlock(FuncNode.function, "preheader");
        long headerBB    = LLVMAppendBasicBlock(FuncNode.function, "loop.header");
        long bodyBB      = LLVMAppendBasicBlock(FuncNode.function, "loop.body");
        long latchBB     = LLVMAppendBasicBlock(FuncNode.function, "loop.latch");
        long exitBB      = LLVMAppendBasicBlock(FuncNode.function, "loop.exit");


// Go to preaheader
        LLVMBuildBr(builder, preheaderBB);

// 2) preheader: run init, then jump to header
        LLVMPositionBuilderAtEnd(builder, preheaderBB);
        init.codegen(builder, context);
        LLVMBuildBr(builder, headerBB);

// 3) header: evaluate condition, branch to body or exit
        LLVMPositionBuilderAtEnd(builder, headerBB);
        long condV = condition.codegen(builder, context).llvmValue;
        LLVMBuildCondBr(builder, condV, bodyBB, exitBB);

// 4) body: emit loop body, then jump to latch
        LLVMPositionBuilderAtEnd(builder, bodyBB);
        body.codegen(builder, context);
        LLVMBuildBr(builder, latchBB);

// 5) latch: run update, then go back to header
        LLVMPositionBuilderAtEnd(builder, latchBB);
        update.codegen(builder, context);
        LLVMBuildBr(builder, headerBB);

// 6) exit: resume after the loop
        LLVMPositionBuilderAtEnd(builder, exitBB);

        FuncNode.terminated = false;

        return null;
    }
}
