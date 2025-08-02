package org.zinc_lang;

import static org.lwjgl.llvm.LLVMCore.*;

public class ReturnNode extends StatementNode {

     public ExprNode value;

    @Override
    public String toString() {
        if (value == null) return "return;";
        return String.format("return %s;", value);
    }

    @Override
    public Val codegen(long builder, long context) {
        if (value == null) {
            if (FuncNode.currentRetType != Typing.prim("void")) {
                // TODO error
                return null;
            }
            FuncNode.terminated = true;
            return Val.wild(0, LLVMBuildRetVoid(builder));
        } else {
            Val retVal = value.codegen(builder, context);
            Typing.BinaryCast t = Typing.upgradeTo(builder, retVal, FuncNode.currentRetType);
            if (t.llvmType() != FuncNode.currentRetType) {
                // TODO error
                return null;
            }
            FuncNode.terminated = true;
            return Val.wild(0, LLVMBuildRet(builder, retVal.llvmValue));
        }
    }
}
