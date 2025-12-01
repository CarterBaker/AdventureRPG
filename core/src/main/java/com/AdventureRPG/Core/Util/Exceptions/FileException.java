package com.AdventureRPG.Core.Util.Exceptions;

public final class FileException {

    public static class FileNotFoundException extends ExceptionEngine {

        public FileNotFoundException(String message) {
            super(message);
        }

        public FileNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class FileReadException extends ExceptionEngine {

        public FileReadException(String message) {
            super(message);
        }

        public FileReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class InvalidDirectoryException extends ExceptionEngine {

        public InvalidDirectoryException(String message) {
            super(message);
        }

        public InvalidDirectoryException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
