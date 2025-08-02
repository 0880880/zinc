package org.zinc_lang;

import static org.lwjgl.llvm.LLVMCore.*;

public class UnaryExpr extends ExprNode {

    public FactorNode factor;
    public String operator;
    public boolean postfix;

    // FIXME Add casting

    @Override
    public String toString() {
        if (postfix) {
            return factor.toString() + operator;
        }
        return operator + factor.toString();
    }

    @Override
    public Val codegen(long builder, long context) {

        Val ret = factor.codegen(builder, context);

        Val newVal = ret;
        boolean isFloat = ret.isRealFloat();
        boolean isUnsigned = ret.isRealUnsigned();
        boolean isInteger = ret.isRealInteger();
        if ("-".equals(operator)) {
            if (isFloat) {
                newVal = Val.of(ret, LLVMBuildFSub(builder, LLVMConstReal(ret.type.getLLVMType(), -0.0), ret.llvmValue, "NegateFloat"));
            } else if (isInteger) {
                newVal = Val.of(ret, LLVMBuildSub(builder, LLVMConstInt(ret.type.getLLVMType(), 0, !isUnsigned), ret.llvmValue, "NegateInt"));
            }
        } if ("++".equals(operator)) {
            if (isFloat) {
                newVal = Val.of(ret, LLVMBuildFAdd(builder, ret.llvmValue, LLVMConstReal(ret.type.getLLVMType(), 1), "IncrementFloat"));
            } else if (isInteger) {
                newVal = Val.of(ret, LLVMBuildAdd(builder, ret.llvmValue, LLVMConstInt(ret.type.getLLVMType(), 1, !isUnsigned), "IncrementInt"));
            }
        } else if ("--".equals(operator)) {
            if (isFloat) {
                newVal = Val.of(ret, LLVMBuildFSub(builder, ret.llvmValue, LLVMConstReal(ret.type.getLLVMType(), 1), "DecrementFloat"));
            } else if (isInteger) {
                newVal = Val.of(ret, LLVMBuildSub(builder, ret.llvmValue, LLVMConstInt(ret.type.getLLVMType(), 1, !isUnsigned), "DecrementInt"));
            }
        }

        if (!postfix) {
            ret = newVal;
        }

        return ret;

    }
}
