package org.zinc_lang;

import com.github.tommyettinger.ds.ObjectDeque;
import com.github.tommyettinger.ds.ObjectList;
import com.github.tommyettinger.ds.ObjectObjectMap;
import org.lwjgl.PointerBuffer;
import org.lwjgl.llvm.*;
import org.lwjgl.system.Configuration;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.File;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.lwjgl.llvm.LLVMAnalysis.LLVMPrintMessageAction;
import static org.lwjgl.llvm.LLVMAnalysis.LLVMVerifyModule;
import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.llvm.LLVMTargetMachine.*;
import static org.lwjgl.system.JNI.invokeV;
import static org.lwjgl.system.MemoryUtil.NULL;


public class Frontend {

    public static long context;
    public static ObjectDeque<Module> modules = new ObjectDeque<>();

    public static class Module {
        public long llvmModule;
        public String path;
        public ObjectList<Module> imports = new ObjectList<>();
        public ObjectList<Map<String, Scope.Def>> scopes = new ObjectList<>();
        public ObjectObjectMap<String, Scope.Def> global = new ObjectObjectMap<>();


        public Module(long l) {
            this.llvmModule = l;
        }

        public boolean verify() {
            MemoryStack stack = MemoryStack.stackGet();
            PointerBuffer error = stack.mallocPointer(1);

            if (LLVMVerifyModule(module().llvmModule, LLVMPrintMessageAction, error)) {
                String msg = MemoryUtil.memUTF8(error.get(0));
                System.err.printf("error : \"%s\"\n", msg);
                nLLVMDisposeMessage(error.get(0));
                return false;
            }
            return true;
        }

    }

    private static void initLlvmStuff(Class<?> container) {
        for(var field : container.getDeclaredFields()) {
            if(field.getType() != long.class || field.getModifiers() != (Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL)) {
                continue;
            }

            try {
                field.setAccessible(true);
                var address = (long) field.get(null);
                if(address != NULL) {
                    invokeV(address);
                }
            } catch(Throwable e) {
                throw new RuntimeException("Failed to get " + container.getDeclaringClass().getSimpleName() + '#' + field.getName());
            }
        }
    }

    // Slightly hacky, don't care.
    private static void initLlvmStuff() {
        try {
            Set.of(
                    LLVMTargetAArch64.Functions.class,
                    LLVMTargetAMDGPU.Functions.class,
                    LLVMTargetARM.Functions.class,
                    LLVMTargetMips.Functions.class,
                    LLVMTargetPowerPC.Functions.class,
                    LLVMTargetRISCV.Functions.class,
                    LLVMTargetWebAssembly.Functions.class,
                    LLVMTargetX86.Functions.class
            ).forEach(Frontend::initLlvmStuff);
        } catch(Throwable e) {
            throw new RuntimeException("Failed to init LWJGL", e);
        }
    }

    public static void initLLVM() {
        Configuration.DISABLE_FUNCTION_CHECKS.set(true);

        try {
            LLVMCore.getLibrary();
        } catch (UnsatisfiedLinkError e) {
            throw new IllegalStateException(
                    "Please configure the LLVM shared libraries path with:\n" +
                            "\t-Dorg.lwjgl.llvm.libname=<LLVM shared library path> or\n" +
                            "\t-Dorg.lwjgl.librarypath=<path that contains LLVM shared libraries>", e);
        }

        initLlvmStuff();

        context = LLVMContextCreate();
        System.out.println(" [FRONTEND]   Created context");
        Typing.populateTypes(context);
        System.out.println(" [FRONTEND]   Populated type");
    }

    public static Module module() {
        return modules.getLast();
    }


    public static Module createModule(List<StatementNode> program, String path, boolean printIR) {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            modules.addLast(new Module(LLVMModuleCreateWithNameInContext("main", context)));
            module().path = path;
            long builder = LLVMCreateBuilderInContext(context);

            System.err.println("\nProgram:\n");
            for (StatementNode s : program) {
                System.err.println(s.toString());
                System.err.println("\n");
            }
            System.err.println("\n\n");

            for (StatementNode node : program) {
                System.out.println(" [FRONTEND]   Generating " + node.getClass().getSimpleName());
                node.codegen(builder, context);
            }
            if (printIR)
                LLVMDumpModule(module().llvmModule);

            Module m = module();
            modules.removeLast();
            return m;

        }

    }

    public static void buildObjectFile(long module, File outputFile) {

        try (MemoryStack stack = MemoryStack.stackPush()) {

            String triple = LLVMTargetMachine.LLVMGetDefaultTargetTriple();
            if (triple == null) {
                throw new RuntimeException("Failed to get target triple.");
            }
            System.out.printf("Building for %s\n", triple);

            PointerBuffer targetBuf = stack.mallocPointer(1);
            PointerBuffer errorBuf = stack.mallocPointer(1);
            LLVMGetTargetFromTriple(stack.UTF8Safe(triple), targetBuf, errorBuf);

            long targetMachine = LLVMCreateTargetMachine(
                    targetBuf.get(0),
                    triple,
                    "", "", // CPU and features
                    LLVMCodeGenLevelDefault,
                    LLVMRelocDefault,
                    LLVMCodeModelDefault
            );

            ByteBuffer outBuf = stack.UTF8Safe(outputFile.getAbsolutePath());
            if (outBuf == null) {
                throw new RuntimeException("Failed to allocate string");
            }
            LLVMTargetMachineEmitToFile(targetMachine, module, outBuf, LLVMObjectFile, errorBuf);

            LLVMDisposeModule(module);

        }

    }

}
