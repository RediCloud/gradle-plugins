package dev.redicloud.libloader.boot.model;

import java.util.*;
import java.io.*;

public final class LibraryConfiguration {
    private final String mainClass;
    private final String libraryFolder;

    public LibraryConfiguration(final String mainClass, final String libraryFolder) {
        this.mainClass = mainClass;
        this.libraryFolder = libraryFolder;
    }

    public LibraryConfiguration() {
        this.mainClass = null;
        this.libraryFolder = null;
    }

    public String mainClass() {
        return this.mainClass;
    }

    public String libraryFolder() {
        return (System.getenv("LIBRARY_FOLDER") == null) ? Objects.requireNonNull(this.libraryFolder) : System.getenv("LIBRARY_FOLDER");
    }

    public File libraryFolderFile() {
        return new File(this.libraryFolder());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || this.getClass() != o.getClass()) return false;
        LibraryConfiguration that = (LibraryConfiguration) o;
        return Objects.equals(this.mainClass, that.mainClass) && Objects.equals(this.libraryFolder, that.libraryFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mainClass, this.libraryFolder);
    }

    @Override
    public String toString() {
        return "LibraryConfiguration{" +
                "mainClass='" + this.mainClass + '\'' +
                ", libraryFolder='" + this.libraryFolder + '\'' +
                '}';
    }
}
