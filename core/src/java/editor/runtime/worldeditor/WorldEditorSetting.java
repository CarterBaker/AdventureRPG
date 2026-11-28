package editor.runtime.worldeditor;

import engine.input.Buttons;

public class WorldEditorSetting {

    /*
     * Constants used only by WorldEditorContext and the layer, view, tool
     * and render systems it owns — their viewport mesh and materials, the
     * height layer's opacity, zoom limits, mouse bindings and selection
     * reach, the held shortcuts the selection reads, and the World Editor's
     * toolbar menu. The images, their brush and the biome palette are the
     * editor's.
     */

    // Meshes
    public static final String MESH_QUAD = "editor/EditorViewportQuad";

    // Materials
    public static final String MATERIAL_IMAGE = "editor/WorldEditorImageMaterial";
    public static final String MATERIAL_HEIGHTS = "editor/WorldEditorHeightMaterial";
    public static final String MATERIAL_BRUSH = "editor/WorldEditorBrushMaterial";
    public static final String MATERIAL_MARKER = "editor/WorldMapMarkerMaterial";
    public static final String MATERIAL_GLOBE = "editor/WorldEditorGlobeMaterial";

    // Uniforms
    public static final String UNIFORM_VIEW_RECT = "u_viewRect";
    public static final String UNIFORM_UV_RECT = "u_uvRect";
    public static final String UNIFORM_TILE_TEXTURE = "u_tileTexture";
    public static final String UNIFORM_OVERLAY_ALPHA = "u_overlayAlpha";
    public static final String UNIFORM_MARKER_CENTER = "u_markerCenter";
    public static final String UNIFORM_MARKER_DIRECTION = "u_markerDirection";
    public static final String UNIFORM_MARKER_SCALE = "u_markerScale";
    public static final String UNIFORM_BRUSH_SHAPE = "u_shape";
    public static final String UNIFORM_BRUSH_RATIO = "u_ratio";
    public static final String UNIFORM_BRUSH_ANGLE = "u_angle";
    public static final String UNIFORM_HEIGHT_TEXTURE = "u_heightTexture";
    public static final String UNIFORM_SHOW_BIOMES = "u_showBiomes";
    public static final String UNIFORM_SHOW_HEIGHTS = "u_showHeights";
    public static final String UNIFORM_FACE_TEXELS = "u_faceTexels";
    public static final String UNIFORM_GLOBE_CAMERA = "u_globeCamera";
    public static final String UNIFORM_GLOBE_FORWARD = "u_globeForward";
    public static final String UNIFORM_GLOBE_RIGHT = "u_globeRight";
    public static final String UNIFORM_GLOBE_UP = "u_globeUp";
    public static final String UNIFORM_BRUSH_FACE = "u_brushFace";
    public static final String UNIFORM_BRUSH_CENTER = "u_brushCenter";
    public static final String UNIFORM_BRUSH_RADIUS = "u_brushRadius";
    public static final String UNIFORM_FOOTPRINT_FACE = "u_footprintFace";
    public static final String UNIFORM_FOOTPRINT_CENTER = "u_footprintCenter";
    public static final String UNIFORM_FOOTPRINT_RADIUS = "u_footprintRadius";
    public static final String UNIFORM_SHOW_BOX = "u_showBox";
    public static final String UNIFORM_BOX_RECT = "u_boxRect";
    public static final String UNIFORM_SHOW_CUBE = "u_showCube";
    public static final String UNIFORM_CUBE_AXIS_X = "u_cubeAxisX";
    public static final String UNIFORM_CUBE_AXIS_Y = "u_cubeAxisY";
    public static final String UNIFORM_CUBE_AXIS_Z = "u_cubeAxisZ";

    // Render Order
    public static final int DEPTH_IMAGE = 0;
    public static final int DEPTH_HEIGHTS = 1;
    public static final int DEPTH_BRUSH = 2;
    public static final int DEPTH_MARKERS = 3;

    // Heights — the height layer is laid over the biomes as the black and white heightmap it is, this opaque
    public static final float HEIGHT_OVERLAY_ALPHA = 0.7f;

    // Markers
    public static final float MARKER_SIZE_PIXELS = 14f;

    // View — zoom is image pixels per window pixel, out to the whole image shown this many times over
    public static final double MIN_IMAGE_PIXELS_PER_SCREEN_PIXEL = 0.03125;
    public static final double MAX_FIT_MULTIPLE = 2.0;
    public static final double ZOOM_STEP = 1.25;

    // Globe — a globe world's image is shown on a ball of this radius the view camera orbits
    public static final float GLOBE_RADIUS = 1f;
    public static final float GLOBE_NO_FACE = -1f;

    // Mouse
    public static final int BUTTON_APPLY = Buttons.LEFT;
    public static final int BUTTON_PAN = Buttons.RIGHT;

    // Selection — window pixels a press travels before it drags, and how near a click picks a placement
    public static final float DRAG_PIXELS = 4f;
    public static final float SELECT_REACH_PIXELS = 12f;

    // Shortcuts — held modifiers the selection reads
    public static final String SHORTCUT_ADD_SELECTION = "WorldEditor/AddSelection";
    public static final String SHORTCUT_SUBTRACT_SELECTION = "WorldEditor/SubtractSelection";
    public static final String SHORTCUT_INVERT_STROKE = "WorldEditor/InvertStroke";

    // Menus
    public static final String MENU_TOOLBAR = "editor/WorldEditor/Toolbar";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_PLAYERS_LABEL = 1;
    public static final int ENTRY_MODE_LABEL = 2;
}
