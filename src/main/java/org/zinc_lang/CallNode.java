package org.zinc_lang;

import com.github.zeroeighteightzero.lwlp.Token;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class CallNode extends ExprNode {

    public Token name;
    public List<ExprNode> args;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(name.value);
        sb.append('(');
        for (int i = 0; i < args.size(); i++) {
            sb.append(args.get(i));
            if (i < args.size() - 1) {
                sb.append(", ");
            }
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public Val codegen(long builder, long context) {
        Scope.Def def = Scope.get(name.value);
        System.out.println(def);
        if (def == null) {
            // TODO error
            return null;
        } else if (!(def instanceof Scope.FuncDef)) {
            // TODO error
            return null;
        }
        Scope.FuncDef f = (Scope.FuncDef) def;
        MemoryStack stack = MemoryStack.stackGet();
        int stackPointer = stack.getPointer();
        try {
            stack.nUTF8("", true);
            long NameEncoded = stack.getPointerAddress();
            if (args.isEmpty()) {
                    return Val.full(f.retType, nLLVMBuildCall2(builder, f.type, f.value, NULL, 0, NameEncoded));
            } else {
                PointerBuffer buf = stack.mallocPointer(args.size());
                for (int i = 0; i < args.size(); i++) {
                    buf.put(i, args.get(i).codegen(builder, context).llvmValue);// TODO Check&Upgrade Type
                }
                return Val.full(f.retType, nLLVMBuildCall2(builder, f.type, f.value, MemoryUtil.memAddress(buf), args.size(), NameEncoded));
            }
        } finally {
            stack.setPointer(stackPointer);
        }
    }
}
