package dev.redicloud.libloader.boot.apply.impl;

import dev.redicloud.libloader.boot.model.SelfDependency;
import dev.redicloud.libloader.boot.Utils;
import dev.redicloud.libloader.boot.apply.ResourceLoader;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ClassLoaderResourceLoader implements ResourceLoader {

    private final String name;

    private final List<SelfDependency> dependencies;
    private final List<String> repositories;


    public ClassLoaderResourceLoader(String name, ClassLoader classLoader) {
        this.name = name;
        try {
            URI uri = classLoader.getResource("depends").toURI();
            Utils.Pair<List<SelfDependency>, List<String>> pair = Utils.walkThroughFS(uri);
            dependencies = pair.first();
            repositories = pair.second();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
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
