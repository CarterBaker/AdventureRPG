package com.internal.bootstrap.menupipeline.element;

public enum TextAlign {
    LEFT, CENTER, RIGHT;

    public static TextAlign fromString(String s) {
        return switch (s.toLowerCase()) {
            case "left" -> LEFT;
            case "right" -> RIGHT;
            default -> CENTER;
        };
    }
}