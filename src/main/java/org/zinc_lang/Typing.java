package org.zinc_lang;

import com.github.tommyettinger.ds.*;
import com.github.tommyettinger.ds.support.util.LongIterator;

import static org.lwjgl.llvm.LLVMCore.*;

public class Typing {

    public static class Hint implements AutoCloseable {
        Hint() {
        }

        @Override
        public void close() {
            Typing.hintPop();
        }
    }

    public static class PrimitiveType {
        public final int precisionLevel;
        public final String name;
        public final long llvmType;

        public PrimitiveType(int precisionLevel, String name, long llvmType) {
            this.precisionLevel = precisionLevel;
            this.name = name;
            this.llvmType = llvmType;
        }

        public static PrimitiveType VOID, I8, U8, I16, U16, I32, U32, I64, U64, F32, F64, BOOLEAN;
    }

    public static final ObjectLongMap<String> primitives = new ObjectLongMap<>();
    public static final ObjectList<PrimitiveType> primitiveTypes = new ObjectList<>();
    public static final ObjectObjectMap<String, PrimitiveType> primitivesEnumStr = new ObjectObjectMap<>();
    public static final LongObjectMap<PrimitiveType> primitivesEnumLong = new LongObjectMap<>();
    public static final ObjectDeque<String> hintsNames = new ObjectDeque<>();
    public static final LongDeque hints = new LongDeque();

    public static final String INT_TYPE = "i64";
    public static final String FLOAT_TYPE = "f64";

    public static void populateTypes(long context) {
        PrimitiveType.I8 = new PrimitiveType(0, "i8", LLVMInt8TypeInContext(context));
        PrimitiveType.U8 = new PrimitiveType(0, "u8", PrimitiveType.I8.llvmType);
        PrimitiveType.I16 = new PrimitiveType(1, "i16", LLVMInt16TypeInContext(context));
        PrimitiveType.U16 = new PrimitiveType(1, "u16", PrimitiveType.I16.llvmType);
        PrimitiveType.I32 = new PrimitiveType(2, "i32", LLVMInt32TypeInContext(context));
        PrimitiveType.U32 = new PrimitiveType(2, "u32", PrimitiveType.I32.llvmType);
        PrimitiveType.I64 = new PrimitiveType(3, "i64", LLVMInt64TypeInContext(context));
        PrimitiveType.U64 = new PrimitiveType(3, "u64", PrimitiveType.I64.llvmType);
        PrimitiveType.F32 = new PrimitiveType(0, "f32", LLVMFloatTypeInContext(context));
        PrimitiveType.F64 = new PrimitiveType(1, "f64", LLVMDoubleTypeInContext(context));
        PrimitiveType.VOID = new PrimitiveType(0, "void", LLVMVoidTypeInContext(context));
        PrimitiveType.BOOLEAN = new PrimitiveType(0, "bool", LLVMInt1TypeInContext(context));
        primitives.put("bool", PrimitiveType.BOOLEAN.llvmType);
        primitives.put("i8", PrimitiveType.I8.llvmType);
        primitives.put("u8", primitives.get("i8"));
        primitives.put("i16", PrimitiveType.I16.llvmType);
        primitives.put("u16", primitives.get("i16"));
        primitives.put("i32", PrimitiveType.I32.llvmType);
        primitives.put("u32", primitives.get("i32"));
        primitives.put("i64", PrimitiveType.I64.llvmType);
        primitives.put("u64", primitives.get("i64"));
        primitives.put("f32", PrimitiveType.F32.llvmType);
        primitives.put("f64", PrimitiveType.F64.llvmType);
        primitives.put("void", PrimitiveType.VOID.llvmType);
        primitives.put("int", primitives.get(INT_TYPE));
        primitives.put("float", primitives.get(FLOAT_TYPE));
        primitives.put("ptr", LLVMPointerTypeInContext(context, 0));
        primitivesEnumStr.put("bool", PrimitiveType.BOOLEAN);
        primitivesEnumStr.put("i8", PrimitiveType.I8);
        primitivesEnumStr.put("u8", PrimitiveType.U8);
        primitivesEnumStr.put("i16", PrimitiveType.I16);
        primitivesEnumStr.put("u16", PrimitiveType.U16);
        primitivesEnumStr.put("i32", PrimitiveType.I32);
        primitivesEnumStr.put("u32", PrimitiveType.U32);
        primitivesEnumStr.put("i64", PrimitiveType.I64);
        primitivesEnumStr.put("int", PrimitiveType.I64);// FIXME should use INT_TYPE
        primitivesEnumStr.put("u64", PrimitiveType.U64);
        primitivesEnumStr.put("f32", PrimitiveType.F32);
        primitivesEnumStr.put("f64", PrimitiveType.F64);// FIXME should use FLOAT_TYPE
        primitivesEnumStr.put("float", PrimitiveType.F64);
        primitivesEnumStr.put("void", PrimitiveType.VOID);
        for (String k : primitivesEnumStr.keySet()) {
            primitivesEnumLong.put(primitives.get(k), primitivesEnumStr.get(k));
        }
        primitiveTypes.add(PrimitiveType.I8);
        primitiveTypes.add(PrimitiveType.U8);
        primitiveTypes.add(PrimitiveType.I16);
        primitiveTypes.add(PrimitiveType.U16);
        primitiveTypes.add(PrimitiveType.I32);
        primitiveTypes.add(PrimitiveType.U32);
        primitiveTypes.add(PrimitiveType.I64);
        primitiveTypes.add(PrimitiveType.U64);
        primitiveTypes.add(PrimitiveType.F32);
        primitiveTypes.add(PrimitiveType.F64);
        primitiveTypes.add(PrimitiveType.VOID);
        primitiveTypes.add(PrimitiveType.BOOLEAN);
    }

