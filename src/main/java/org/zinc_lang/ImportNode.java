package org.zinc_lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ImportNode extends StatementNode {

    public String path;

    @Override
    public String toString() {
        return "import " + path + ";";
    }

    @Override
    public Val codegen(long builder, long context) {
        String[] paths = path.split("\\.");
        Path modulePath = Paths.get(Frontend.module().path);
        for (String p : paths) {
            modulePath = modulePath.resolve(p);
            if (!Files.exists(modulePath) || Files.isDirectory(modulePath)) {
                throw new RuntimeException("Error module " + path + " does not exist.");
            }
        }
        Frontend.Module current = Frontend.module();
        try {
            current.imports.add(Frontend.createModule(Compiler.buildAST(Zinc.readFile(modulePath)), modulePath.getParent().toFile().getAbsolutePath(), false));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
