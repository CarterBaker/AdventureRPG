package editor.runtime.treeviewer;

public class TreeViewerSetting {

    /*
     * Constants used only by TreeViewerContext — its materials and render
     * order, the frame its camera holds, its menu, and the names its layers
     * and status line give each layer of the preview.
     */

    // Materials
    public static final String MATERIAL_BARK = "editor/TreeViewerBarkMaterial";
    public static final String MATERIAL_LEAF = "editor/TreeViewerLeafMaterial";
    public static final String MATERIAL_IMPOSTOR = "editor/TreeViewerImpostorMaterial";

    // Impostor — the uniforms placing the impostor's quad
    public static final String UNIFORM_IMPOSTOR_CENTER_Y = "u_impostorCenterY";
    public static final String UNIFORM_IMPOSTOR_RADIUS = "u_impostorRadius";
    public static final String UNIFORM_IMPOSTOR_LAYER = "u_impostorLayer";

    // Render Order
    public static final int DEPTH_TREE = 0;

    // Frame — its middle a share of the tree's height up
    public static final float FRAME_HEIGHT_SHARE = 0.5f;

    // Menus
    public static final String MENU_VIEWER = "editor/TreeViewer/Viewer";
    public static final int ENTRY_STATUS = 0;

    // Layers — keys and names, a level's own and the shared tree's
    public static final String LAYER_KEY_LEVEL = "level_";
    public static final String LAYER_KEY_SHARED = "shared";
    public static final String LAYER_NAME_FULL = "LOD 0 (Full)";
    public static final String LAYER_NAME_LEVEL = "LOD ";
    public static final String LAYER_NAME_SHARED = "Impostor";

    // Text
    public static final String STATUS_PREFIX = "Tree Viewer  |  ";
    public static final String STATUS_NONE = "No tree selected in the Hierarchy";
    public static final String STATUS_HIDDEN = "Every layer is hidden";
    public static final String STATUS_NO_IMPOSTOR = " — no impostor baked yet";
    public static final String STATUS_BAKING = "Baking impostor: ";
    public static final String STATUS_QUEUED = " queued";
    public static final String STATUS_SEPARATOR = "  ·  ";
    public static final String STATUS_VERTICES = " vertices";
    public static final String STATUS_AGE = "Age ";
    public static final String STATUS_SEED = "Seed ";
    public static final String STATUS_PERCENT_SIGN = "%";
    public static final float STATUS_PERCENT = 100f;
}
