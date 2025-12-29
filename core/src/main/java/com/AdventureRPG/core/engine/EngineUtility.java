package com.AdventureRPG.core.engine;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

abstract class EngineUtility {

    // Internal
    private final String systemName = getClass().getSimpleName();

    // Debug \\

    protected final void debug() {
        debug("");
    }

    protected final void debug(Object input) {
        System.out.println("[" + systemName + "] " + String.valueOf(input));
    }

    protected final void timeStampDebug(Object input) {
        debug(timeStamp() + String.valueOf(input));
    }

    // Log \\

    protected final void log(Object input) {
        System.out.println(String.valueOf(input));
    }

    protected final void error(Object input) {
        System.err.println(String.valueOf(input));
    }

    protected final void timeStampLog(Object input) {
        System.out.println(timeStamp() + String.valueOf(input));
    }

    // Exception Handling \\

    protected final <T> T throwException() {
        return throwException("Unspecified fatal error", null);
    }

    protected final <T> T throwException(String message) {
        return throwException(message, null);
    }

    protected final <T> T throwException(Throwable cause) {
        return throwException("Unspecified fatal error", cause);
    }

    protected final <T> T throwException(String message, Throwable cause) {
        logFatal(message, cause);
        throw new InternalException("[" + systemName + "] " + message, cause);
    }

    protected final <T> T throwException(Object input) {
        return throwException(String.valueOf(input), null);
    }

    protected final <T> T throwException(Object input, Throwable cause) {
        return throwException(String.valueOf(input), cause);
    }

    // Utility \\

    private final String timeStamp() {
        String time = LocalTime.now().format(TIME_FORMAT);
        return "[" + time + "] ";
    }

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    public class InternalException extends RuntimeException {

        public InternalException(String message) {
            super(message);
        }

        public InternalException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private void logFatal(String message, Throwable cause) {

        error("========================================");
        error("FATAL ENGINE EXCEPTION");
        error("System : " + systemName);
        error("Time   : " + timeStamp());
        error("Message: " + message);

        if (cause != null) {
            error("Cause:");
            cause.printStackTrace(System.err);
        }

        error("========================================");
    }
}
