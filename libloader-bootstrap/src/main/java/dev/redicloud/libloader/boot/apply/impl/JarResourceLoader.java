package dev.redicloud.libloader.boot.apply.impl;

import dev.redicloud.libloader.boot.model.SelfDependency;
import dev.redicloud.libloader.boot.Utils;
import dev.redicloud.libloader.boot.apply.ResourceLoader;

import java.io.File;
import java.net.URI;
import java.util.List;

public class JarResourceLoader implements ResourceLoader {
    private final String name;

    private final List<SelfDependency> dependencies;
    private final List<String> repositories;

    public JarResourceLoader(String name, File file) {
        this.name = name;
        Utils.Pair<List<SelfDependency>, List<String>> pair = Utils.walkThroughFS(URI.create("jar:" + file.toURI()));
        dependencies = pair.first();
        repositories = pair.second();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<SelfDependency> getDependencies() {
        return dependencies;
    }

    @Override
    public List<String> getRepositories() {
        return repositories;
    }
}
