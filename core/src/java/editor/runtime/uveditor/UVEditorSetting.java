package editor.runtime.uveditor;

import engine.input.Buttons;

public class UVEditorSetting {

    /*
     * Constants used only by UVEditorContext and the systems it owns — its
     * materials and their uniforms, the view's fit, zoom and pan, the
     * pointer's reach and buttons, the held shortcuts its selection reads,
     * and the colours and sizes its UVs are drawn with.
     */

    // Materials
    public static final String MATERIAL_BACKGROUND = "editor/UVEditorBackgroundMaterial";
    public static final String MATERIAL_OVERLAY = "editor/UVEditorOverlayMaterial";

    // Uniforms
    public static final String UNIFORM_UV_TO_CLIP = "u_uvToClip";
    public static final String UNIFORM_VIEW_SIZE = "u_viewSize";
    public static final String UNIFORM_TILE_ID = "u_tileID";

    // Render Order
    public static final int DEPTH_BACKGROUND = 0;
    public static final int DEPTH_OVERLAY = 1;

    // View — the share of the window the unit square fills when fitted, the zoom step per wheel notch, and how far
    // in and out the view may zoom, in window pixels per unit of UV
    public static final float FIT_SHARE = 0.85f;
    public static final float ZOOM_STEP = 1.15f;
    public static final float MIN_PIXELS_PER_UNIT = 32f;
    public static final float MAX_PIXELS_PER_UNIT = 65536f;

    // Pointer — how far from a UV a click still picks it, in window pixels, and the drag that starts a box
    public static final float PICK_RADIUS_PIXELS = 8f;
    public static final float BOX_THRESHOLD_PIXELS = 4f;
    public static final int BUTTON_SELECT = Buttons.LEFT;
    public static final int BUTTON_CANCEL = Buttons.RIGHT;
    public static final int BUTTON_PAN = Buttons.RIGHT;

    // Shortcuts — held
    public static final String SHORTCUT_ADD_SELECTION = "UVEditor/AddSelection";
    public static final String SHORTCUT_SUBTRACT_SELECTION = "UVEditor/SubtractSelection";

    // Overlay — floats per vertex and per colour, sizes in window pixels
    public static final int OVERLAY_VERTEX_FLOATS = 12;
    public static final int OVERLAY_COLOR_FLOATS = 4;
    public static final float CORNER_HALF_PIXELS = 2.5f;
    public static final float CORNER_SELECTED_HALF_PIXELS = 3.5f;
    public static final float EDGE_HALF_PIXELS = 0.75f;
    public static final float SEAM_HALF_PIXELS = 1.5f;
    public static final float BOX_HALF_PIXELS = 1f;

    // Overlay — colours
    public static final float[] COLOR_EDGE = { 0.85f, 0.85f, 0.9f, 0.9f };
    public static final float[] COLOR_SEAM = { 0.95f, 0.15f, 0.15f, 1f };
    public static final float[] COLOR_CORNER = { 0.05f, 0.05f, 0.08f, 1f };
    public static final float[] COLOR_SELECTED = { 1f, 0.55f, 0.1f, 1f };
    public static final float[] COLOR_PINNED = { 0.95f, 0.2f, 0.2f, 1f };
    public static final float[] COLOR_BOX = { 1f, 1f, 1f, 0.85f };
}
