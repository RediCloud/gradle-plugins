package dev.redicloud.libloader.boot.model;

import java.util.*;

public final class IgnoreConfiguration {
    private final ArrayList<String> ignore;

    public IgnoreConfiguration(final ArrayList<String> ignore) {
        this.ignore = ignore;
    }

    public ArrayList<String> ignore() {
        return this.ignore;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final IgnoreConfiguration that = (IgnoreConfiguration) obj;
        return Objects.equals(this.ignore, that.ignore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.ignore);
    }

    @Override
    public String toString() {
        return "IgnoreConfiguration[ignore=" + this.ignore + ']';
    }
}
