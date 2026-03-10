package com.internal.bootstrap.menupipeline.element;

public enum StackDirection {
    NONE,
    VERTICAL,
    HORIZONTAL;

    public static StackDirection fromString(String s) {
        return switch (s.toLowerCase()) {
            case "vertical" -> VERTICAL;
            case "horizontal" -> HORIZONTAL;
            default -> NONE;
        };
    }
}