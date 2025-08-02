package org.zinc_lang;

public class Val {

    public TypeVal type;
    public long llvmValue;

    public static Val of(Val from, long value) {
        Val v = new Val();
        v.type = new TypeVal();
        v.type.llvmType = from.type.getLLVMType();
        v.type.baseType = from.type.getBaseType();
        v.type.builtin = from.type.builtin;
        v.type.pointerLevel = from.type.pointerLevel;
        v.llvmValue = value;
        return v;
    }

    public static Val external(long type, long value) {
        Val v = new Val();
        v.type = new TypeVal();
        v.type.llvmType = type;
        v.type.baseType = type;
        v.type.builtin = false;
        v.llvmValue = value;
        return v;
    }

    public static Val wild(long type, long value) {
        Val v = external(type, value);
        for (String k : Typing.primitives.keySet()) {
            long l = Typing.primitives.get(k);
            if (type == l) {
                v.type.builtin = true;
                break;
            }
        }
        return v;
    }

    public static Val full(TypeVal type, long value) {
        Val v = new Val();
        v.type = type;
        v.llvmValue = value;
        return v;
    }

    public static Val prim(long type, long value) {
        Val v = new Val();
        v.type = new TypeVal();
        v.type.llvmType = type;
        v.type.baseType = type;
        v.type.builtin = true;
        v.llvmValue = value;
        return v;
    }

    public static Val ptr(long baseType, long value, int level) {
        Val v = new Val();
        v.type = new TypeVal();
        v.type.llvmType = Typing.prim("ptr");
        v.type.baseType = baseType;
        v.type.pointerLevel = level;
        v.llvmValue = value;
        for (String k : Typing.primitives.keySet()) {
            long l = Typing.primitives.get(k);
            if (baseType == l) {
                v.type.builtin = true;
                break;
            }
        }
        return v;
    }

    public boolean isBaseFloat() {
        return type.isBaseFloat();
    }

    public boolean isBaseUnsigned() {
        return type.isBaseUnsigned();
    }

    public boolean isBaseInteger() {
        return type.isBaseInteger();
    }

    public boolean isRealFloat() {
        return type.isRealFloat();
    }

    public boolean isRealUnsigned() {
        return type.isRealUnsigned();
    }

    public boolean isRealInteger() {
        return type.isRealInteger();
    }

    public boolean isPointer() {
        return type.isPointer();
    }

}
