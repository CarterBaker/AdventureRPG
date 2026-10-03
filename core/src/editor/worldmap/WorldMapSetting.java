package editor.worldmap;

import engine.input.Buttons;

public class WorldMapSetting {

    /*
     * Constants used only by WorldMapContext — its viewport mesh and
     * materials, zoom limits, the character marker, mouse binding and status
     * menu. The map itself, its tiles and palette, is the engine's.
     */

    // Meshes
    public static final String MESH_QUAD = "editor/EditorViewportQuad";

    // Materials
    public static final String MATERIAL_TILE = "editor/WorldMapTileMaterial";
    public static final String MATERIAL_MARKER = "editor/WorldMapMarkerMaterial";

    // Uniforms
    public static final String UNIFORM_VIEW_RECT = "u_viewRect";
    public static final String UNIFORM_UV_RECT = "u_uvRect";
    public static final String UNIFORM_TILE_TEXTURE = "u_tileTexture";
    public static final String UNIFORM_MARKER_CENTER = "u_markerCenter";
    public static final String UNIFORM_MARKER_DIRECTION = "u_markerDirection";
    public static final String UNIFORM_MARKER_SCALE = "u_markerScale";

    // Render Order
    public static final int DEPTH_TILES = 0;
    public static final int DEPTH_MARKER = 1;

    // View
    public static final double DEFAULT_BLOCKS_PER_PIXEL = 4.0;
    public static final double MIN_BLOCKS_PER_PIXEL = 0.125;
    public static final double ZOOM_STEP = 1.25;

    // Marker
    public static final float MARKER_SIZE_PIXELS = 14f;

    // Mouse
    public static final int BUTTON_PAN = Buttons.LEFT;

    // Menus
    public static final String MENU_STATUS = "editor/WorldMap/Status";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_FOLLOW_LABEL = 1;
}
