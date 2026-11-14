package editor.runtime.treeviewer;

public class TreeViewerSetting {

    /*
     * Constants used only by TreeViewerContext — its materials and render
     * order, the frame its camera holds, its menu, and the names its status
     * line gives each tier of the row, in TreeDetail order.
     */

    // Materials
    public static final String MATERIAL_BARK = "editor/TreeViewerBarkMaterial";
    public static final String MATERIAL_LEAF = "editor/TreeViewerLeafMaterial";

    // Render Order
    public static final int DEPTH_TREE = 0;

    // Frame — its middle a share of the tree's height up; one tier, or the whole row
    public static final float FRAME_HEIGHT_SHARE = 0.5f;
    public static final int FRAME_ALL = -1;

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
