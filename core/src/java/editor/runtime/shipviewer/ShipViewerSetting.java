package editor.runtime.shipviewer;

public class ShipViewerSetting {

    /*
     * Constants used only by ShipViewerContext — its material and render
     * order, its cutaway, the views its camera snaps to, its menu and axis
     * gizmo, and the text of its status line and deck label.
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

    // Views — each axis a view is snapped along, looking back at the ship from it: forward (bow), aft, up, down,
    // starboard and port
    public static final String[] VIEW_AXES = { "x", "-x", "y", "-y", "z", "-z" };
    public static final float[][] VIEW_AXIS_VECTORS = {
            { 1f, 0f, 0f }, { -1f, 0f, 0f }, { 0f, 1f, 0f }, { 0f, -1f, 0f }, { 0f, 0f, 1f }, { 0f, 0f, -1f } };

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
