package dev.redicloud.libloader.boot;

import java.lang.instrument.*;
import java.util.jar.*;

public class Agent {
    static Instrumentation instrumentation = null;

    public static void agentmain(final String agentArgs, final Instrumentation inst) {
        if (inst != null && Agent.instrumentation == null) {
            if (System.getProperty("libloader.debug") != null) {
                System.out.println("[Agent-Main] Loaded Instrumentation");
            }
            Agent.instrumentation = inst;
        }
    }

    public static void premain(final String agentArgs, final Instrumentation inst) {
        if (inst != null && Agent.instrumentation == null) {
            if (System.getProperty("libloader.debug") != null) {
                System.out.println("[Premain-Main] Loaded Instrumentation");
            }
            Agent.instrumentation = inst;
        }
    }

    public static boolean appendJarFile(final JarFile file) {
        if (Agent.instrumentation != null) {
            Agent.instrumentation.appendToSystemClassLoaderSearch(file);
            return true;
        }
        return false;
    }

}
