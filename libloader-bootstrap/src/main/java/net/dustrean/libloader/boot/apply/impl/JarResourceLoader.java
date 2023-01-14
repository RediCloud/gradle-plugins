package net.dustrean.libloader.boot.apply.impl;

import net.dustrean.libloader.boot.Utils;
import net.dustrean.libloader.boot.apply.ResourceLoader;
import net.dustrean.libloader.boot.model.SelfDependency;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

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
