package editor.textureviewer;

import engine.input.Buttons;

public class TextureViewerSetting {

    /*
     * Constants used only by TextureViewerContext — its viewport mesh and
     * material, atlas layout, mouse binding, and status menu.
     */

    // Meshes
    public static final String MESH_QUAD = "editor/EditorViewportQuad";

    // Materials
    public static final String MATERIAL_ATLAS = "editor/TextureViewerAtlasMaterial";

    // Uniforms
    public static final String UNIFORM_VIEW_RECT = "u_viewRect";
    public static final String UNIFORM_TILE_RECTS = "u_tileRects";
    public static final String UNIFORM_TILE_COUNT = "u_tileCount";
    public static final String UNIFORM_HOVERED_TILE = "u_hoveredTile";
    public static final String UNIFORM_SELECTED_TILE = "u_selectedTile";

    // Tiles — matches the tile array size declared by the atlas shader
    public static final int MAX_TILES = 64;

    // Layout
    public static final float VIEW_MARGIN_PIXELS = 32f;

    // Render Order
    public static final int DEPTH_ATLAS = 0;

    // Mouse
    public static final int BUTTON_SELECT = Buttons.LEFT;

    // Menus
    public static final String MENU_STATUS = "editor/TextureViewer/Status";
    public static final int ENTRY_STATUS = 0;
}
