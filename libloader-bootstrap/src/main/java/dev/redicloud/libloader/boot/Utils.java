package dev.redicloud.libloader.boot;

import java.net.*;

import dev.redicloud.libloader.boot.model.*;

import java.io.*;
import java.nio.file.*;
import java.util.stream.*;
import java.util.*;

public class Utils {
    public static Pair<Set<SelfDependency>, Set<String>> walkThroughFS(final URI uri) {
        final List<SelfDependency> dependencies = new ArrayList<>();
        final List<String> repositories = new ArrayList<>();
        try {
            final FileSystem fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
            final Stream<Path> walk = Files.walk(fileSystem.getPath("depends"));
            for (Iterator<Path> it = walk.iterator(); it.hasNext(); ) {
                Path path = it.next();
                if (path.getFileName().toString().startsWith("dependencies")) {
                    dependencies.addAll(Bootstrap.gson.fromJson(new InputStreamReader(path.toUri().toURL().openStream()), Bootstrap.DEPENDENCY_TYPE));
                } else {
                    if (!path.getFileName().toString().startsWith("repositories")) {
                        continue;
                    }
                    repositories.addAll(Bootstrap.gson.fromJson(new InputStreamReader(path.toUri().toURL().openStream()), Bootstrap.REPOSITORY_TYPE));
                }
            }
            walk.close();
            fileSystem.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new Pair<>(new HashSet<>(dependencies), new HashSet<>(repositories));
    }
}
