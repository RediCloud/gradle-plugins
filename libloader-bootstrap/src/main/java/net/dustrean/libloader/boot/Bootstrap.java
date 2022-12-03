package net.dustrean.libloader.boot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import net.dustrean.libloader.boot.model.IgnoreConfiguration;
import net.dustrean.libloader.boot.model.LibraryConfiguration;
import net.dustrean.libloader.boot.model.SelfDependency;
import org.jsoup.Jsoup;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Objects;
import java.util.jar.JarFile;

/**
 * The on runtime loader.
 * After finishing, it will try to run the main class.
 */
public class Bootstrap {
    private static JsonArray repositories;
    private static final ArrayList<String> loaded = new ArrayList<>();
    private static IgnoreConfiguration ignore;
    private static LibraryConfiguration configuration;
    private static Gson gson;
    private static JarLoader loader;

    public static void main(String[] args) throws IOException {
        apply();
        // Run Main Class
        try {
            Class.forName(configuration.mainClass()).getDeclaredMethod("main", String[].class).invoke(null, (Object) args);
        } catch (ClassNotFoundException e) {
            System.out.println("Error while getting main class. Check your gradle build configuration.");
        } catch (NoSuchMethodException e) {
            System.out.println("Main method missing in class " + configuration.mainClass());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void apply() throws IOException {
        gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        // Load Configuration
        configuration = gson.fromJson(new InputStreamReader(Objects.requireNonNull(Bootstrap.class.getClassLoader().getResourceAsStream("config.json"))), LibraryConfiguration.class);
        try {
            loader = (JarLoader) Class.forName(configuration.jarLoaderClass()).getConstructor()
                            .newInstance();
        } catch (Exception e) {
            System.out.println("Loader init error:");
            e.printStackTrace(System.out);
        }
        configuration.libraryFolderFile().mkdirs();
        File ignore = new File(configuration.libraryFolder(), "ignore.json");
        if (!ignore.exists()) {
            FileWriter ignoreWrite = new FileWriter(ignore);
            ignore.createNewFile();
            gson.toJson(new IgnoreConfiguration(new ArrayList<>()), ignoreWrite);
            ignoreWrite.flush();
            ignoreWrite.close();
        }
        Bootstrap.ignore = gson.fromJson(new FileReader(ignore), IgnoreConfiguration.class);
        JsonArray dependencies = gson.fromJson(new InputStreamReader(Objects.requireNonNull(Bootstrap.class.getClassLoader().getResourceAsStream("dependencies.json"))), JsonArray.class);
        repositories = gson.fromJson(new InputStreamReader(Objects.requireNonNull(Bootstrap.class.getClassLoader().getResourceAsStream("repositories.json"))), JsonArray.class);
        int initNum = Bootstrap.ignore.ignore().size();
        dependencies.forEach((dependency) -> resolve(gson.fromJson(dependency.getAsJsonObject(), SelfDependency.class)));
    }

    public static void resolve(SelfDependency dependency) {
        dependency.dependencies().forEach((child_depend) -> {
            if (!loaded.contains(child_depend.toString())) {
                resolve(child_depend);
            }
        });
        if (ignore.ignore().contains(dependency.toString())) return;
        File path = new File(configuration.libraryFolderFile(), dependency.toPath());
        if (!path.exists()) {
            try {
                System.out.println("Downloading " + dependency);
                String result = null;
                for (JsonElement repo : repositories) {
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
                    repositories.forEach(r1 -> {
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
    private static boolean succeeded = false;
    public static void bootSuccess() {
        if (succeeded) return;
        try {
            FileWriter writer = new FileWriter(new File(configuration.libraryFolderFile(), "ignore.json"));
            gson.toJson(ignore, writer);
            writer.flush();
            writer.close();
            succeeded = true;
        } catch (Exception ignored) {}
    }

    private static HttpURLConnection retrieve(HttpURLConnection conn) {
        if (conn.getURL().getHost().equalsIgnoreCase("repo.dustrean.net"))
            conn.addRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((System.getenv("dustreanUsername") + ":" + System.getenv("dustreanPassword")).getBytes()));
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
}
