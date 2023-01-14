package net.dustrean.libloader.boot.apply.impl;

import net.dustrean.libloader.boot.Utils;
import net.dustrean.libloader.boot.apply.ResourceLoader;
import net.dustrean.libloader.boot.model.SelfDependency;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
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
