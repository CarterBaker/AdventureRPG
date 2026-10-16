package application.bootstrap.shipwrightpipeline.shipwright;

import engine.root.EngineSetting;

public enum ShipwrightTool {

    /*
     * What a press on the drafting sheet does. The hull is painted a layer at
     * a time on its plan, mirrored either side of the centre line, and decks
     * are laid by layer on the side elevation. Walls are painted, and doors,
     * stairs, furniture, the helm and the capstan set down, on the plan of
     * one level, none of them mirrored. Masts are stepped on the plan of the
     * whole ship. Each tool names the sheet it draws on.
     */

    HULL(EngineSetting.SHIPWRIGHT_TOOL_HULL, ShipwrightSheet.HULL_PLAN, true),
    DECKS(EngineSetting.SHIPWRIGHT_TOOL_DECKS, ShipwrightSheet.SIDE_ELEVATION, false),
    WALLS(EngineSetting.SHIPWRIGHT_TOOL_WALLS, ShipwrightSheet.LEVEL_PLAN, true),
    DOORS(EngineSetting.SHIPWRIGHT_TOOL_DOORS, ShipwrightSheet.LEVEL_PLAN, false),
    STAIRS(EngineSetting.SHIPWRIGHT_TOOL_STAIRS, ShipwrightSheet.LEVEL_PLAN, false),
    FURNITURE(EngineSetting.SHIPWRIGHT_TOOL_FURNITURE, ShipwrightSheet.LEVEL_PLAN, false),
    HELM(EngineSetting.SHIPWRIGHT_TOOL_HELM, ShipwrightSheet.LEVEL_PLAN, false),
    CAPSTAN(EngineSetting.SHIPWRIGHT_TOOL_CAPSTAN, ShipwrightSheet.LEVEL_PLAN, false),
    MASTS(EngineSetting.SHIPWRIGHT_TOOL_MASTS, ShipwrightSheet.DECK_PLAN, false);

    // Identity
    private final String title;
    private final ShipwrightSheet sheet;
    private final boolean painted;

    // Constructor \\

    ShipwrightTool(String title, ShipwrightSheet sheet, boolean painted) {

        // Identity
        this.title = title;
        this.sheet = sheet;
        this.painted = painted;
    }

    // Accessible \\

    public String getTitle() {
        return title;
    }

    public ShipwrightSheet getSheet() {
        return sheet;
    }

    // True for a tool dragged across the sheet, false for one that sets a single thing down per press
    public boolean isPainted() {
        return painted;
    }
}
