package net.dustrean.libloader.boot.loaders;

import net.dustrean.libloader.boot.JarLoader;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This is a JarLoader for dynamic projects like <strong>plugins</strong>, which has been tested for <strong>Spigot and Velocity</strong>
 * Required is the JVM argument: <p><strong>--add-opens java.base/java.net=ALL-UNNAMED</strong></p>
 * You can get the classloader which is needed using:
 * <pre>
 * {@code
 * URLClassLoader classLoader = (URLClassLoader) this.getClass().getClassLoader();
 * // Both usable for Spigot and Velocity
 * }
 * </pre>
 */
public class URLClassLoaderJarLoader implements JarLoader {
    private final URLClassLoader classLoader;

    public URLClassLoaderJarLoader(URLClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Override
    public void load(URL javaFile) {
        try {
            Method method = classLoader.getClass().getSuperclass().getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(classLoader, javaFile);
        } catch (Throwable t) {
            System.out.println("Error while loading JarFile " + javaFile);
            t.printStackTrace(System.out);
        }
    }
}
