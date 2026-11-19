package editor.runtime.itemeditor;

import engine.input.Buttons;

public class ItemEditorSetting {

    /*
     * Constants used only by ItemEditorContext — its viewport meshes and
     * materials, the stamp preview, mouse binding, and status bar menu.
     */

    // Meshes
    public static final String MESH_GRID = "editor/ItemEditorGrid";
    public static final String MESH_CURSOR = "editor/ItemEditorCursor";

    // Materials
    public static final String MATERIAL_MODEL = "editor/ItemEditorModelMaterial";
    public static final String MATERIAL_GRID = "editor/ItemEditorGridMaterial";
    public static final String MATERIAL_CURSOR_PLACE = "editor/ItemEditorCursorPlaceMaterial";
    public static final String MATERIAL_CURSOR_ERASE = "editor/ItemEditorCursorEraseMaterial";
    public static final String MATERIAL_CURSOR_PAINT = "editor/ItemEditorCursorPaintMaterial";
    public static final String MATERIAL_STAMP_BUILD = "editor/ItemEditorStampBuildMaterial";
    public static final String MATERIAL_STAMP_DIG = "editor/ItemEditorStampDigMaterial";

    // Uniforms
    public static final String UNIFORM_RESOLUTION = "u_resolution";
    public static final String UNIFORM_EXTENT = "u_extent";
    public static final String UNIFORM_CURSOR_CELL = "u_cursorCell";
    public static final String UNIFORM_CURSOR_SIZE = "u_cursorSize";

    // Render Order
    public static final int DEPTH_MODEL = 0;
    public static final int DEPTH_GRID = 1;
    public static final int DEPTH_CURSOR = 2;

    // Stamp — the one part its cubes are laid in, and how many values say what it shows
    public static final String STAMP_PART_NAME = "Stamp";
    public static final int STAMP_KEY_LENGTH = 12;

    // Mouse
    public static final int BUTTON_APPLY = Buttons.LEFT;

    // Menus
    public static final String MENU_TOOLBAR = "editor/ItemEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;
}
