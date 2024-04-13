package dev.redicloud.libloader.boot.apply.impl;

import dev.redicloud.libloader.boot.apply.*;

import java.util.*;

import dev.redicloud.libloader.boot.model.*;

import java.io.*;
import java.net.*;

import dev.redicloud.libloader.boot.*;

public class JarResourceLoader implements ResourceLoader {
    private final String name;
    private final Set<SelfDependency> dependencies;
    private final Set<String> repositories;

    public JarResourceLoader(final String name, final File file) {
        this.name = name;
        final Pair<Set<SelfDependency>, Set<String>> pair = Utils.walkThroughFS(URI.create("jar:" + file.toURI()));
        this.dependencies = pair.first();
        this.repositories = pair.second();
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
