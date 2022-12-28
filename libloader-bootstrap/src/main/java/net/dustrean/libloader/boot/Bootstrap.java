package net.dustrean.libloader.boot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.dustrean.libloader.boot.loaders.DefaultJarLoader;
import net.dustrean.libloader.boot.model.IgnoreConfiguration;
import net.dustrean.libloader.boot.model.LibraryConfiguration;
import net.dustrean.libloader.boot.model.SelfDependency;
import org.jsoup.Jsoup;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/**
 * The on runtime loader.
 * After finishing, it will try to run the main class.
 */
public class Bootstrap {
    private final ArrayList<String> loaded = new ArrayList<>();
    private final Gson gson;
    private final HashMap<String, JsonArray> repositories = new HashMap<>();
    private IgnoreConfiguration ignore;
    private LibraryConfiguration configuration;
    private int ignoreInitialCount;
    private boolean succeeded = false;

    public Bootstrap() {
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    public static void main(String[] args) throws IOException, URISyntaxException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.apply(new DefaultJarLoader());
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
        if (conn.getURL().getHost().equalsIgnoreCase("repo.dustrean.net"))
            conn.addRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((System.getenv("DUSTREAN_REPO_USERNAME") + ":" + System.getenv("DUSTREAN_REPO_PASSWORD")).getBytes()));
        return conn;
    }

    private static String readString(InputStream inputStream) throws IOException {
        int bufferSize = 1024;
        char[] buffer = new char[bufferSize];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        for (int numRead; (numRead = in.read(buffer, 0, buffer.length)) > 0; ) {
            out.append(buffer, 0, numRead);
        }
        return out.toString();
    }

    public void apply(JarLoader loader) throws IOException, URISyntaxException {
        apply(loader, Bootstrap.class.getClassLoader(), Bootstrap.class.getClassLoader());
    }

    public void apply(JarLoader loader, ClassLoader configClassLoader, ClassLoader... dependsClassLoader) throws IOException, URISyntaxException {
        // Load Configuration
        try {
            configuration = gson.fromJson(new InputStreamReader(Objects.requireNonNull(configClassLoader.getResourceAsStream("depends/config.json"))), LibraryConfiguration.class);
        } catch (Throwable e) {
            configuration = new LibraryConfiguration();
        }
        configuration.libraryFolderFile().mkdirs();
        File ignoreFile = new File(configuration.libraryFolder(), "ignore.json");
        if (!ignoreFile.exists()) {
            FileWriter ignoreWrite = new FileWriter(ignoreFile);
            ignoreFile.createNewFile();
            gson.toJson(new IgnoreConfiguration(new ArrayList<>()), ignoreWrite);
            ignoreWrite.flush();
            ignoreWrite.close();
        }
        this.ignore = gson.fromJson(new FileReader(ignoreFile), IgnoreConfiguration.class);
        HashMap<String, JsonArray> dependencies = new HashMap<>();
        for (ClassLoader classLoader : dependsClassLoader) {
            URI uri = classLoader.getResource("depends").toURI();
            FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            Stream<Path> walk = Files.walk(fileSystem.getPath("depends"));
            for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
                Path path = it.next();
                if (path.getFileName().toString().startsWith("dependencies")) {
                    dependencies.put(path.getFileName().toString().substring(13), gson.fromJson(new InputStreamReader(path.toUri().toURL().openStream()), JsonArray.class));
                } else if (path.getFileName().toString().startsWith("repositories")) {
                    repositories.put(path.getFileName().toString().substring(13), gson.fromJson(new InputStreamReader(path.toUri().toURL().openStream()), JsonArray.class));
                }
            }
            walk.close();
            fileSystem.close();
        }
        ignoreInitialCount = ignore.ignore().size();
        dependencies.forEach((_dependencies, array) -> array.forEach(dependency -> resolve(gson.fromJson(dependency.getAsJsonObject(), SelfDependency.class), loader, _dependencies)));
    }

    public void resolve(SelfDependency dependency, JarLoader loader, String key) {
        dependency.dependencies().forEach((child_depend) -> {
            if (!loaded.contains(child_depend.toString())) {
                resolve(child_depend, loader, key);
            }
        });
        if (ignore.ignore().contains(dependency.toString())) return;
        File path = new File(configuration.libraryFolderFile(), dependency.toPath());
        if (!path.exists() || (dependency.groupId().startsWith("net.dustrean") && dependency.version().endsWith("SNAPSHOT"))) {
            try {
                System.out.println("Downloading " + dependency);
                String result = null;
                for (JsonElement repo : repositories.get(key)) {
                    try {
                        // 1. Try to get file directly, without getting any XMLs
                        String repository = (repo.getAsString().endsWith("/") ? repo.getAsString() : repo.getAsString() + "/");
                        HttpURLConnection connection = retrieve((HttpURLConnection) new URL(repository + dependency.toPath()).openConnection());
                        if (connection.getResponseCode() == 200) {
                            result = repository + dependency.toPath();
                            break;
                        } else {
                            // 2. Try to get latest from maven-metadata.xml
                            HttpURLConnection connection1 = retrieve((HttpURLConnection) new URL(
                                    repository + dependency.groupId().replace(".", "/") + "/" +
                                            dependency.artifactId() + "/" + dependency.version() + "/" +
                                            "maven-metadata.xml"
                            ).openConnection());
                            if (connection1.getResponseCode() == 200) {
                                String version = Jsoup.parse(readString(connection1.getInputStream())).select("snapshotVersion:has(extension:contains(jar))").first().select("value").first().text();
                                result = repository + dependency.groupId().replace(".", "/") + "/" + dependency.artifactId() + "/" + dependency.version() + "/" + dependency.artifactId() + "-" + version + ".jar";
                                break;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (result == null) {
                    System.out.println("No matching server found for " + dependency.groupId() + ":" + dependency.artifactId() + ":" + dependency.version() + "\nTried:");
                    repositories.get(key).forEach(r1 -> {
                        String r = r1.getAsString();
                        System.out.println("    " + (r.endsWith("/") ? r : r + "/") + dependency.toPath());
                    });
                    ignore.ignore().add(dependency.groupId() + ":" + dependency.artifactId() + ":" + dependency.version());
                    return;
                } else System.out.println("Found " + dependency);
                URLConnection con = retrieve((HttpURLConnection) new URL(result).openConnection());

                // write content of url to file
                InputStream inputStream = con.getInputStream();
                path.getParentFile().mkdirs();
                path.createNewFile();
                FileOutputStream fileOutputStream = new FileOutputStream(path);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = inputStream.read(buffer)) > 0) {
                    fileOutputStream.write(buffer, 0, length);
                }
                fileOutputStream.close();
                inputStream.close();
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
        if (succeeded || ignoreInitialCount <= ignore.ignore().size()) return;
        try {
            FileWriter writer = new FileWriter(new File(configuration.libraryFolderFile(), "ignore.json"));
            gson.toJson(ignore, writer);
            writer.flush();
            writer.close();
            succeeded = true;
        } catch (Exception ignored) {
        }
    }
}
