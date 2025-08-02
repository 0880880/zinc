package org.zinc_lang;

import org.lwjgl.system.MemoryStack;

import static org.lwjgl.llvm.LLVMCore.*;

public class BinaryTerm extends ASTNode {

    public ASTNode left;
    public String op;
    public FactorNode right;

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
        boolean isUnsigned = leftCode.isRealUnsigned();
        boolean isInteger = leftCode.isRealInteger();
        if (t == null) {
            return null;
        }
        if ("*".equals(op)) {
            if (isFloat) {
                return Val.wild(t.llvmType(), LLVMBuildFMul(builder, leftCode.llvmValue, rightCode.llvmValue, "FloatMul"));
            } else if (isInteger) {
                return Val.wild(t.llvmType(), LLVMBuildMul(builder, leftCode.llvmValue, rightCode.llvmValue, "IntMul"));
            } else {
                // TODO Operator Overload
            }
        } else if ("/".equals(op)) {
            if (isFloat) {
                return Val.wild(t.llvmType(), LLVMBuildFDiv(builder, leftCode.llvmValue, rightCode.llvmValue, "FloatDiv"));
            } else if (isInteger) {
                return Val.wild(
                        t.llvmType(),
                        isUnsigned ? LLVMBuildUDiv(builder, leftCode.llvmValue, rightCode.llvmValue, "UIntDiv") :
                                LLVMBuildSDiv(builder, leftCode.llvmValue, rightCode.llvmValue, "IntDiv")
                        );
            } else {
                // TODO Operator Overload
            }
        } else if ("&&".equals(op)) {
            if (t.llvmType() == Typing.prim("bool")) {
                long thenBB = LLVMAppendBasicBlock(FuncNode.function, "then");
                long elseBB = LLVMAppendBasicBlock(FuncNode.function, "else");
                long mergeBB = LLVMAppendBasicBlock(FuncNode.function, "merge");

                LLVMBuildCondBr(builder, leftCode.llvmValue, thenBB, elseBB);

                LLVMPositionBuilderAtEnd(builder, thenBB);
                long rhsVal = rightCode.llvmValue; // if rhs is a more complex expression, generate here
                LLVMBuildBr(builder, mergeBB);

                LLVMPositionBuilderAtEnd(builder, elseBB);
                long falseVal = LLVMConstInt(Typing.prim("bool"), 0, false);
                LLVMBuildBr(builder, mergeBB);

// Merge block
                LLVMPositionBuilderAtEnd(builder, mergeBB);
                long phi = LLVMBuildPhi(builder, Typing.prim("bool"), "andtmp");

                MemoryStack stack = MemoryStack.stackGet();

                LLVMAddIncoming(phi, stack.pointers(rhsVal), stack.pointers(thenBB));
                LLVMAddIncoming(phi, stack.pointers(falseVal), stack.pointers(elseBB));

                return Val.wild(t.llvmType(), phi);
            } else {
                // TODO error
            }
        }
        return null;
    }
}
