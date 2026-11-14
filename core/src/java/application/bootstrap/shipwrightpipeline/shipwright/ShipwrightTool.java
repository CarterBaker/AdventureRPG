package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;

public enum ShipwrightTool {

    /*
     * What the drafting table's cursor does, always on the plan of the deck
     * picked. A deck's outline is drawn mirrored either side of the centre
     * line or, as a porch, out to one side alone, and the waist is cut
     * through a deck's floor and a roof laid over its open floor the same two
     * ways; walls, full or half height, are painted where they stand; a door
     * or a window is set on the wall or side it is cut through, and a hatch
     * in a floor; a staircase is set at its foot facing the way it climbs, a
     * ladder at its foot and a boarding ladder over the side of an open deck;
     * a mast is stepped on the centre line; furniture, cannons, the helm and
     * the capstan are set down. A painted tool works every octant the cursor
     * crosses while held; any other works the octant it was pressed on once,
     * setting the thing in hand down there or picking up or taking away
     * whatever already stands there. Tools are taken up through the options
     * the catalogue's tabs list, ShipwrightTab.
     */

    DECK(EngineSetting.SHIPWRIGHT_TOOL_DECK, true, EngineSetting.SHIPWRIGHT_TEXT_PORCH),
    WAIST(EngineSetting.SHIPWRIGHT_TOOL_WAIST, true, EngineSetting.SHIPWRIGHT_TEXT_SIDE_WAIST),
    ROOF(EngineSetting.SHIPWRIGHT_TOOL_ROOF, true, EngineSetting.SHIPWRIGHT_TEXT_SIDE_ROOF),
    WALLS(EngineSetting.SHIPWRIGHT_TOOL_WALLS, true, null),
    OPENINGS(EngineSetting.SHIPWRIGHT_TOOL_OPENINGS, false, null),
    STAIRS(EngineSetting.SHIPWRIGHT_TOOL_STAIRS, false, null),
    MASTS(EngineSetting.SHIPWRIGHT_TOOL_MASTS, false, null),
    FURNITURE(EngineSetting.SHIPWRIGHT_TOOL_FURNITURE, false, null);

    // Values
    public static final ShipwrightTool[] VALUES = values();

    // Identity
    private final String title;
    private final boolean painted;

    // Sides — the title of the option that works one side alone, null for a tool with no such option
    private final String sideTitle;

    // Constructor \\

    ShipwrightTool(String title, boolean painted, String sideTitle) {

        // Identity
        this.title = title;
        this.painted = painted;

        // Sides
        this.sideTitle = sideTitle;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    // True for a tool dragged across the sheet, false for one that works a single octant per press
    public boolean isPainted() {
        return painted;
    }

    // True for a tool whose strokes are drawn on both sides of the centre line at once or on one alone, as its
    // options SHIPWRIGHT_OPTION_MIRRORED and SHIPWRIGHT_OPTION_ONE_SIDE pick
    public boolean isSided() {
        return sideTitle != null;
    }

    // The title of one of a sided tool's options: mirrored, the tool's own, or one side alone
    public String getTitle(int option) {
        return option == EngineSetting.SHIPWRIGHT_OPTION_ONE_SIDE && sideTitle != null ? sideTitle : title;
    }
}
