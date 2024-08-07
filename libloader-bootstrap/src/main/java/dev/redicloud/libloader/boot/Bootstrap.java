package dev.redicloud.libloader.boot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dev.redicloud.libloader.boot.apply.ResourceLoader;
import dev.redicloud.libloader.boot.apply.impl.ClassLoaderResourceLoader;
import dev.redicloud.libloader.boot.loaders.DefaultJarLoader;
import dev.redicloud.libloader.boot.model.IgnoreConfiguration;
import dev.redicloud.libloader.boot.model.LibraryConfiguration;
import dev.redicloud.libloader.boot.model.SelfDependency;
import org.jsoup.Jsoup;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * The on runtime loader.
 * After finishing, it will try to run the main class.
 */
public class Bootstrap {
    public static final Type DEPENDENCY_TYPE = new TypeToken<ArrayList<SelfDependency>>() {}.getType();
    public static final Type REPOSITORY_TYPE = new TypeToken<ArrayList<String>>() {}.getType();
    private static final List<String> downloadedSnapshots = new ArrayList<>();
    private final ArrayList<String> loaded = new ArrayList<>();
    public static final Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private IgnoreConfiguration ignore;
    private LibraryConfiguration configuration;
    private int ignoreInitialCount;
    private boolean succeeded = false;


