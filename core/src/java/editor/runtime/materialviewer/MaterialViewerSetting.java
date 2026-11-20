package editor.runtime.materialviewer;

import editor.runtime.EditorSetting;

public class MaterialViewerSetting {

    /*
     * Constants used only by MaterialViewerContext — its material and its
     * uniforms, render order, the shapes it shows a material on and how they
     * are laid out and unwrapped, the frame its camera holds, the light the
     * Light tool turns, its menu, and the words its status line gives.
     */

    // Materials
    public static final String MATERIAL_VIEWER = "editor/MaterialViewerMaterial";

    // Uniforms — the maps the viewer reads, by their place in CHARACTER_PAINT_MAP_LABELS, each with its sampler
    public static final int[] SHOWN_MAPS = {
            EditorSetting.MATERIALS_MAP_ALBEDO, EditorSetting.MATERIALS_MAP_NORMAL,
            EditorSetting.MATERIALS_MAP_AMBIENT_OCCLUSION, EditorSetting.MATERIALS_MAP_METALLIC,
            EditorSetting.MATERIALS_MAP_ROUGHNESS };
    public static final String[] UNIFORM_MAPS = {
            "u_albedoMap", "u_normalMap", "u_occlusionMap", "u_metallicMap", "u_roughnessMap" };
    public static final String UNIFORM_BASE_COLOR = "u_baseColor";
    public static final String UNIFORM_SURFACE = "u_surface";
    public static final String UNIFORM_LIGHT_DIRECTION = "u_lightDirection";

    // Render Order
    public static final int DEPTH_SHAPE = 0;

    // Shapes — in TOOL_SHAPE_NAMES order; each fits a sphere of the shape radius, in metres
    public static final int SHAPE_SPHERE = 0;
    public static final int SHAPE_CUBE = 1;
    public static final int SHAPE_CYLINDER = 2;
    public static final int SHAPE_PLANE = 3;
    public static final float SHAPE_RADIUS = 0.5f;
    public static final int SPHERE_SEGMENTS = 48;
    public static final int SPHERE_RINGS = 24;
    public static final int CYLINDER_SEGMENTS = 48;
    public static final float CUBE_HALF_EXTENT = 0.35f;
    public static final float CYLINDER_RADIUS = 0.35f;
    public static final float CYLINDER_HALF_HEIGHT = 0.35f;
    public static final float PLANE_HALF_EXTENT = 0.5f;

    // Unwrap — how many times a material repeats round a sphere and a cylinder's side, so its texels stay square
    public static final float SPHERE_U_REPEAT = 2f;
    public static final float CYLINDER_U_REPEAT = 3f;
    public static final float CAP_UV_MIDDLE = 0.5f;

    // Cube — each face's normal, the way its u runs and the way its v runs, u across v being the normal
    public static final float[][] CUBE_FACES = {
            { 1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f },
            { -1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f },
            { 0f, 0f, 1f, 1f, 0f, 0f, 0f, 1f, 0f },
            { 0f, 0f, -1f, -1f, 0f, 0f, 0f, 1f, 0f },
            { 0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, -1f },
            { 0f, -1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f } };
    public static final float[][] QUAD_CORNERS = { { -1f, -1f }, { 1f, -1f }, { 1f, 1f }, { -1f, 1f } };

    // Frame — the camera holds a sphere this share wider than the shapes
    public static final float FRAME_RADIUS_SHARE = 1.25f;

    // Light — where it starts, and how far it turns per window pixel dragged, in radians
    public static final float[] LIGHT_DIRECTION_DEFAULT = { -0.4f, -1f, -0.6f };
    public static final float LIGHT_TURN_PER_PIXEL = 0.01f;

    // Menus
    public static final String MENU_VIEWER = "editor/MaterialViewer/Viewer";
    public static final int ENTRY_STATUS = 0;

    // Text
    public static final String STATUS_PREFIX = "Material Viewer  |  ";
    public static final String STATUS_SEPARATOR = "  ·  ";
}
