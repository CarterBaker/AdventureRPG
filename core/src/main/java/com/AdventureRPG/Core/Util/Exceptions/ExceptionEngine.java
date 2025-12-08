package com.AdventureRPG.core.util.Exceptions;

public abstract class ExceptionEngine extends RuntimeException {

    protected ExceptionEngine(String message) {
        super(format(message));
    }

    protected ExceptionEngine(String message, Throwable cause) {
        super(format(message), cause);
    }

    private static String format(String message) {

        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String className = "Unknown";

        if (stack.length > 4) {
            className = stack[4].getClassName();
            className = className.substring(className.lastIndexOf('.') + 1);
        }

        return "[" + className + "] " + message;
    }
}
