package dev.redicloud.libloader.boot.model;

import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord")
public final class SelfDependency {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final ArrayList<SelfDependency> dependencies;

    public SelfDependency(String groupId, String artifactId, String version,
                          ArrayList<SelfDependency> dependencies) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.dependencies = dependencies;
    }

    @Override
    public String toString() {
        return groupId + ":" + artifactId + ":" + version;
    }

    public String toPath() {
        return groupId.replace(".", "/") + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar";
    }

    public String groupId() {
        return groupId;
    }

    public String artifactId() {
        return artifactId;
    }

    public String version() {
        return version;
    }

    public ArrayList<SelfDependency> dependencies() {
        return dependencies;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SelfDependency) obj;
        return Objects.equals(this.groupId, that.groupId) &&
                Objects.equals(this.artifactId, that.artifactId) &&
                Objects.equals(this.version, that.version) &&
                Objects.equals(this.dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version, dependencies);
    }

}
