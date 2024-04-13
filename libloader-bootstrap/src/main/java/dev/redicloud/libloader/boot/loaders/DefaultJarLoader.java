package dev.redicloud.libloader.boot.loaders;

import java.net.*;
import java.io.*;
import java.util.jar.*;

import dev.redicloud.libloader.boot.*;

public class DefaultJarLoader implements JarLoader {
    @Override
    public void load(final URL javaFile) {
        try {
            Agent.appendJarFile(new JarFile(new File(javaFile.toURI())));
        } catch (Throwable t) {
            System.out.println("Error while loading JarFile " + javaFile);
            t.printStackTrace(System.out);
        }
    }
}