    public static long prim(String primitive) {
        return primitives.get(primitive);
    }

    public static Hint hint(String name, long type) {
        hintsNames.addLast(name);
        hints.addLast(type);
        return new Hint();
    }

    public static Hint hint(String name, String primType) {
        hintsNames.addLast(name);
        hints.addLast(primitives.get(primType));
        return new Hint();
    }

    public static void hintPop() {
        hintsNames.removeLast();
        hints.removeLast();
    }

    private static final String[] intNames = {
            "i8", "u8",
            "i16", "u16",
            "i32", "u32",
            "i64", "u64",
            "int"
    };

    public static Val createInt(String longStr) {
        String hName = hintsNames.peekLast();
        boolean nul = false;
        if (hName == null) {
            nul = true;
            hName = INT_TYPE;
        }

        if (!Utils.equalsAny(hName, intNames)) {
            // TODO error : type mismatch
            return null;
        }

        boolean sign = hName.charAt(0) == 'i';

        long h;
        if (nul) {
            h = primitives.get("int");
        } else {
            h = hints.peekLast();
        }
        return Val.prim(h, LLVMConstInt(h, sign ? Long.parseLong(longStr) : Long.parseUnsignedLong(longStr), sign));

    }

    public static Val createFloat(double d) {
        String hName = hintsNames.peekLast();
        boolean nul = false;
        if (hName == null) {
            nul = true;
            hName = FLOAT_TYPE;
        }

        if (!Utils.equalsAny(hName, "f32", "f64", "float")) {
            // TODO error : type mismatch
            return null;
        }

        long h;
        if (nul) {
            h = primitives.get("float");
        } else {
            h = hints.peekLast();
        }
        return Val.prim(h, LLVMConstReal(h, d));

    }

    public static boolean isUnsigned(long type) {
        return type == primitives.get("u8") || type == primitives.get("u16") || type == primitives.get("u32") || type == primitives.get("u64");
    }

    public static boolean isInteger(long type) {
        return type == primitives.get("i8") || type == primitives.get("i16") || type == primitives.get("i32") || type == primitives.get("i64");
    }

