package editor.itemeditor;

import engine.input.Buttons;

public class ItemEditorSetting {

    /*
     * Constants used only by ItemEditorContext — its render target, viewport
     * meshes and materials, orbit camera, mouse bindings, and toolbar menu.
     */

    // Render Target
    public static final String FBO_ITEM_EDITOR = "ItemEditorScene";

    // Meshes
    public static final String MESH_GRID = "editor/ItemEditorGrid";
    public static final String MESH_CURSOR = "editor/ItemEditorCursor";

    // Materials
    public static final String MATERIAL_MODEL = "editor/ItemEditorModelMaterial";
    public static final String MATERIAL_GRID = "editor/ItemEditorGridMaterial";
    public static final String MATERIAL_CURSOR_PLACE = "editor/ItemEditorCursorPlaceMaterial";
    public static final String MATERIAL_CURSOR_ERASE = "editor/ItemEditorCursorEraseMaterial";
    public static final String MATERIAL_CURSOR_PAINT = "editor/ItemEditorCursorPaintMaterial";

    // Uniforms
    public static final String UNIFORM_RESOLUTION = "u_resolution";
    public static final String UNIFORM_CURSOR_CELL = "u_cursorCell";

    // Render Order
    public static final int DEPTH_MODEL = 0;
    public static final int DEPTH_GRID = 1;
    public static final int DEPTH_CURSOR = 2;

    // Camera
    public static final float ORBIT_TARGET_X = 0.5f;
    public static final float ORBIT_TARGET_Y = 0.5f;
    public static final float ORBIT_TARGET_Z = 0.5f;
    public static final float ORBIT_DISTANCE_DEFAULT = 2f;
    public static final float ORBIT_DISTANCE_MIN = 0.6f;
    public static final float ORBIT_DISTANCE_MAX = 6f;
    public static final float ORBIT_ZOOM_STEP = 0.2f;
    public static final float ORBIT_INITIAL_YAW_DEGREES = 35f;
    public static final float ORBIT_INITIAL_PITCH_DEGREES = 25f;

    // Mouse
    public static final int BUTTON_APPLY = Buttons.LEFT;
    public static final int BUTTON_ORBIT = Buttons.RIGHT;

    // Menus
    public static final String MENU_TOOLBAR = "editor/ItemEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;
}
