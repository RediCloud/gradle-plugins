package net.dustrean.libloader.boot.model;

import java.io.File;
import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord")
public final class LibraryConfiguration {
    private final String mainClass;
    private final String libraryFolder;

    public LibraryConfiguration(String mainClass, String libraryFolder, String jarLoaderClass) {
        this.mainClass = mainClass;
        this.libraryFolder = libraryFolder;
    }

    public String mainClass() {
        return mainClass;
    }

    public String libraryFolder() {
        return System.getenv("LIBRARY_FOLDER") == null ? libraryFolder : System.getenv("LIBRARY_FOLDER");
    }

    public File libraryFolderFile() {
        return new File(libraryFolder);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LibraryConfiguration that = (LibraryConfiguration) o;
        return Objects.equals(mainClass, that.mainClass) && Objects.equals(libraryFolder, that.libraryFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainClass, libraryFolder);
    }

    @Override
    public String toString() {
        return "LibraryConfiguration{" +
                "mainClass='" + mainClass + '\'' +
                ", libraryFolder='" + libraryFolder + '\'' +
                '}';
    }

}