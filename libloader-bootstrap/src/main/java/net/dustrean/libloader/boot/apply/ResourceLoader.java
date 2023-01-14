package net.dustrean.libloader.boot.apply;

import net.dustrean.libloader.boot.model.SelfDependency;

import java.util.List;

public interface ResourceLoader {
    String getName();
    List<SelfDependency> getDependencies();
    List<String> getRepositories();
}
