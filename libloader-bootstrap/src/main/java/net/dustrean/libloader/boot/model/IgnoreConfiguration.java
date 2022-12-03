package net.dustrean.libloader.boot.model;

import java.util.ArrayList;
import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord")
public final class IgnoreConfiguration {
    private final ArrayList<String> ignore;

    public IgnoreConfiguration(ArrayList<String> ignore) {
        this.ignore = ignore;
    }

    public ArrayList<String> ignore() {
        return ignore;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (IgnoreConfiguration) obj;
        return Objects.equals(this.ignore, that.ignore);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ignore);
    }

    @Override
    public String toString() {
        return "IgnoreConfiguration[" +
                "ignore=" + ignore + ']';
    }

}
