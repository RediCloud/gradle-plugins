package dev.redicloud.libloader.boot.model;

import java.util.*;

@SuppressWarnings("ClassCanBeRecord")
public final class SelfDependency {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final ArrayList<SelfDependency> dependencies;

    public SelfDependency(
            final String groupId,
            final String artifactId,
            final String version,
            final ArrayList<SelfDependency> dependencies
    ) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.dependencies = dependencies;
    }

    @Override
    public String toString() {
        return this.groupId + ":" + this.artifactId + ":" + this.version;
    }

    public String toPath() {
        return this.groupId.replace(".", "/") + "/" + this.artifactId + "/" + this.version + "/" + this.artifactId + "-" + this.version + ".jar";
    }

    public String groupId() {
        return this.groupId;
    }

    public String artifactId() {
        return this.artifactId;
    }

    public String version() {
        return this.version;
    }

    public ArrayList<SelfDependency> dependencies() {
        return this.dependencies;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final SelfDependency that = (SelfDependency) obj;
        return Objects.equals(this.groupId, that.groupId) && Objects.equals(this.artifactId, that.artifactId) && Objects.equals(this.version, that.version) && Objects.equals(this.dependencies, that.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.groupId, this.artifactId, this.version, this.dependencies);
    }
}
