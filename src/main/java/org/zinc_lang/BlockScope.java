package org.zinc_lang;

import java.util.List;

import static org.lwjgl.llvm.LLVMCore.*;

public class BlockScope extends StatementNode {

    List<StatementNode> nodes;

    @Override
    public String toString() {
        if (nodes == null || nodes.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (StatementNode node : nodes) {
            sb.append(node.toString());
            sb.append('\n');
        }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public Val codegen(long builder, long context) {
        Scope.begin();
        for (StatementNode node : nodes) {
            node.codegen(builder, context);
        }
        Scope.end();
        return null;
    }

    public long block(long builder, long context) {
        long entry = LLVMCreateBasicBlockInContext(context, "entry");
        LLVMPositionBuilderAtEnd(builder, entry);

        Scope.begin();
        for (StatementNode node : nodes) {
            node.codegen(builder, context);
        }
        Scope.end();

        return entry;
    }

    public long appendBlock(long builder, long context, long v, String name) {
        long b = LLVMAppendBasicBlockInContext(context, v, name);
        LLVMPositionBuilderAtEnd(builder, b);

        Scope.begin();
        for (StatementNode node : nodes) {
            node.codegen(builder, context);
        }
        Scope.end();

        return b;
    }

}