    public static boolean isFloat(long type) {
        return type == primitives.get("f32") || type == primitives.get("f64");
    }

    public record BinaryCast(
            long llvmType,
            PrimitiveType pType
    ) {
    }

    public static BinaryCast upgrade(long builder, Val lhs, Val rhs) {
        long llvmType = lhs.type.getLLVMType();
        PrimitiveType pType = typePrimitive(lhs.type);
        if (lhs.type.getLLVMType() == rhs.type.getLLVMType()) {
            ;
        } else {
            if (lhs.type.builtin && rhs.type.builtin) {
                Val high = typePrimitive(lhs.type).precisionLevel > typePrimitive(rhs.type).precisionLevel ? lhs : rhs;
                Val low = typePrimitive(lhs.type).precisionLevel > typePrimitive(rhs.type).precisionLevel ? rhs : lhs;
                low.type.llvmType = high.type.getLLVMType();
                low.type.baseType = high.type.getLLVMType();
                pType = typePrimitive(high.type);
                llvmType = high.type.getLLVMType();
                if (lhs.isRealFloat() && rhs.isRealFloat()) {
                    low.llvmValue = LLVMBuildFPExt(builder, low.llvmValue, high.type.getLLVMType(), "implicit_cast_f64");
                } else if (lhs.isRealUnsigned() && rhs.isRealInteger()) {
                    low.llvmValue = LLVMBuildZExt(builder, low.llvmValue, high.type.getLLVMType(), "implicit_cast_uinteger");
                } else if (lhs.isRealInteger() && rhs.isRealInteger()) {
                    low.llvmValue = LLVMBuildSExt(builder, low.llvmValue, high.type.getLLVMType(), "implicit_cast_integer");
                } else {
                    System.err.printf("Error: Type mismatch %s with %s.\n", typePrimitive(lhs.type).name, typePrimitive(rhs.type).name);
                    // TODO error type mismatch
                    return null;
                }
            } else {
                // TODO error type mismatch
                return null;
            }
        }
        return new BinaryCast(llvmType, pType);
    }

    public static PrimitiveType typePrimitive(TypeVal type) {
        LongIterator it = primitivesEnumLong.keySet().iterator();
        while (it.hasNext()) {
            long l = it.next();
            if (l == type.getLLVMType()) {
                return primitivesEnumLong.get(l);
            }
        }
        return null;
    }

    public static BinaryCast upgradeTo(long builder, Val from, long to) {
        long llvmType = from.type.getLLVMType();
        PrimitiveType pType = typePrimitive(from.type);
        if (from.type.getLLVMType() == to) {
            ;
        } else {
            if (from.type.builtin) {
                PrimitiveType toPType = null;
                for (PrimitiveType p : primitivesEnumStr.values()) {
                    if (p.llvmType == to) {
                        toPType = p;
                        break;
                    }
                }
                if (pType.precisionLevel > toPType.precisionLevel) {
                    // TODO error can't lower `to` to `from`
                    return null;
                }
                from.type.llvmType = to;
                from.type.baseType = to;
                llvmType = to;
                pType = toPType;
                if (from.isRealFloat() && isFloat(to)) {
                    from.llvmValue = LLVMBuildFPExt(builder, from.llvmValue, to, "implicit_cast_f64");
                } else if (from.isRealInteger() && isUnsigned(to)) {
                    from.llvmValue = LLVMBuildZExt(builder, from.llvmValue, to, "implicit_cast_uinteger");
                } else if (from.isRealInteger() && isInteger(to)) {
                    from.llvmValue = LLVMBuildSExt(builder, from.llvmValue, to, "implicit_cast_integer");
                } else {
                    System.err.printf("Error: Type mismatch %s with %s.\n", typePrimitive(from.type).name, toPType.name);
                    // TODO error type mismatch
                    return null;
                }
            } else {
                // TODO error type mismatch
                return null;
            }
        }
        return new BinaryCast(llvmType, pType);
    }

}
