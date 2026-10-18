package editor.charactereditor;

import engine.input.Buttons;

public class CharacterEditorSetting {

    /*
     * Constants used only by CharacterEditorContext — its toolbar menu, the
     * row templates its two panels are filled from, the size of the box
     * shown for a held anchor, and the mesh modes' pointer, keys, picking and
     * the overlay their vertices and edges are drawn with.
     */

    // Menus
    public static final String MENU_TOOLBAR = "editor/CharacterEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_LIST = 1;
    public static final int ENTRY_PROPERTIES = 2;

    // Templates
    public static final String TEMPLATE_HEADER_ROW = "editor/CharacterEditor/character_editor_header_row";
    public static final String TEMPLATE_ITEM_ROW = "editor/CharacterEditor/character_editor_item_row";
    public static final String TEMPLATE_ITEM_ROW_SELECTED = "editor/CharacterEditor/character_editor_item_row_selected";
    public static final String TEMPLATE_NUMBER_ROW = "editor/CharacterEditor/character_editor_number_row";
    public static final String TEMPLATE_CHOICE_ROW = "editor/CharacterEditor/character_editor_choice_row";
    public static final String TEMPLATE_ACTION_ROW = "editor/CharacterEditor/character_editor_action_row";

    // Elements
    public static final String ELEMENT_ROW_LABEL = "character_editor_row_label";
    public static final String ELEMENT_ROW_VALUE = "character_editor_row_value";
    public static final String ELEMENT_ROW_VALUE_BUTTON = "character_editor_row_value_button";
    public static final String ELEMENT_ROW_DECREASE = "character_editor_row_decrease";
    public static final String ELEMENT_ROW_INCREASE = "character_editor_row_increase";

    // Markers — the box shown for a held anchor, in blocks
    public static final float HELD_ANCHOR_MARKER_SIZE = 0.06f;

    // Mesh Input — buttons and the GLFW keys of the mesh modes
    public static final int BUTTON_SELECT = Buttons.LEFT;
    public static final int BUTTON_CANCEL = Buttons.RIGHT;
    public static final int KEY_LEFT_CONTROL = 341;
    public static final int KEY_RIGHT_CONTROL = 345;
    public static final int KEY_DELETE = 261;
    public static final int KEY_SELECT_ALL = 65;
    public static final int KEY_DUPLICATE = 68;
    public static final int KEY_EXTRUDE = 69;
    public static final int KEY_MAKE_FACE = 70;
    public static final int KEY_GRAB = 71;
    public static final int KEY_SELECT_LINKED = 76;
    public static final int KEY_MERGE = 77;
    public static final int KEY_ROTATE = 82;
    public static final int KEY_SCALE = 83;
    public static final int KEY_DELETE_ALTERNATE = 88;
    public static final int KEY_REDO = 89;
    public static final int KEY_UNDO = 90;
    public static final int[] KEY_AXES = { 88, 89, 90 };

    // Mesh Input — pointer distances in pixels, the brush rate per second and transform snapping
    public static final float PICK_RADIUS_PIXELS = 10f;
    public static final float DRAG_PIXELS = 4f;
    public static final float SCALE_MIN_PIXELS = 4f;
    public static final float BRUSH_RATE = 8f;
    public static final float SNAP_DEGREES = 15f;
    public static final float SNAP_SCALE = 0.1f;
    public static final float RAY_EPSILON = 0.00001f;

    // Mesh View — a point is hidden by a face nearer the camera than it by more than the bias, in blocks
    public static final float OCCLUSION_BIAS = 0.003f;
    public static final float OCCLUSION_EPSILON = 0.000001f;

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
