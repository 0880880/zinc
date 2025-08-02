package org.zinc_lang;

import com.github.zeroeighteightzero.lwlp.Token;

import static org.lwjgl.llvm.LLVMCore.LLVMBuildAlloca;
import static org.lwjgl.llvm.LLVMCore.LLVMBuildStore;

public class DeclareNode extends StatementNode {

    public TypeVal type;
    public Token identifier;
    public ExprNode value;

    @Override
    public String toString() {
        return String.format("%s %s = %s;", type, identifier.value, value);
    }

    @Override
    public Val codegen(long builder, long context) {
        Val v = value.codegen(builder, context);

        Typing.BinaryCast t = Typing.upgradeTo(builder, v, type.getBaseType());

        if (t == null) {
            // TODO error
            return null;
        }

        if (Scope.get(identifier.value) != null) {
            // TODO error
            return null;
        }

        long variable = LLVMBuildAlloca(builder, t.llvmType(), "var_" + identifier.value);
        LLVMBuildStore(builder, v.llvmValue, variable);

        System.out.println("                   >> > > >>   DECLARED    " + variable + "   " + t.llvmType());

        Scope.var(identifier.value, t.llvmType(), variable);

        return v;
    }
}
