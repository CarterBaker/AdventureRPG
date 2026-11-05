package editor.runtime.shipviewer;

import engine.input.Buttons;

public class ShipViewerSetting {

    /*
     * Constants used only by ShipViewerContext — its material and render
     * order, its cutaway, its orbit and pan camera and the views it snaps to,
     * its mouse bindings, its menu and axis gizmo, and the text of its status
     * line and deck label.
     */

    // Materials
    public static final String MATERIAL_SHIP = "editor/ShipViewerMaterial";
    public static final String UNIFORM_CUTAWAY_HEIGHT = "u_cutawayHeight";

    // Render Order
    public static final int DEPTH_SHIP = 0;

    // Cutaway — model blocks; every deck shown clips at no height, a deck shown clips just under the floor above it
    public static final float CUTAWAY_NONE = 100000f;
    public static final float CUTAWAY_EPSILON = 0.01f;
    public static final int DECK_ALL = -1;

    // Camera — the frame's centre the middle of the ship's bounds moved by the pan, the distances in its bounding
    // radius; a pan moves the ship with the pointer
    public static final float ORBIT_DISTANCE_DEFAULT = 2.2f;
    public static final float ORBIT_DISTANCE_MIN = 0.3f;
    public static final float ORBIT_DISTANCE_MAX = 8f;
    public static final float ORBIT_ZOOM_STEP = 0.1f;
    public static final float ORBIT_INITIAL_YAW_DEGREES = 35f;
    public static final float ORBIT_INITIAL_PITCH_DEGREES = 18f;

    // Views — each axis a view is snapped along, looking back at the ship from it: forward (bow), aft, up, down,
    // starboard and port; a view straight up or down leans this far toward the bow so the camera keeps its heading
    public static final String[] VIEW_AXES = { "x", "-x", "y", "-y", "z", "-z" };
    public static final float[][] VIEW_AXIS_VECTORS = {
            { 1f, 0f, 0f }, { -1f, 0f, 0f }, { 0f, 1f, 0f }, { 0f, -1f, 0f }, { 0f, 0f, 1f }, { 0f, 0f, -1f } };
    public static final float VIEW_VERTICAL_LEAN = 0.02f;

    // Mouse
    public static final int BUTTON_ORBIT = Buttons.LEFT;
    public static final int BUTTON_PAN = Buttons.RIGHT;

    // Gizmo — every axis's arm drawn from the middle to where the axis points on screen, out to a radius in pixels,
    // as dots up to the cone at its tip, placed in steps across the gizmo
    public static final float GIZMO_RADIUS_PIXELS = 30f;
    public static final int GIZMO_STEPS = 31;
    public static final int GIZMO_ARM_DOTS = 3;

    // Menus — entry points in the order the menu lists them: the gizmo's cones in VIEW_AXES order, then its arms'
    // dots axis after axis, from the middle out
    public static final String MENU_VIEWER = "editor/ShipViewer/Viewer";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_DECK = 1;
    public static final int ENTRY_GIZMO_CONES = 2;
    public static final int ENTRY_GIZMO_ARMS = ENTRY_GIZMO_CONES + VIEW_AXES.length;

    // Text
    public static final String STATUS_PREFIX = "Ship Viewer  |  ";
    public static final String STATUS_NONE =
            "No ship chosen — select one in the hierarchy or open the Shipwright";
    public static final String STATUS_UNREGISTERED = " is not built — save it at the Shipwright";
    public static final String STATUS_SEPARATOR = "  ·  ";
    public static final String STATUS_PARTS = " parts";
    public static final String STATUS_DRAFT = "on the table";
    public static final String STATUS_UNNAMED = "Unnamed ship";
    public static final String DECK_TEXT_ALL = "All decks";
    public static final String DECK_TEXT_FORMAT = "Deck %d of %d";
}
