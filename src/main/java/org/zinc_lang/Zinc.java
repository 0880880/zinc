package org.zinc_lang;

import com.github.zeroeighteightzero.lwlp.Token;
import org.apache.commons.io.FilenameUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.llvm.LLVMLinker;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.lwjgl.llvm.LLVMCore.*;
import static org.lwjgl.llvm.LLVMExecutionEngine.*;

@CommandLine.Command(
        name = "zinc",
        mixinStandardHelpOptions = true,
        version = "Zinc Compiler 0.1.0",
        description = "The official compiler for the Zinc programming language 🚀",
        subcommands = {
                Zinc.Build.class,
                Zinc.Run.class,
                Zinc.Check.class,
                CommandLine.HelpCommand.class
        },
        footer = {
                "",
                "For more information, visit https://github.com/your-repo/zinc"
        }
)
public class Zinc implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    public static void main(String[] args) {
        // The exit code is propagated to the shell
        int exitCode = new CommandLine(new Zinc()).execute(args);
        System.exit(exitCode);
    }

    /**
     * This method is called when 'zinc' is run without subcommands.
     * It prints the usage help.
     */
    @Override
    public void run() {
        spec.commandLine().usage(System.out);
    }

    /**
     * A utility method to read file contents.
     */
    private static String readFile(Path p) throws IOException {
        if (!Files.exists(p)) {
            System.err.println("Error: File not found at " + p);
            return null;
        }
        return Files.readString(p);
    }

    // --- Shared Options Mixin ---
    // Contains options common to multiple subcommands to avoid code duplication.
    static class CompilerOptions {

        @CommandLine.Option(
                names = "--dump-ir",
                description = "Print the LLVM Intermediate Representation (IR) to the console."
        )
        boolean dumpIR;

        @CommandLine.Option(
                names = "--show-ast",
                description = "Print the Abstract Syntax Tree (AST) after parsing."
        )
        boolean showAST;

    }

    // --- Subcommands ---

    @CommandLine.Command(
            name = "build",
            mixinStandardHelpOptions = true,
            description = "Compile a Zinc program into an executable binary."
    )
    static class Build implements Runnable {

        @CommandLine.Mixin
        private CompilerOptions options = new CompilerOptions();

        @CommandLine.Option(
                names = {"-o", "--output"},
                description = "The path for the output executable. Defaults to the input file name.",
                paramLabel = "<path>"
        )
        String outputFile;

        @CommandLine.Parameters(
                index = "0",
                description = "The main source file to compile (.zn).",
                paramLabel = "<file>"
        )
        String inputFile;

        @Override
        public void run() {
            Frontend.initLLVM();
            Linker.initialize();

            File inFile = new File(inputFile);
            if (inFile.isDirectory()) {
                throw new RuntimeException("Input cannot be a directory.");
            }

            try {
                String source = readFile(inFile.toPath());
                if (source == null) return;

                String out = outputFile;
                if (out == null) {
                    out = new File(inFile.getParentFile(), FilenameUtils.getBaseName(inputFile)).getAbsolutePath();
                } else if (new File(out).isDirectory()) {
                    out = new File(out, FilenameUtils.getBaseName(inputFile)).getAbsolutePath();
                }

                Path tempDir = Files.createTempDirectory("zinc_compiler");
                Parser.DEBUG = options.showAST;

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    Frontend.Module m = Frontend.createModule(Compiler.buildAST(source), inFile.getParent(), options.dumpIR);
                    for (Frontend.Module imp : m.imports) {
                        if (!imp.verify()) return;
                        LLVMLinker.LLVMLinkModules2(m.llvmModule, imp.llvmModule);
                    }
                    m.verify();

                    File object = tempDir.resolve(FilenameUtils.getBaseName(inputFile) + ".o").toFile();
                    Frontend.buildObjectFile(m.llvmModule, object);

                    Linker.link(object.getAbsolutePath(), out);
                    System.out.println("✅ Build successful! Output: " + out);
                }

            } catch (IOException e) {
                throw new RuntimeException("An I/O error occurred.", e);
            }
        }
    }

    @CommandLine.Command(
            name = "run",
            mixinStandardHelpOptions = true,
            description = "Compile and immediately run a Zinc program in memory."
    )
    static class Run implements Runnable {

        @CommandLine.Mixin
        private CompilerOptions options = new CompilerOptions();

        @CommandLine.Parameters(
                index = "0",
                description = "The source file to run (.zn).",
                paramLabel = "<file>"
        )
        String inputFile;

        @Override
        public void run() {
            Frontend.initLLVM();
            File inFile = new File(inputFile);
            if (inFile.isDirectory()) {
                throw new RuntimeException("Input cannot be a directory.");
            }

            try {
                String source = readFile(inFile.toPath());
                if (source == null) return;

                Parser.DEBUG = options.showAST;

                try (MemoryStack stack = MemoryStack.stackPush()) {
                    Frontend.Module m = Frontend.createModule(Compiler.buildAST(source), inFile.getParent(), options.dumpIR);
                    for (Frontend.Module imp : m.imports) {
                        if (!imp.verify()) return;
                        LLVMLinker.LLVMLinkModules2(m.llvmModule, imp.llvmModule);
                    }
                    m.verify();

                    PointerBuffer error = stack.mallocPointer(1);
                    PointerBuffer enginePtr = stack.mallocPointer(1);
                    if (LLVMCreateExecutionEngineForModule(enginePtr, m.llvmModule, error)) {
                        String msg = MemoryUtil.memUTF8(error.get(0));
                        System.err.printf("Failed to create execution engine: \"%s\"%n", msg);
                        nLLVMDisposeMessage(error.get(0));
                        return;
                    }

                    long mainFunc = LLVMGetNamedFunction(m.llvmModule, "main");
                    if (mainFunc == 0) {
                        System.err.println("Error: 'main' function not found.");
                        return;
                    }

                    LLVMRunFunction(enginePtr.get(0), mainFunc, null);
                    LLVMDisposeExecutionEngine(enginePtr.get(0));
                }
            } catch (IOException e) {
                throw new RuntimeException("An I/O error occurred.", e);
            } finally {
                LLVMContextDispose(Frontend.context);
            }
        }
    }

    @CommandLine.Command(
            name = "check",
            mixinStandardHelpOptions = true,
            description = "Check a Zinc source file for syntax and semantic errors without generating code."
    )
    static class Check implements Runnable {

        @CommandLine.Mixin
        private CompilerOptions options = new CompilerOptions();

        @CommandLine.Parameters(
                index = "0",
                description = "The source file to check (.zn).",
                paramLabel = "<file>"
        )
        String inputFile;

        @Override
        public void run() {
            System.out.println("Checking file: " + inputFile);
            try {
                String source = readFile(Paths.get(inputFile));
                if (source == null) return;

                Parser.DEBUG = options.showAST;

                // This will parse the source and build the AST.
                // It is assumed that Compiler.buildAST will throw an exception on a syntax error.
                Compiler.buildAST(source);

                System.out.println("✅ Syntax check passed successfully for " + inputFile);

            } catch (Exception e) {
                // The parser/lexer should have already printed the specific error.
                System.err.println("❌ Check failed for " + inputFile);
                // Re-throw to ensure a non-zero exit code from picocli
                throw new RuntimeException("Check failed.", e);
            }
        }
    }
}