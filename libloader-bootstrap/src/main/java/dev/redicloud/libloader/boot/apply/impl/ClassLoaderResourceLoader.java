package dev.redicloud.libloader.boot.apply.impl;

import dev.redicloud.libloader.boot.apply.*;

import java.util.*;

import dev.redicloud.libloader.boot.model.*;

import java.net.*;

import dev.redicloud.libloader.boot.*;

public class ClassLoaderResourceLoader implements ResourceLoader {
    private final String name;
    private final Set<SelfDependency> dependencies;
    private final Set<String> repositories;

    public ClassLoaderResourceLoader(final String name, final ClassLoader classLoader) {
        this.name = name;
        try {
            final URI uri = Objects.requireNonNull(classLoader.getResource("depends")).toURI();
            final Pair<Set<SelfDependency>, Set<String>> pair = Utils.walkThroughFS(uri);
            this.dependencies = pair.first();
            this.repositories = pair.second();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Set<SelfDependency> getDependencies() {
        return this.dependencies;
    }

    @Override
    public Set<String> getRepositories() {
        return this.repositories;
    }
}
