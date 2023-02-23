package dev.redicloud.libloader.boot.apply;

import dev.redicloud.libloader.boot.model.SelfDependency;

import java.util.List;

public interface ResourceLoader {
    String getName();
    List<SelfDependency> getDependencies();
    List<String> getRepositories();
}
