package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;

public enum ShipwrightTab {

    /*
     * The four tabs of a drafting table's catalogue, each the options of one
     * part of the work: the structure — the deck's outline, the waist, the
     * floors laid in it and the gun platforms cannons stand on; the style —
     * walls, railings and lips, doors and windows, the helm and the capstan,
     * stairs and ladders; the masts; and every piece of furniture. A tab
     * lists its fixed options first, each the tool that works it, the option
     * of that tool and the section it is listed under, then the options of
     * one tool whose count follows the ship or the game: every mast stepped,
     * or every furnishing past the helm and the capstan.
     */

    STRUCTURE(EngineSetting.SHIPWRIGHT_TAB_STRUCTURE,
            new ShipwrightTool[] {
                    ShipwrightTool.DECK, ShipwrightTool.WAIST,
                    ShipwrightTool.OPENINGS, ShipwrightTool.OPENINGS, ShipwrightTool.OPENINGS,
                    ShipwrightTool.OPENINGS },
            new int[] {
                    0, 0,
                    ShipOpening.HATCH.ordinal(), ShipOpening.GRATE.ordinal(), ShipOpening.GLASS.ordinal(),
                    ShipOpening.PLATFORM.ordinal() },
            new String[] {
                    EngineSetting.SHIPWRIGHT_SECTION_HULL, EngineSetting.SHIPWRIGHT_SECTION_HULL,
                    EngineSetting.SHIPWRIGHT_SECTION_FLOORS, EngineSetting.SHIPWRIGHT_SECTION_FLOORS,
                    EngineSetting.SHIPWRIGHT_SECTION_FLOORS, EngineSetting.SHIPWRIGHT_SECTION_GUNS },
            null,
            0),
    STYLE(EngineSetting.SHIPWRIGHT_TAB_STYLE,
            new ShipwrightTool[] {
                    ShipwrightTool.WALLS, ShipwrightTool.WALLS, ShipwrightTool.WALLS, ShipwrightTool.WALLS,
                    ShipwrightTool.OPENINGS, ShipwrightTool.OPENINGS,
                    ShipwrightTool.FURNITURE, ShipwrightTool.FURNITURE,
                    ShipwrightTool.STAIRS, ShipwrightTool.STAIRS, ShipwrightTool.STAIRS },
            new int[] {
                    ShipWall.FULL.ordinal(), ShipWall.HALF.ordinal(), ShipWall.RAILING.ordinal(),
                    ShipWall.LIP.ordinal(),
                    ShipOpening.DOOR.ordinal(), ShipOpening.WINDOW.ordinal(),
                    EngineSetting.SHIPWRIGHT_ITEM_HELM, EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN,
                    ShipClimb.STAIR.ordinal(), ShipClimb.LADDER.ordinal(), ShipClimb.BOARDING.ordinal() },
            new String[] {
                    EngineSetting.SHIPWRIGHT_SECTION_WALLS, EngineSetting.SHIPWRIGHT_SECTION_WALLS,
                    EngineSetting.SHIPWRIGHT_SECTION_WALLS, EngineSetting.SHIPWRIGHT_SECTION_WALLS,
                    EngineSetting.SHIPWRIGHT_SECTION_OPENINGS, EngineSetting.SHIPWRIGHT_SECTION_OPENINGS,
                    EngineSetting.SHIPWRIGHT_TEXT_FITTINGS, EngineSetting.SHIPWRIGHT_TEXT_FITTINGS,
                    EngineSetting.SHIPWRIGHT_SECTION_CLIMBS, EngineSetting.SHIPWRIGHT_SECTION_CLIMBS,
                    EngineSetting.SHIPWRIGHT_SECTION_CLIMBS },
            null,
            0),
    MASTS(EngineSetting.SHIPWRIGHT_TAB_MASTS,
            new ShipwrightTool[0],
            new int[0],
            new String[0],
            ShipwrightTool.MASTS,
            0),
    FURNITURE(EngineSetting.SHIPWRIGHT_TAB_FURNITURE,
            new ShipwrightTool[0],
            new int[0],
            new String[0],
            ShipwrightTool.FURNITURE,
            EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS);

    // Values
    public static final ShipwrightTab[] VALUES = values();

    // Identity
    private final String title;

    // Entries — the fixed options, each a tool, its option and the section it is listed under
    private final ShipwrightTool[] entryTools;
    private final int[] entryOptions;
    private final String[] entrySections;

    // Following — the tool whose options follow the fixed ones, from the option given, null for none
    private final ShipwrightTool followingTool;
    private final int followingOffset;

    // Constructor \\

    ShipwrightTab(
            String title,
            ShipwrightTool[] entryTools,
            int[] entryOptions,
            String[] entrySections,
            ShipwrightTool followingTool,
            int followingOffset) {

        // Identity
        this.title = title;

        // Entries
        this.entryTools = entryTools;
        this.entryOptions = entryOptions;
        this.entrySections = entrySections;

        // Following
        this.followingTool = followingTool;
        this.followingOffset = followingOffset;
    }

    // Lookup \\

    // The fixed entry a tool's option is listed as, INDEX_NOT_FOUND when this tab does not list it so
    public int findEntry(ShipwrightTool tool, int option) {

        for (int i = 0; i < entryTools.length; i++)
            if (entryTools[i] == tool && entryOptions[i] == option)
                return i;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // True when a tool's option follows this tab's fixed entries
    public boolean follows(ShipwrightTool tool, int option) {
        return followingTool == tool && option >= followingOffset;
    }

    // The tab a tool's option is listed in: a fixed entry first, then the tab whose options it follows
    public static ShipwrightTab of(ShipwrightTool tool, int option) {

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].findEntry(tool, option) != EngineSetting.INDEX_NOT_FOUND)
                return VALUES[i];

        for (int i = 0; i < VALUES.length; i++)
            if (VALUES[i].followingTool == tool)
                return VALUES[i];

        return STRUCTURE;
    }

    // The tab after this one, round to the first
    public ShipwrightTab next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    public int getEntryCount() {
        return entryTools.length;
    }

    public ShipwrightTool getEntryTool(int entry) {
        return entryTools[entry];
    }

    public int getEntryOption(int entry) {
        return entryOptions[entry];
    }

    public String getEntrySection(int entry) {
        return entrySections[entry];
    }

    public boolean hasFollowing() {
        return followingTool != null;
    }

    public ShipwrightTool getFollowingTool() {
        return followingTool;
    }

    public int getFollowingOffset() {
        return followingOffset;
    }

    // The tool a tab hands over when first taken up: its first fixed entry's, or the tool whose options it lists
    public ShipwrightTool getFirstTool() {
        return entryTools.length > 0 ? entryTools[0] : followingTool;
    }
}
