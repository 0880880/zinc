package org.zinc_lang;

import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectObjectMap;

import java.util.*;
import java.util.function.Consumer;

public class Scope {

    public static abstract class Def {
        public String name;
        public long type;
        public long value;
    }

    public static class VariableDef extends Def {
    }

    public static class FuncDef extends Def {
        public List<TypeVal> argTypes;
        public List<String> argNames;
        public int args;
        public FuncNode.Visibility visibility;

        public TypeVal retType;
    }

    public static class StructDef extends Def {
        public List<TypeVal> types;
        public List<String> names;
        public int bodySize;
    }

    public static void begin() {
        Frontend.module().scopes.add(new HashMap<>());
    }

    public static void var(String name, long type, long value) {
        if (Frontend.module().scopes.getLast().containsKey(name)) {
            throw new RuntimeException("Variable with this name already exists");
        }
        VariableDef d = new VariableDef();
        d.name = name;
        d.type = type;
        d.value = value;
        Map<String, Def> m = Frontend.module().scopes.isEmpty() ? Frontend.module().global : Frontend.module().scopes.getLast();
        m.put(name, d);
    }

    public static void func(String name, long type, long value, FuncNode func) {
        if (!Frontend.module().scopes.isEmpty()) {
            // TODO error
            return;
        }
        if (Frontend.module().global.containsKey(name)) {
            throw new RuntimeException("Function with this name already exists");
        }
        FuncDef d = new FuncDef();
        d.name = name;
        d.type = type;
        d.value = value;
        d.argTypes = func.argTypes;
        d.visibility = func.visibility;
        d.argNames = new ArrayList<>();
        if (func.argNames != null) {
            func.argNames.forEach(i -> d.argNames.add(i.value));
        }
        d.retType = func.retType;
        d.args = func.args;
        Frontend.module().global.put(name, d);
    }

    public static void def(String name, Def def, String typeName) {
        if (Frontend.module().global.containsKey(name) || (!Frontend.module().scopes.isEmpty() && Frontend.module().scopes.getLast().containsKey(name))) {
            throw new RuntimeException(typeName + " with this name already exists");
        }
        Map<String, Def> m = Frontend.module().scopes.isEmpty() ? Frontend.module().global : Frontend.module().scopes.getLast();
        m.put(name, def);
    }

    public static Def get(String name) {
        if (Frontend.module().global.containsKey(name)) {
            return Frontend.module().global.get(name);
        }
        if (Frontend.module().scopes.isEmpty()) {
            return null;
        }
        for (int i = 0; i < Frontend.module().scopes.size(); i++) {
            Map<String, Def> scope = Frontend.module().scopes.get(i);
            for (String k : scope.keySet()) {
                if (scope.get(k).name.equals(name)) {
                    return scope.get(k);
                }
            }
        }
        return null;
    }

    public static FuncDef getFunction() {
        for (Def d : Frontend.module().global.values()) {
            if (d instanceof FuncDef) {
                return (FuncDef) d;
            }
        }
        return null;
    }

    public static void end() {
        if (Frontend.module().scopes.isEmpty())
            return;
        Frontend.module().scopes.removeLast();
    }

    public void forEach(Consumer<? super Def> action) {
        for (int i = 0; i < Frontend.module().scopes.size(); i++) {
            for (String k : Frontend.module().scopes.get(i).keySet()) {
                action.accept(Frontend.module().scopes.get(i).get(k));
            }
        }
    }

}
