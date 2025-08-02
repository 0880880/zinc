package org.zinc_lang;

public class TypeVal {

    public String type;
    public long llvmType = 0;
    public long baseType = 0;
    public int pointerLevel;
    public boolean builtin;

    public long getBaseType() {
        if (baseType != 0)
            return baseType;
        String ttype = type;
        if (type.equals("int")) ttype = Typing.INT_TYPE;
        else if (type.equals("float")) ttype = Typing.FLOAT_TYPE;
        for (Typing.PrimitiveType p : Typing.primitiveTypes) {
            if (p.name.equals(ttype)) {
                baseType = p.llvmType;
                return p.llvmType;
            }
        }
        return 0; // TODO FIXME Structs/Classes other stuff...
    }

    public long getLLVMType() {
        if (llvmType != 0)
            return llvmType;
        if (pointerLevel > 0) {
            llvmType = Typing.prim("ptr");
            return llvmType;
        }
        llvmType = getBaseType();
        return llvmType;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Typing.primitiveTypes.size(); i++) {
            Typing.PrimitiveType p = Typing.primitiveTypes.get(i);
            if (p.llvmType == getBaseType()) {
                sb.append(p.name);
                break;
            }
        }
        for (int i = 0; i < pointerLevel; i++) {
            sb.append('*');
        }
        /*for (int i = 0; i < dimensions; i++) {
            sb.append('[');
            if (arraySizes[i] != -1) {
                sb.append(arraySizes[i]);
            }
                    sb.append(']');
        }*/
        return sb.toString();
    }

    public static TypeVal fake(String type, int pointers, boolean primitive) {
        TypeVal t = new TypeVal();
        t.type = type;
        t.pointerLevel = pointers;
        t.builtin = primitive;
        return t;
    }

    public boolean isBaseFloat() {
        long t = getBaseType();
        return t == Typing.PrimitiveType.F32.llvmType || t == Typing.PrimitiveType.F64.llvmType;
    }

    public boolean isBaseUnsigned() {
        long t = getBaseType();
        return t == Typing.PrimitiveType.U8.llvmType || t == Typing.PrimitiveType.U16.llvmType || t == Typing.PrimitiveType.U32.llvmType || t == Typing.PrimitiveType.U64.llvmType;
    }

    public boolean isBaseInteger() {
        long t = getBaseType();
        return isBaseUnsigned() || t == Typing.PrimitiveType.I8.llvmType || t == Typing.PrimitiveType.I16.llvmType || t == Typing.PrimitiveType.I32.llvmType || t == Typing.PrimitiveType.I64.llvmType;
    }

    public boolean isRealFloat() {
        long t = getBaseType();
        return t == Typing.PrimitiveType.F32.llvmType || t == Typing.PrimitiveType.F64.llvmType;
    }

    public boolean isRealUnsigned() {
        long t = getLLVMType();
        return t == Typing.PrimitiveType.U8.llvmType || t == Typing.PrimitiveType.U16.llvmType || t == Typing.PrimitiveType.U32.llvmType || t == Typing.PrimitiveType.U64.llvmType;
    }

    public boolean isRealInteger() {
        long t = getLLVMType();
        return isRealUnsigned() || t == Typing.PrimitiveType.I8.llvmType || t == Typing.PrimitiveType.I16.llvmType || t == Typing.PrimitiveType.I32.llvmType || t == Typing.PrimitiveType.I64.llvmType;
    }

    public boolean isPointer() {
        return pointerLevel > 0;
    }
}
