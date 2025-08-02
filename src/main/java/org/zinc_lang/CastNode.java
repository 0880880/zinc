package org.zinc_lang;

import static org.lwjgl.llvm.LLVMCore.*;

public class CastNode extends ASTNode {

    public TypeVal destType;
    public FactorNode val;

    @Override
    public String toString() {
        return String.format("(%s) %s", destType, val);
    }

    @Override
    public Val codegen(long builder, long context) {
        Val f = val.codegen(builder, context);

        if (destType.builtin && f.type.builtin) {
            Val v = new Val();
            v.type = destType;
            if (destType.isRealInteger() && f.isRealInteger()) {
                v.llvmValue = LLVMBuildIntCast(builder, f.llvmValue, destType.getLLVMType(), "IntIntCast");
                return v;
            } else if (destType.isRealFloat() && f.isRealInteger()) {
                if (f.isRealUnsigned()) {
                    v.llvmValue = LLVMBuildUIToFP(builder, f.llvmValue, destType.getLLVMType(), "UIntFloatCast");
                } else {
                    v.llvmValue = LLVMBuildSIToFP(builder, f.llvmValue, destType.getLLVMType(), "IntFloatCast");
                }
                return v;
            } else if (destType.isRealInteger() && f.isRealFloat()) {
                if (destType.isRealUnsigned()) {
                    v.llvmValue = LLVMBuildFPToUI(builder, f.llvmValue, destType.getLLVMType(), "FloatUIntCast");
                } else {
                    v.llvmValue = LLVMBuildFPToSI(builder, f.llvmValue, destType.getLLVMType(), "FloatIntCast");
                }
            } else if (destType.isRealFloat() && f.isRealFloat()) {
                v.llvmValue = LLVMBuildFPCast(builder, f.llvmValue, destType.getLLVMType(), "FloatFloatCast");
                return v;
            } else if (destType.isPointer() && f.isPointer()) {
                v.llvmValue = LLVMBuildBitCast(builder, f.llvmValue, destType.getLLVMType(), "PtrPtrCast");
                return v;
            } else {
                // TODO idk
            }
        } else {
            // TODO idk
            return null;
        }
        return null;
    }
}
