package net.dustrean.libloader.boot.loaders;

import net.dustrean.libloader.boot.JarLoader;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * This is a JarLoader for Spigot.
 * You have to set the classLoader from your plugin to be able to use this.
 * Required is the JVM argument: --add-opens java.base/java.net=ALL-UNNAMED
 * Do it like this:
 * <pre>
 * {@code
 * public class YourPlugin extends JavaPlugin {
 *    public void onEnable() {
 *        SpigotJarLoader.setClassLoader(((URLClassLoader) this.getClass().getClassLoader()));
 *    }
 * }
 * }
 * </pre>
 */
public class SpigotJarLoader implements JarLoader {
    public static void setClassLoader(URLClassLoader classLoader) {
        SpigotJarLoader.classLoader = classLoader;
    }

    private static URLClassLoader classLoader;
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
