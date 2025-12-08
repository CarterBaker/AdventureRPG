package com.AdventureRPG.core.util.Exceptions;

public final class CoreException {

    // Engine Frame \\

    public static class DuplicateEngineFrameDetected extends ExceptionEngine {

        public DuplicateEngineFrameDetected(String message) {
            super(message);
        }

        public DuplicateEngineFrameDetected(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class FatalErrorDetected extends ExceptionEngine {

        public FatalErrorDetected(String message) {
            super(message);
        }

        public FatalErrorDetected(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class GameStateException extends ExceptionEngine {

        public GameStateException(String message) {
            super(message);
        }

        public GameStateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class OutOfOrderException extends ExceptionEngine {

        public OutOfOrderException(String message) {
            super(message);
        }

        public OutOfOrderException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class DuplicateSystemFrameDetected extends ExceptionEngine {

        public DuplicateSystemFrameDetected(String message) {
            super(message);
        }

        public DuplicateSystemFrameDetected(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
