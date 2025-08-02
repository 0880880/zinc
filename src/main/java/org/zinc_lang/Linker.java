package org.zinc_lang;

import java.io.File;
import java.io.IOException;

public class Linker {

    private enum LinkerType {
        CLANG,
        GCC,
        MSVC
    }

    public static File findFileInPath(String fileName) {
        String path = System.getenv("PATH");
        String[] paths = path.split(File.pathSeparator);

        // On Windows, try common executable extensions
        String[] extensions = {""};
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            String pathext = System.getenv("PATHEXT");
            if (pathext != null) {
                extensions = pathext.toLowerCase().split(";");
            } else {
                extensions = new String[]{".exe", ".bat", ".cmd"};
            }
        }

        for (String dir : paths) {
            for (String ext : extensions) {
                File file = new File(dir, fileName + ext);
                if (file.exists() && file.isFile()) {
                    return file;
                }
            }
        }
        return null;
    }

    private static LinkerType linkerType = null;
    private static File linker;

    public static void initialize() {
        File l = findFileInPath("clang");
        if (l != null) {
            linkerType = LinkerType.CLANG;
            linker = l;
            return;
        }
        l = findFileInPath("lld");
        if (l != null) {
            linkerType = LinkerType.CLANG;
            linker = l;
            return;
        }
        l = findFileInPath("gcc");
        if (l != null) {
            linkerType = LinkerType.GCC;
            linker = l;
            return;
        }
        l = findFileInPath("ld");
        if (l != null) {
            linkerType = LinkerType.GCC;
            linker = l;
            return;
        }
        l = findFileInPath("link");
        if (l != null) {
            linkerType = LinkerType.MSVC;
            linker = l;
            return;
        }
    }

    public static boolean link(String input, String output) {
        if (linkerType == null)
            return false;
        ProcessBuilder pb = new ProcessBuilder();
        if (linkerType == LinkerType.CLANG) {
            pb.command(linker.getAbsolutePath(), input, "-o", output);
        } else if (linkerType == LinkerType.GCC) {
            pb.command(linker.getAbsolutePath(), input, "-o", output);
        } else if (linkerType == LinkerType.MSVC) {
            System.err.println("MSVC not supported");
            return false;
        }
        try {
            Process p = pb.start();
            return p.waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
