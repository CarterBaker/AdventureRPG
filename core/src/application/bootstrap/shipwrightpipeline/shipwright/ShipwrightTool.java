package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;

public enum ShipwrightTool {

    /*
     * What the drafting table's cursor does, always on the plan of the deck
     * picked. A deck's outline is drawn mirrored either side of the centre
     * line; walls, full or half height, are painted where they stand; a door
     * or a window is cut through the wall or side it faces; a stair is set at
     * its foot facing the way it climbs; a mast is stepped on the centre line;
     * furniture, the helm and the capstan are set down. A painted tool works
     * every octant the cursor crosses while held, any other the octant it
     * was pressed on once.
     */

    DECK(EngineSetting.SHIPWRIGHT_TOOL_DECK, true, true),
    WALLS(EngineSetting.SHIPWRIGHT_TOOL_WALLS, true, false),
    OPENINGS(EngineSetting.SHIPWRIGHT_TOOL_OPENINGS, false, false),
    STAIRS(EngineSetting.SHIPWRIGHT_TOOL_STAIRS, false, false),
    MASTS(EngineSetting.SHIPWRIGHT_TOOL_MASTS, false, false),
    FURNITURE(EngineSetting.SHIPWRIGHT_TOOL_FURNITURE, false, false);

    // Values
    public static final ShipwrightTool[] VALUES = values();

    // Identity
    private final String title;
    private final boolean painted;
    private final boolean mirrored;

    // Constructor \\

    ShipwrightTool(String title, boolean painted, boolean mirrored) {

        // Identity
        this.title = title;
        this.painted = painted;
        this.mirrored = mirrored;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    // True for a tool dragged across the sheet, false for one that works a single octant per press
    public boolean isPainted() {
        return painted;
    }

    // True for a tool whose every stroke is drawn on both sides of the centre line at once
    public boolean isMirrored() {
        return mirrored;
    }

    // The tool after this one, round to the first
    public ShipwrightTool next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }
}
