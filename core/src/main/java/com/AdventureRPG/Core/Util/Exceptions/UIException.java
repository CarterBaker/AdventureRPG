package com.AdventureRPG.Core.Util.Exceptions;

public class UIException {

    private UIException() {
    } // prevents instantiation

    // Generic / unknown menu type requested
    public static class UnknownMenuException extends RuntimeException {
        public UnknownMenuException(String menuName) {
            super("UI Exception: Unknown menu type: " + menuName);
        }
    }
}
