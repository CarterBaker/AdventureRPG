package editor.runtime.colorwheel;

public class ColorWheelSetting {

    /*
     * Constants used only by ColorWheelContext — its menu and entry points,
     * the wheel's image size and the parts drawn on it (the hue ring, the
     * triangle inside it and the two markers), each as a share of the image,
     * and the parts a press may begin on.
     */

    // Menus
    public static final String MENU_COLOR_WHEEL = "editor/ColorWheel/Wheel";

    // Entry Points
    public static final int ENTRY_TITLE = 0;
    public static final int ENTRY_WHEEL = 1;
    public static final int ENTRY_ROWS = 2;

    // Wheel — the image's side in texels, the ring's outer and inner radius and the triangle's corner radius as
    // shares of half the side
    public static final int WHEEL_SIZE = 256;
    public static final float WHEEL_RING_OUTER = 0.99f;
    public static final float WHEEL_RING_INNER = 0.82f;
    public static final float WHEEL_TRIANGLE_RADIUS = 0.78f;
    public static final float WHEEL_EDGE_SOFTNESS = 1f;
    public static final int WHEEL_CORNER_COUNT = 3;
    public static final float WHEEL_TRIANGLE_HEIGHT_SHARE = 1.5f;

    // Markers — radius and outline width in texels, and the brightness past which the triangle's marker turns dark
    public static final float WHEEL_MARKER_RADIUS = 6f;
    public static final float WHEEL_MARKER_WIDTH = 1.5f;
    public static final float WHEEL_MARKER_DARK_ABOVE = 0.6f;
    public static final float WHEEL_MARKER_DARK_SATURATION = 0.5f;

    // Parts — what a press on the wheel began on
    public static final int PART_NONE = 0;
    public static final int PART_RING = 1;
    public static final int PART_TRIANGLE = 2;

    // Corners — the white and black corners' turns from the hue corner, in turns
    public static final float WHEEL_WHITE_TURN = 1f / 3f;
    public static final float WHEEL_BLACK_TURN = 2f / 3f;
}
