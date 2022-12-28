package net.dustrean.libloader.boot;

import java.lang.instrument.Instrumentation;
import java.util.jar.JarFile;

public class Agent {
    static Instrumentation instrumentation = null;

    public static void agentmain(String agentArgs, Instrumentation inst) {
        if (inst != null && instrumentation == null) {
            System.out.println("[Agent-Main] Loaded Instrumentation");
            instrumentation = inst;
        }
    }

    public static void premain(String agentArgs, Instrumentation inst) {
        if (inst != null && instrumentation == null) {
            System.out.println("[Premain-Main] Loaded Instrumentation");
            instrumentation = inst;
        }
    }

    public static boolean appendJarFile(JarFile file) {
        if (instrumentation != null)
            instrumentation.appendToSystemClassLoaderSearch(file);
        else return false;
        return true;
    }
}
