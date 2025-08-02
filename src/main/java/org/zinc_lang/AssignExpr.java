package org.zinc_lang;

import com.github.zeroeighteightzero.lwlp.Token;

import static org.lwjgl.llvm.LLVMCore.LLVMBuildAlloca;
import static org.lwjgl.llvm.LLVMCore.LLVMBuildStore;

public class AssignExpr extends ExprNode {

    public Token identifier;
    public ExprNode value;

    @Override
    public String toString() {
        return String.format("%s = %s", identifier.value, value);
    }

    @Override
    public Val codegen(long builder, long context) {
        Val v = value.codegen(builder, context);

        Scope.Def def = Scope.get(identifier.value);

        if (def == null) {
            // TODO error
            return null;
        }

        if (!(def instanceof Scope.VariableDef)) {
            // TODO error
            return null;
        }
        Scope.VariableDef var = (Scope.VariableDef) def;

        long destType = var.type;

        Typing.BinaryCast t = Typing.upgradeTo(builder, v, destType);

        if (t == null) {
            // TODO error
            return null;
        }

        LLVMBuildStore(builder, v.llvmValue, var.value);

        return v;
    }
}
