package editor.runtime.charactereditor;

public class CharacterEditorSetting {

    /*
     * Constants used only by CharacterLookContext, CharacterModelingContext
     * and the viewport systems they own — the toolbar menu the Character
     * Appearance, Build and Equipment tabs share, the one every modelling
     * tab shares and the status entry both share, the size of the box shown
     * for a held anchor, and the mesh modes' box selection, held shortcuts
     * and the overlay their vertices and edges are drawn with. Its panels'
     * rows are the docker windows'.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/CharacterEditor/Toolbar";
    public static final String MENU_MODELING_TOOLBAR = "editor/CharacterMeshEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;

    // Markers — the box shown for a held anchor, in blocks
    public static final float HELD_ANCHOR_MARKER_SIZE = 0.06f;

    // Mesh Input — the held shortcuts a selection, a transform and a stroke read, named in
    // shortcuts/CharacterMeshEditor
    public static final String SHORTCUT_ADD_SELECTION = "CharacterMeshEditor/AddSelection";
    public static final String SHORTCUT_SUBTRACT_SELECTION = "CharacterMeshEditor/SubtractSelection";
    public static final String SHORTCUT_SNAP = "CharacterMeshEditor/Snap";
    public static final String SHORTCUT_INVERT_STROKE = "CharacterMeshEditor/InvertStroke";

    // Mesh Input — the pointer distance in pixels a press travels before it draws a box
    public static final float DRAG_PIXELS = 4f;

    // Mesh View — a point is hidden by a face nearer the camera than it by more than the bias, in blocks
    public static final float OCCLUSION_BIAS = 0.003f;
    public static final float OCCLUSION_EPSILON = 0.000001f;

    // Mesh View — occluding faces are binned into square cells of the window this many pixels wide
    public static final int OCCLUSION_CELL_PIXELS = 24;

    // Mesh View — a vertex's visibility this frame: not yet tested, seen, or hidden behind a face
    public static final byte VISIBILITY_UNKNOWN = 0;
    public static final byte VISIBILITY_SHOWN = 1;
    public static final byte VISIBILITY_HIDDEN = 2;

    // Overlay — a vertex holds its position, the other end of its line, its offset in pixels and its colour
    public static final String MESH_OVERLAY_VAO = "editor/CharacterMeshOverlayVAO";
    public static final String MATERIAL_OVERLAY = "editor/CharacterMeshOverlayMaterial";
    public static final String UNIFORM_DEPTH_BIAS = "u_depthBias";
    public static final int OVERLAY_VERTEX_FLOATS = 12;
    public static final int OVERLAY_COLOR_FLOATS = 4;
    public static final float OVERLAY_DEPTH_BIAS = 0.0004f;
    public static final int DEPTH_OVERLAY = 2;
    public static final int DEPTH_GUIDES = 3;

    // Overlay — half sizes in pixels
    public static final float VERTEX_HALF_PIXELS = 2.5f;
    public static final float VERTEX_SELECTED_HALF_PIXELS = 3.5f;
    public static final float WEIGHT_HALF_PIXELS = 3f;
    public static final float WEIGHT_SELECTED_HALF_PIXELS = 4.5f;
    public static final float EDGE_HALF_PIXELS = 0.75f;
    public static final float GUIDE_HALF_PIXELS = 1f;

    // Overlay — guides; screen guides sit this many near planes from the camera, an axis runs this far each way
    public static final float GUIDE_NEAR_SCALE = 2f;
    public static final float AXIS_GUIDE_LENGTH = 4f;
    public static final int BRUSH_GUIDE_SEGMENTS = 32;

    // Overlay — colours
    public static final float[] COLOR_VERTEX = { 0.05f, 0.05f, 0.08f, 1f };
    public static final float[] COLOR_SELECTED = { 1f, 0.55f, 0.1f, 1f };
    public static final float[] COLOR_ACTIVE = { 1f, 1f, 1f, 1f };
    public static final float[] COLOR_EDGE = { 0.08f, 0.08f, 0.1f, 0.75f };
    public static final float[] COLOR_EDGE_SELECTED = { 1f, 0.6f, 0.15f, 1f };
    public static final float[] COLOR_HEAT_NONE = { 0.1f, 0.2f, 0.85f, 1f };
    public static final float[] COLOR_HEAT_HALF = { 0.15f, 0.85f, 0.2f, 1f };
    public static final float[] COLOR_HEAT_FULL = { 0.95f, 0.15f, 0.1f, 1f };
    public static final float[] COLOR_GUIDE = { 1f, 1f, 1f, 0.85f };
    public static final float[] COLOR_AXIS_NORMAL = { 1f, 0.85f, 0.2f, 1f };
    public static final float[][] COLOR_AXES = {
            { 0.95f, 0.25f, 0.25f, 1f },
            { 0.3f, 0.9f, 0.3f, 1f },
            { 0.3f, 0.5f, 1f, 1f } };
}
