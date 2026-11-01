package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipClimb;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOpening;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPickStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ShipwrightInspectBranch extends BranchPackage {

    /*
     * Says what stands where on the deck picked at a drafting table, the one
     * answer the cursor's description, picking things up, taking them away
     * and the sheet's highlight all share. Everything set down is found over
     * its whole footprint, not only the octant it was set at: furniture, the
     * helm and the capstan first, then doors and windows, ladders, boarding
     * ladders, stairs, hatches and last the masts. The footprint any tool's
     * option covers when set at a cell is worked out here too, so the ghost
     * of the thing in hand and the thing once set down cover the same
     * octants: a stair its whole run, a hatch the lid it was built with.
     */

    // Internal
    private ShipwrightManager shipwrightManager;
    private ShipwrightFittingBranch shipwrightFittingBranch;

    // Scratch — main thread only
    private ShipPickStruct footprint;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.footprint = new ShipPickStruct();
    }

    @Override
    protected void get() {
        this.shipwrightManager = get(ShipwrightManager.class);
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
    }

    // Pick \\

    // Whatever stands at an octant of the deck picked, written to the pick; an empty pick when nothing does
    void pick(ShipwrightSessionInstance session, int x, int z, ShipPickStruct pick) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int deck = session.getDeck();

        pick.clear();

        if (pickFurniture(session, blueprint, deck, x, z, pick))
            return;

        if (pickFitting(session, blueprint.getHelm(), EngineSetting.SHIPWRIGHT_ITEM_HELM, deck, x, z, pick)
                || pickFitting(session, blueprint.getCapstan(), EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN, deck, x, z,
                        pick))
            return;

        if (pickCells(session, ShipwrightTool.OPENINGS, ShipOpening.DOOR.ordinal(), blueprint.getDoors(), deck, x, z,
                pick)
                || pickCells(session, ShipwrightTool.OPENINGS, ShipOpening.WINDOW.ordinal(), blueprint.getWindows(),
                        deck, x, z, pick)
                || pickCells(session, ShipwrightTool.STAIRS, ShipClimb.LADDER.ordinal(), blueprint.getLadders(),
                        deck, x, z, pick)
                || pickCells(session, ShipwrightTool.STAIRS, ShipClimb.BOARDING.ordinal(),
                        blueprint.getBoardingLadders(), deck, x, z, pick)
                || pickCells(session, ShipwrightTool.STAIRS, ShipClimb.STAIR.ordinal(), blueprint.getStairs(), deck,
                        x, z, pick)
                || pickCells(session, ShipwrightTool.OPENINGS, ShipOpening.HATCH.ordinal(), blueprint.getHatches(),
                        deck, x, z, pick))
            return;

        pickMast(session, blueprint, x, z, pick);
    }

    private boolean pickFurniture(
            ShipwrightSessionInstance session,
            ShipBlueprintStruct blueprint,
            int deck,
            int x,
            int z,
            ShipPickStruct pick) {

        for (int i = 0; i < blueprint.getFurniture().size(); i++) {

            ShipFurnitureStruct piece = blueprint.getFurniture().get(i);

            if (ShipCellUtility.unpackLevel(piece.getCell()) != deck)
                continue;

            int item = shipwrightManager.findItemIndex(piece.getFurnishingName());

            resolveFootprint(session, ShipwrightTool.FURNITURE, item, piece.getCell(), footprint);

            if (footprint.covers(x, z)) {
                pick.set(footprint);
                pick.set(ShipwrightTool.FURNITURE, item, piece.getCell(), i);
                return true;
            }
        }

        return false;
    }

    private boolean pickFitting(
            ShipwrightSessionInstance session,
            int cell,
            int item,
            int deck,
            int x,
            int z,
            ShipPickStruct pick) {

        if (cell == EngineSetting.SHIP_CELL_NONE || ShipCellUtility.unpackLevel(cell) != deck)
            return false;

        resolveFootprint(session, ShipwrightTool.FURNITURE, item, cell, footprint);

        if (!footprint.covers(x, z))
            return false;

        pick.set(footprint);
        pick.set(ShipwrightTool.FURNITURE, item, cell, EngineSetting.INDEX_NOT_FOUND);

        return true;
    }

    private boolean pickCells(
            ShipwrightSessionInstance session,
            ShipwrightTool tool,
            int option,
            IntArrayList cells,
            int deck,
            int x,
            int z,
            ShipPickStruct pick) {

        for (int i = 0; i < cells.size(); i++) {

            int cell = cells.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) != deck)
                continue;

            resolveFootprint(session, tool, option, cell, footprint);

            if (footprint.covers(x, z)) {
                pick.set(footprint);
                pick.set(tool, option, cell, i);
                return true;
            }
        }

        return false;
    }

    private void pickMast(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int x, int z,
            ShipPickStruct pick) {

        for (int i = 0; i < blueprint.getMasts().size(); i++) {

            int cell = toMastCell(blueprint.getMasts().get(i).getX());

            resolveFootprint(session, ShipwrightTool.MASTS, i, cell, footprint);

            if (footprint.covers(x, z)) {
                pick.set(footprint);
                pick.set(ShipwrightTool.MASTS, i, cell, i);
                return;
            }
        }
    }

    // The cell a mast is kept at for a pick: its foot along the length, on the centre line
    private static int toMastCell(int footX) {
        return ShipCellUtility.pack(0, footX, EngineSetting.SHIP_SHEET_HALF_BREADTH, 0);
    }

    // Footprint \\

    // The octants of the sheet a tool's option covers once set down at a cell, written to the pick's footprint
    void resolveFootprint(
            ShipwrightSessionInstance session,
            ShipwrightTool tool,
            int option,
            int cell,
            ShipPickStruct out) {

        int x = ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell);
        int spin = ShipCellUtility.unpackSpin(cell);

        out.setFootprint(x, z, x + 1, z + 1);

        switch (tool) {
            case FURNITURE -> resolveItemFootprint(option, x, z, spin, out);
            case OPENINGS -> resolveOpeningFootprint(session.getLayout(), option, cell, out);
            case STAIRS -> resolveClimbFootprint(option, x, z, spin, out);
            case MASTS -> out.setFootprint(x, z - 1, x + EngineSetting.SHIP_MAST_OCTANTS, z + 1);
            default -> {
            }
        }
    }

    private void resolveItemFootprint(int item, int x, int z, int spin, ShipPickStruct out) {

        int fittings = EngineSetting.SHIP_FITTING_OCTANTS;

        if (item == EngineSetting.SHIPWRIGHT_ITEM_HELM) {
            out.setFootprint(x, z, x + 1, z + fittings);
            return;
        }

        if (item == EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN) {
            out.setFootprint(x, z, x + fittings, z + fittings);
            return;
        }

        if (item == EngineSetting.INDEX_NOT_FOUND)
            return;

        shipwrightFittingBranch.resolveFootprint(shipwrightManager.getItemFurnishingName(item), spin);
        out.setFootprint(x, z, x + shipwrightFittingBranch.getFootprint(EngineSetting.AXIS_X),
                z + shipwrightFittingBranch.getFootprint(EngineSetting.AXIS_Z));
    }

    // A door its door's footprint, a window its octant, a hatch the lid it was built with or the square it cuts
    private void resolveOpeningFootprint(ShipLayoutStruct layout, int option, int cell, ShipPickStruct out) {

        int x = ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell);

        if (option == ShipOpening.DOOR.ordinal()) {
            shipwrightFittingBranch.resolveFootprint(EngineSetting.SHIP_DOOR_FURNISHING,
                    ShipCellUtility.unpackSpin(cell));
            out.setFootprint(x, z, x + shipwrightFittingBranch.getFootprint(EngineSetting.AXIS_X),
                    z + shipwrightFittingBranch.getFootprint(EngineSetting.AXIS_Z));
            return;
        }

        if (option != ShipOpening.HATCH.ordinal())
            return;

        int size = EngineSetting.SHIP_HATCH_OCTANTS;
        IntArrayList boxes = layout.getHatchBoxes();
        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int gridX = layout.getOriginX() + x;

        out.setFootprint(x, z, x + size, z + size);

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++)
            if (ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Y) == floor
                    && gridX >= ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_X)
                    && gridX < ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_X)
                    && z >= ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Z)
                    && z < ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Z)) {
                out.setFootprint(
                        ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_X) - layout.getOriginX(),
                        ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Z),
                        ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_X) - layout.getOriginX(),
                        ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Z));
                return;
            }
    }

    // A stair its whole run from its foot, a ladder its square, a boarding ladder its width along the deck's edge
    private void resolveClimbFootprint(int option, int x, int z, int spin, ShipPickStruct out) {

        int width = EngineSetting.SHIP_STAIR_WIDTH;

        if (option == ShipClimb.LADDER.ordinal()) {
            out.setFootprint(x, z, x + EngineSetting.SHIP_HATCH_OCTANTS, z + EngineSetting.SHIP_HATCH_OCTANTS);
            return;
        }

        if (option == ShipClimb.BOARDING.ordinal()) {
            out.setFootprint(x, z, x + width, z + 1);
            return;
        }

        int run = EngineSetting.SHIP_DECK_HEIGHT - 1;
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);
        int endX = x + stepX * run + (stepX == 0 ? width - 1 : 0);
        int endZ = z + stepZ * run + (stepZ == 0 ? width - 1 : 0);

        out.setFootprint(Math.min(x, endX), Math.min(z, endZ), Math.max(x, endX) + 1, Math.max(z, endZ) + 1);
    }
}
