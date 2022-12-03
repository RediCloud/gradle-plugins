package net.dustrean.libloader.boot.model;

import java.io.File;
import java.util.Objects;

@SuppressWarnings("ClassCanBeRecord")
public final class LibraryConfiguration {
    private final String mainClass;
    private final String libraryFolder;

    public LibraryConfiguration(String mainClass, String libraryFolder) {
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
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (LibraryConfiguration) obj;
        return Objects.equals(this.mainClass, that.mainClass) &&
                Objects.equals(this.libraryFolder, that.libraryFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mainClass, libraryFolder);
    }

    @Override
    public String toString() {
        return "LibraryConfiguration[" +
                "mainClass=" + mainClass + ", " +
                "libraryFolder=" + libraryFolder + ']';
    }

}