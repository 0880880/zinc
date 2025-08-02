package org.zinc_lang;

import static org.lwjgl.llvm.LLVMCore.*;

public class CompareExpr extends ExprNode {

    ExprNode left;
    String op;
    ExprNode right;

    @Override
    public String toString() {
        return String.format("%s %s %s", left, op, right);
    }

    @Override
    public Val codegen(long builder, long context) {

        Val leftCode = left.codegen(builder, context);

        Val rightCode = right.codegen(builder, context);
        Typing.BinaryCast t = Typing.upgrade(builder, leftCode, rightCode);
        boolean isFloat = leftCode.isRealFloat();
        boolean isInteger = leftCode.isRealInteger();
        boolean isUnsigned = leftCode.isRealUnsigned();

        int icmp = 0;
        int fcmp = switch (op) {
            case "==" -> {
                icmp = LLVMIntEQ;
                yield LLVMRealOEQ;
            }
            case "!=" -> {
                icmp = LLVMIntNE;
                yield LLVMRealONE;
            }
            case ">" -> {
                icmp = isUnsigned ? LLVMIntUGT : LLVMIntSGT;
                yield LLVMRealOGT;
            }
            case "<" -> {
                icmp = isUnsigned ? LLVMIntULT : LLVMIntSLT;
                yield LLVMRealOLT;
            }
            case ">=" -> {
                icmp = isUnsigned ? LLVMIntUGE : LLVMIntSGE;
                yield LLVMRealOGE;
            }
            case "<=" -> {
                icmp = isUnsigned ? LLVMIntULE : LLVMIntSLE;
                yield LLVMRealOLE;
            }
            default -> 0;
        };

        if (isInteger) {
            return Val.of(leftCode, LLVMBuildICmp(builder, icmp, leftCode.llvmValue, rightCode.llvmValue, "ComparisonInt"));
        } else if (isFloat) {
            return Val.of(leftCode, LLVMBuildFCmp(builder, fcmp, leftCode.llvmValue, rightCode.llvmValue, "ComparisonFloat"));
        } else {
            // TODO operator overload
            if (op.equals("==")) {
                return Val.of(leftCode, LLVMBuildICmp(builder, LLVMIntEQ, leftCode.llvmValue, rightCode.llvmValue, "ComparePtrEQ"));
            } else if (op.equals("!=")) {
                return Val.of(leftCode, LLVMBuildICmp(builder, LLVMIntNE, leftCode.llvmValue, rightCode.llvmValue, "ComparePtrNEQ"));
            }
        }
        // TODO error
        return null;
    }
}
