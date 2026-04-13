package application.bootstrap.menupipeline.util;

public enum TextAlign {

    /*
     * Horizontal alignment hint for label text within its element bounds.
     */

    LEFT, CENTER, RIGHT;

    public static TextAlign fromString(String s) {
        return switch (s.toLowerCase()) {
            case "left" -> LEFT;
            case "right" -> RIGHT;
            default -> CENTER;
        };
    }
}