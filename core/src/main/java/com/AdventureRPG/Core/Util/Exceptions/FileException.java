package com.AdventureRPG.Core.Util.Exceptions;

import java.io.File;

public final class FileException {

    private FileException() {
    } // prevents instantiation

    // Generic missing file exception
    public static class FileNotFoundException extends RuntimeException {
        public FileNotFoundException(File file) {
            super("File Exception: Could not find the file at: " + file.getAbsolutePath());
        }
    }

    // File failed to load exception
    public static class FileLoadException extends RuntimeException {
        public FileLoadException(File file, Throwable cause) {
            super("File Exception: Failed to load file at: " + file.getAbsolutePath(), cause);
        }
    }
}
