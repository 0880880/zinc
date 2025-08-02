package org.zinc_lang;

import com.github.zeroeighteightzero.lwlp.Token;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

import static org.lwjgl.llvm.LLVMCore.*;

public class FuncNode extends StatementNode {

    public enum Visibility {
        PUBLIC,
        PRIVATE,
        PACKAGE
    }

    public Token name;
    public List<TypeVal> argTypes;
    public List<Token> argNames;
    public int args;
    public Visibility visibility;

    public StatementNode body;

    public TypeVal retType;

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (visibility != Visibility.PACKAGE) {
            sb.append(visibility.name().toLowerCase());
            sb.append(' ');
        }
        sb.append("func ");
        sb.append(name.value);
        sb.append('(');
        for (int i = 0; i < args; i++) {
            sb.append(argTypes.get(i));
            sb.append(' ');
            sb.append(argNames.get(i).value);
            if (i < args - 1) {
                sb.append(", ");
            }
        }
        sb.append(") ");
        if (retType != null) {
            sb.append(retType);
            if (body != null) sb.append(' ');
        }
        if (body == null) {
            sb.append(';');
        } else {
            sb.append(body);
        }
        return sb.toString();
    }

    public static long currentRetType;
    public static long function;
    public static boolean terminated; // Anything that moves the builder must set this to false

    @Override
    public Val codegen(long builder, long context) {

        PointerBuffer llvmTypes = MemoryUtil.memAllocPointer(args);

        for (int i = 0; i < args; i++) {
            llvmTypes.put(i, argTypes.get(i).getLLVMType());
        }

        currentRetType = retType == null ? Typing.prim("void") : retType.getLLVMType();
        terminated = false;

        long funcType = LLVMFunctionType(currentRetType, llvmTypes, false);

        long func = LLVMAddFunction(Frontend.module().llvmModule, name.value, funcType);
        LLVMSetFunctionCallConv(func, LLVMCCallConv);
        function = func;

        Scope.func(name.value, funcType, func, this);

        if (body != null) {
            long entry = LLVMAppendBasicBlockInContext(context, func, "entry");
            LLVMPositionBuilderAtEnd(builder, entry);

            Scope.begin();

            body.codegen(builder, context);

            if (!terminated) {
                if (retType == null) {
                    LLVMBuildRetVoid(builder);
                } else {
                    // TODO error
                }
            }

            Scope.end();
        }

        return Val.external(funcType, func);

    }
}
