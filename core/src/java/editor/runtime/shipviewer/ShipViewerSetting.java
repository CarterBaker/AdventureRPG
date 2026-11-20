package editor.runtime.shipviewer;

public class ShipViewerSetting {

    /*
     * Constants used only by ShipViewerContext — its material and render
     * order, its cutaway, its menu, and the text of its status line and deck
     * label.
     */

    // Materials
    public static final String MATERIAL_SHIP = "editor/ShipViewerMaterial";
    public static final String MATERIAL_SHIP_PAINTED = "editor/ShipViewerPaintedMaterial";
    public static final String UNIFORM_CUTAWAY_HEIGHT = "u_cutawayHeight";

    // Render Order
    public static final int DEPTH_SHIP = 0;

    // Cutaway — model blocks; every deck shown clips at no height, a deck shown clips just under the floor above it
    public static final float CUTAWAY_NONE = 100000f;
    public static final float CUTAWAY_EPSILON = 0.01f;
    public static final int DECK_ALL = -1;

    // Menus — entry points in the order the menu lists them
    public static final String MENU_VIEWER = "editor/ShipViewer/Viewer";
    public static final int ENTRY_STATUS = 0;
    public static final int ENTRY_DECK = 1;

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
