package editor.runtime.treeviewer;

import engine.input.Buttons;

public class TreeViewerSetting {

    /*
     * Constants used only by TreeViewerContext — its materials and render
     * order, its orbit camera and mouse binding, its menu, and the names its
     * status line gives each tier of the row, in TreeDetail order.
     */

    // Materials
    public static final String MATERIAL_BARK = "editor/TreeViewerBarkMaterial";
    public static final String MATERIAL_LEAF = "editor/TreeViewerLeafMaterial";

    // Render Order
    public static final int DEPTH_TREE = 0;

    // Camera — the frame's centre a share of the tree's height up, the distances in sizes of what is framed
    public static final float ORBIT_TARGET_HEIGHT_SHARE = 0.5f;
    public static final float ORBIT_DISTANCE_DEFAULT = 1.6f;
    public static final float ORBIT_DISTANCE_MIN = 0.2f;
    public static final float ORBIT_DISTANCE_MAX = 12f;
    public static final float ORBIT_ZOOM_STEP = 0.1f;
    public static final float ORBIT_INITIAL_YAW_DEGREES = 25f;
    public static final float ORBIT_INITIAL_PITCH_DEGREES = 12f;
    public static final int FRAME_ALL = -1;

    // Mouse
    public static final int BUTTON_ORBIT = Buttons.RIGHT;

    // Menus
    public static final String MENU_VIEWER = "editor/TreeViewer/Viewer";
    public static final int ENTRY_STATUS = 0;

    // Text
    public static final String STATUS_PREFIX = "Tree Viewer  |  ";
    public static final String STATUS_NONE = "No tree selected in the Tree Planner";
    public static final String STATUS_TIER_SEPARATOR = "  ·  ";
    public static final String STATUS_VERTICES = " vertices, left to right";
    public static final String[] TIER_NAMES = { "Full", "Model", "Sketch", "Outline" };
}
