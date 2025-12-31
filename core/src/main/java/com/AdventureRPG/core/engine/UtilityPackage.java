package com.AdventureRPG.core.engine;

public abstract class UtilityPackage {

    // Internal \\

    protected static final EngineUtility ENGINE = new EngineUtility() {
    };

    // Debug \\

    protected static void debug() {
        ENGINE.debug();
    }

    protected static void debug(Object input) {
        ENGINE.debug(input);
    }

    protected static void timeStampDebug(Object input) {
        ENGINE.timeStampDebug(input);
    }

    // Log \\

    protected static void log(Object input) {
        ENGINE.log(input);
    }

    protected static void errorLog(Object input) {
        ENGINE.errorLog(input);
    }

    protected static void timeStampLog(Object input) {
        ENGINE.timeStampLog(input);
    }

    // Exception Handling \\

    protected static <T> T throwException() {
        return ENGINE.throwException();
    }

    protected static <T> T throwException(String message) {
        return ENGINE.throwException(message);
    }

    protected static <T> T throwException(Throwable cause) {
        return ENGINE.throwException(cause);
    }

    protected static <T> T throwException(String message, Throwable cause) {
        return ENGINE.throwException(message, cause);
    }

    protected static <T> T throwException(Object input) {
        return ENGINE.throwException(input);
    }

    protected static <T> T throwException(Object input, Throwable cause) {
        return ENGINE.throwException(input, cause);
    }
}
