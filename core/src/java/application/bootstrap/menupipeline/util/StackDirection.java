package application.bootstrap.menupipeline.util;

public enum StackDirection {

    /*
     * Controls how child elements are laid out inside a container.
     * NONE uses anchor/pivot positioning. VERTICAL and HORIZONTAL stack
     * children sequentially along the given axis.
     */

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