    public static void main(String[] args) throws IOException, URISyntaxException {
        if (System.getProperty("libloader.debug.delay") != null) {
            try {
                Thread.sleep(Long.parseLong(System.getProperty("libloader.debug.delay")));
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to sleep", e);
            }
        }
        System.out.println("Loading libraries...");
        final Bootstrap bootstrap = new Bootstrap();
        bootstrap.apply(new DefaultJarLoader());
        bootstrap.bootSuccess();
        // Run Main Class
        try {
            Class.forName(bootstrap.configuration.mainClass()).getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
        } catch (ClassNotFoundException e) {
            System.out.println("Error while getting main class. Check your gradle build configuration.");
        } catch (NoSuchMethodException e) {
            System.out.println("Main method missing in class " + bootstrap.configuration.mainClass());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpURLConnection retrieve(HttpURLConnection conn) {
        if (conn.getURL().getHost().equalsIgnoreCase("repo.redicloud.dev"))
            conn.addRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((System.getenv("REDI_CLOUD_REPO_USERNAME") + ":" + System.getenv("REDI_CLOUD_REPO_PASSWORD")).getBytes()));
        return conn;
    }

    private static String readString(InputStream inputStream) throws IOException {
        final int bufferSize = 1024;
        final char[] buffer = new char[bufferSize];
        final StringBuilder out = new StringBuilder();
        final Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();
    }

    public void apply(final JarLoader loader) throws IOException, URISyntaxException {
        this.apply(loader, Bootstrap.class.getClassLoader(), new ClassLoaderResourceLoader(Bootstrap.class.getClassLoader().getClass().getSimpleName(), Bootstrap.class.getClassLoader()));
    }

    public void apply(JarLoader loader, ClassLoader configClassLoader, ResourceLoader... resourceLoaders) throws IOException, URISyntaxException {
        // Load Configuration
        try {
            configuration = gson.fromJson(new InputStreamReader(Objects.requireNonNull(configClassLoader.getResourceAsStream("depends/config.json"))), LibraryConfiguration.class);
        } catch (Throwable e) {
            configuration = new LibraryConfiguration();
        }
        configuration.libraryFolderFile().mkdirs();
        final File ignoreFile = new File(configuration.libraryFolder(), "ignore.json");
        if (!ignoreFile.exists()) {
            FileWriter ignoreWrite = new FileWriter(ignoreFile);
            ignoreFile.createNewFile();
            gson.toJson(new IgnoreConfiguration(new ArrayList<>()), ignoreWrite);
            ignoreWrite.flush();
            ignoreWrite.close();
        }
        this.ignore = gson.fromJson(new FileReader(ignoreFile), IgnoreConfiguration.class);
        ignoreInitialCount = ignore.ignore().size();
        for (ResourceLoader resourceLoader : resourceLoaders) {
            Set<String> repositories = resourceLoader.getRepositories();
            Set<SelfDependency> dependencies = resourceLoader.getDependencies();

            if (System.getProperty("libloader.debug") != null)
                System.out.println("Loading " + dependencies.size() + " dependencies from " + resourceLoader.getName());

            dependencies.forEach(selfDependency -> {
                resolve(selfDependency, loader, repositories);
            });
        }

    }

    public void resolve(SelfDependency dependency, JarLoader loader, Set<String> repositories) {
        dependency.dependencies().forEach((child_depend) -> {
            if (!loaded.contains(child_depend.toString())) {
                resolve(child_depend, loader, repositories);
            }
        });
        if (ignore.ignore().contains(dependency.toString())) {
            return;
        }
        final File path = new File(configuration.libraryFolderFile(), dependency.toPath());
        if (!path.exists() || (dependency.groupId().startsWith("dev.redicloud") && dependency.version().endsWith("SNAPSHOT") && !downloadedSnapshots.contains(dependency.toString()))) {
            try {
                System.out.println("Downloading " + dependency);
                String result = null;
                for (String repo : repositories) {
                    String repository = (repo.endsWith("/") ? repo : repo + "/");
                    try {
                        // 1. Try to get file directly, without getting any XMLs
                        HttpURLConnection connection = retrieve((HttpURLConnection) new URL(repository + dependency.toPath()).openConnection());
                        if (connection.getResponseCode() == 200) {
                            result = repository + dependency.toPath();
                            break;
                        } else {
                            // 2. Try to get latest from maven-metadata.xml
                            HttpURLConnection connection1 = retrieve((HttpURLConnection) new URL(repository + dependency.groupId().replace(".", "/") + "/" + dependency.artifactId() + "/" + dependency.version() + "/" + "maven-metadata.xml").openConnection());
                            if (connection1.getResponseCode() == 200) {
                                String version = Jsoup.parse(readString(connection1.getInputStream())).select("snapshotVersion:has(extension:contains(jar))").first().select("value").first().text();
                                result = repository + dependency.groupId().replace(".", "/") + "/" + dependency.artifactId() + "/" + dependency.version() + "/" + dependency.artifactId() + "-" + version + ".jar";
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace(System.out);
                    }
                }
                if (result == null) {
                    if (System.getProperty("libloader.debug") != null || !dependency.artifactId().endsWith("-bom")) {
                        System.out.println("No matching server found for " + dependency.groupId() + ":" + dependency.artifactId() + ":" + dependency.version() + "\nTried:");
                        repositories.forEach(r -> System.out.println("    " + (r.endsWith("/") ? r : r + "/") + dependency.toPath()));
                    }
                    ignore.ignore().add(dependency.toString());
                    return;
                } else if (System.getProperty("libloader.debug") != null) {
                    System.out.println("Found " + dependency);
                }
                final URLConnection con = retrieve((HttpURLConnection) new URL(result).openConnection());

                // write content of url to file
                final InputStream inputStream = con.getInputStream();
                path.getParentFile().mkdirs();
                path.createNewFile();
                final FileOutputStream fileOutputStream = new FileOutputStream(path);
                final byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
                fileOutputStream.close();
                inputStream.close();

                if (dependency.groupId().startsWith("dev.redicloud") && dependency.version().endsWith("SNAPSHOT")) {
                    downloadedSnapshots.add(dependency.toString());
                }
            } catch (Throwable t) {
                System.out.println("Failed to download " + dependency);
                t.printStackTrace(System.out);
            }
        }
        try {
            loader.load(path.toURI().toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        loaded.add(dependency.toString());
    }

    public void bootSuccess() {
        if (succeeded || ignoreInitialCount == ignore.ignore().size()) {
            return;
        }
        try {
            FileWriter writer = new FileWriter(new File(configuration.libraryFolderFile(), "ignore.json"));
            gson.toJson(ignore, writer);
            writer.flush();
            writer.close();
            succeeded = true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to save ignore.json", e);
        }
    }

}
