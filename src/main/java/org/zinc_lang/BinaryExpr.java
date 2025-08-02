package org.zinc_lang;

import org.lwjgl.system.MemoryStack;

import static org.lwjgl.llvm.LLVMCore.*;

public class BinaryExpr extends ExprNode {

    public ASTNode left;
    public String op;
    public BinaryTerm right;

    @Override
    public String toString() {
        if (op == null) {
            return left.toString();
        }
        return String.format("%s %s %s", left, op, right);
    }

    @Override
    public Val codegen(long builder, long context) {

        Val leftCode = left.codegen(builder, context);
        if (right == null || op == null) {
            return leftCode;
        }
        Val rightCode = right.codegen(builder, context);
        Typing.BinaryCast t = Typing.upgrade(builder, leftCode, rightCode);
        boolean isFloat = leftCode.isRealFloat();
        boolean isInteger = leftCode.isRealInteger();
        if (t == null) {
            return null;
        }
        if ("+".equals(op)) {
            if (isFloat) {
                return Val.wild(t.llvmType(), LLVMBuildFAdd(builder, leftCode.llvmValue, rightCode.llvmValue, "FloatAdd"));
            } else if (isInteger) {
                return Val.wild(t.llvmType(), LLVMBuildAdd(builder, leftCode.llvmValue, rightCode.llvmValue, "IntAdd"));
            } else {
                // TODO Operator Overload
            }
        } else if ("-".equals(op)) {
            if (isFloat) {
                return Val.wild(t.llvmType(), LLVMBuildFSub(builder, leftCode.llvmValue, rightCode.llvmValue, "FloatSub"));
            } else if (isInteger) {
                return Val.wild(t.llvmType(), LLVMBuildSub(builder, leftCode.llvmValue, rightCode.llvmValue, "IntSub"));
            } else {
                // TODO Operator Overload
            }
        } else if ("&".equals(op)) {
            return Val.wild(t.llvmType(), LLVMBuildAnd(builder, leftCode.llvmValue, rightCode.llvmValue, "BitwiseAnd"));
        } else if ("|".equals(op)) {
            return Val.wild(t.llvmType(), LLVMBuildOr(builder, leftCode.llvmValue, rightCode.llvmValue, "BitwiseOr"));
        } else if ("||".equals(op)) {
            if (t.llvmType() == Typing.prim("bool")) {
                long trueBB  = LLVMAppendBasicBlock(FuncNode.function, "or.true");
                long evalBB  = LLVMAppendBasicBlock(FuncNode.function, "or.eval");
                long mergeBB = LLVMAppendBasicBlock(FuncNode.function, "or.merge");

                LLVMBuildCondBr(builder, leftCode.llvmValue, trueBB, evalBB);

                LLVMPositionBuilderAtEnd(builder, trueBB);
                long trueVal = LLVMConstInt(Typing.prim("bool"), 1, false);
                LLVMBuildBr(builder, mergeBB);

                LLVMPositionBuilderAtEnd(builder, evalBB);
                long rhsVal = rightCode.llvmValue;      // generate full RHS here if needed
                LLVMBuildBr(builder, mergeBB);

                LLVMPositionBuilderAtEnd(builder, mergeBB);
                long phi = LLVMBuildPhi(builder, Typing.prim("bool"), "ortmp");

                MemoryStack stack = MemoryStack.stackGet();

                LLVMAddIncoming(phi,
                        stack.pointers(trueVal),
                        stack.pointers(trueBB));

                LLVMAddIncoming(phi,
                        stack.pointers(rhsVal),
                        stack.pointers(evalBB));


                return Val.wild(t.llvmType(), phi);
            } else {
                // TODO error
            }
        }
        System.err.println("Err binary expr");
        return null;
    }

}
