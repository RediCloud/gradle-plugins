package dev.redicloud.libloader.boot.apply;

import java.util.*;

import dev.redicloud.libloader.boot.model.*;

public interface ResourceLoader {
    String getName();
    Set<SelfDependency> getDependencies();
    Set<String> getRepositories();
}
