package dev.redicloud.libloader.boot.loaders;

import dev.redicloud.libloader.boot.Agent;
import dev.redicloud.libloader.boot.JarLoader;

import java.io.File;
import java.net.URL;
import java.util.jar.JarFile;

public class DefaultJarLoader implements JarLoader {
    public DefaultJarLoader() {
    }

    @Override
    public void load(URL javaFile) {
        try {
            Agent.appendJarFile(new JarFile(new File(javaFile.toURI())));
        } catch (Throwable t) {
            System.out.println("Error while loading JarFile " + javaFile);
            t.printStackTrace(System.out);
        }
    }
}
