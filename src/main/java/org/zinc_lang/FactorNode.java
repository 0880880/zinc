package org.zinc_lang;

import com.github.tommyettinger.ds.ObjectLongMap;
import com.github.zeroeighteightzero.lwlp.Token;

import static org.lwjgl.llvm.LLVMCore.*;

public class FactorNode extends ASTNode {

    public enum FactorType {
        EXPR,
        LITERAL,
        UNARY,
        CAST
    }

    public FactorType factorType;
    public UnaryExpr unary;
    public CastNode cast;
    public Token literal;
    public ExprNode expr;

    private static final ObjectLongMap<String> stringCache = new ObjectLongMap<>();

    @Override
    public String toString() {
        return switch (factorType) {
            case UNARY -> unary.toString();
            case LITERAL -> literal.value;
            case EXPR -> '(' + expr.toString() + ')';
            case CAST -> cast.toString();
            case null -> "NaN";
        };
    }

    @Override
    public Val codegen(long builder, long context) {
        if (factorType == FactorType.EXPR) {
            return expr.codegen(builder, context);
        } else if (factorType == FactorType.UNARY) {
            return unary.codegen(builder, context);
        } else if (factorType == FactorType.CAST) {
            return cast.codegen(builder, context);
        } else if (factorType == FactorType.LITERAL) {
            if ("INTEGER".equals(literal.type)) {
                return Typing.createInt(literal.value);
            } else if ("DECIMAL".equals(literal.type)) {
                return Typing.createFloat(Double.parseDouble(literal.value));
            } else if ("IDENTIFIER".equals(literal.type)) {
                Scope.Def def = Scope.get(literal.value);
                if (def == null) {
                    Scope.FuncDef func = Scope.getFunction();
                    for (int i = 0; i < func.args; i++) {
                        if (func.argNames.get(i).equals(literal.value)) {
                            return Val.wild(func.argTypes.get(i).getLLVMType(), LLVMGetParam(func.value, i));
                        }
                    }
                }
                return Val.wild(def.type, LLVMBuildLoad2(builder, def.type, def.value, "loadtmp"));
            } else if ("BOOLEAN".equals(literal.type)) {
                return Val.prim(Typing.prim("bool"),
                        literal.value.equals("true") ? LLVMConstInt(Typing.prim("bool"), 1, false) : LLVMConstInt(Typing.prim("bool"), 0, false));
            } else if ("NULL".equals(literal.type)) {
                long ty = LLVMPointerType(Typing.prim("void"), 0);
                return Val.wild(ty, LLVMConstNull(ty));
            } else if ("STRING".equals(literal.type)) {
                String realStr = literal.value.substring(1,literal.value.length()-1);
                long str = stringCache.getOrDefault(realStr, 0);
                if (str == 0) {
                    str = LLVMBuildGlobalStringPtr(builder, realStr, literal.value.substring(0,Math.min(5,literal.value.length())));
                    stringCache.put(realStr, str);
                }
                return Val.ptr(Typing.prim("i8"), str, 1);
            }
        }
        System.err.println("Error invalid factor. Must investigate.");
        return null;
    }
}
