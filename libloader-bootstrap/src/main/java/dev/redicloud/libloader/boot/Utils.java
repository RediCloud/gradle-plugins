package dev.redicloud.libloader.boot;

import dev.redicloud.libloader.boot.model.SelfDependency;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import static dev.redicloud.libloader.boot.Bootstrap.*;

public class Utils {
    public record Pair<F, S>(F first, S second) {}
    public static Pair<List<SelfDependency>, List<String>> walkThroughFS(URI uri) {
        List<SelfDependency> dependencies = new ArrayList<>();
        List<String> repositories = new ArrayList<>();
        try {
            FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            Stream<Path> walk = Files.walk(fileSystem.getPath("depends"));
            for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
                Path path = it.next();
                if (path.getFileName().toString().startsWith("dependencies")) {
                    dependencies.addAll(gson.fromJson(new InputStreamReader(path.toUri().toURL().openStream()), DEPENDENCY_TYPE));
                } else if (path.getFileName().toString().startsWith("repositories")) {
                    repositories.addAll(gson.fromJson(new InputStreamReader(path.toUri().toURL().openStream()), REPOSITORY_TYPE));
                }
            }
            walk.close();
            fileSystem.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Pair<>(dependencies, repositories);
    }
}
