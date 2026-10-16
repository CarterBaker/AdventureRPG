package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ShipwrightEditBranch extends BranchPackage {

    /*
     * Works every change a drafting table makes to its blueprint, always on
     * the deck picked. Holding draw or erase starts a stroke at the cursor: a
     * deck's outline is drawn out to the cursor at every octant along the
     * sheet it crosses, mirrored either side of the centre line, and walls are
     * painted on the open floor it passes over; every other tool works the octant the stroke
     * began on once, setting a thing down or taking up whatever already stands
     * there. Every stroke, and every option worked from the menu, is one
     * change: the blueprint as it stood is kept to undo and the blueprint is
     * compiled. Redrawing a deck takes up whatever no longer stands on it and
     * is always kept, its shape's problems only shown; any other change that
     * leaves something built with no room to stand when nothing was is taken
     * back and the reason shown. Sheet octants arrive as texels, the plan's
     * starboard drawn down.
     */

    // Internal
    private ShipwrightManager shipwrightManager;

    // Base \\

    @Override
    protected void get() {
        this.shipwrightManager = get(ShipwrightManager.class);
    }

    // Stroke \\

    void beginStroke(ShipwrightSessionInstance session, boolean erasing) {

        if (!session.isStroking())
            session.beginStroke(erasing);
    }

    // Works the octant under the cursor, once per octant for a painted tool and only the first for any other
    void strokeAt(ShipwrightSessionInstance session, int u, int v) {

        if (!session.isStroking() || !session.moveStroke(u, v))
            return;

        if (!session.isStrokeStart() && !session.getTool().isPainted())
            return;

        if (apply(session, u, v, session.isStrokeErasing()))
            session.markStrokeChanged();
    }

    void endStroke(ShipwrightSessionInstance session) {

        if (!session.isStroking())
            return;

        boolean changed = session.isStrokeChanged();
        ShipBlueprintStruct before = session.endStroke();

        if (!changed)
            return;

        if (session.getTool() == ShipwrightTool.DECK) {
            session.getBlueprint().prunePlacements();
            commit(session, before, false);
            return;
        }

        commit(session, before, true);
    }

    // One octant worked by the tool in hand, true when the blueprint changed
    private boolean apply(ShipwrightSessionInstance session, int u, int v, boolean erasing) {

        int x = u - EngineSetting.SHIP_AFT_MARGIN;
        int z = EngineSetting.SHIP_SHEET_BREADTH - 1 - v;

        if (x < 0 || x >= EngineSetting.SHIP_SHEET_LENGTH || z < 0 || z >= EngineSetting.SHIP_SHEET_BREADTH)
            return false;

        ShipBlueprintStruct blueprint = session.getBlueprint();

        return switch (session.getTool()) {
            case DECK -> drawOutline(blueprint, session.getDeck(), x, z, erasing);
            case WALLS -> paintWall(session, x, z, erasing);
            case OPENINGS -> togglePlacement(session,
                    session.isWindows() ? blueprint.getWindows() : blueprint.getDoors(), x, z, erasing);
            case STAIRS -> togglePlacement(session, blueprint.getStairs(), x, z, erasing);
            case MASTS -> pickMast(session, x, erasing);
            case FURNITURE -> placeItem(session, x, z, erasing);
        };
    }

    // The deck's outline drawn out to the cursor at its octant along the sheet, or taken back to nothing there
    private boolean drawOutline(ShipBlueprintStruct blueprint, int deck, int x, int z, boolean erasing) {

        int halfBeam = erasing ? 0 : ShipCellUtility.toHalfBreadth(z) + 1;

        if (blueprint.getOutline(deck, x) == halfBeam)
            return false;

        blueprint.setOutline(deck, x, halfBeam);

        return true;
    }

    private boolean paintWall(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int deck = session.getDeck();

        if (erasing) {

            if (!blueprint.hasWall(deck, x, z))
                return false;

            blueprint.setWall(deck, x, z, false, false);

            return true;
        }

        if (!isOpenFloor(session, x, z) || blueprint.hasWallOf(deck, x, z, session.isHalfWalls()))
            return false;

        blueprint.setWall(deck, x, z, true, session.isHalfWalls());

        return true;
    }

    private boolean togglePlacement(
            ShipwrightSessionInstance session,
            IntArrayList cells,
            int x,
            int z,
            boolean erasing) {

        int cell = resolveCell(session, x, z);
        boolean standing = ShipBlueprintStruct.findPlacement(cells, cell) != EngineSetting.INDEX_NOT_FOUND;

        if (erasing && !standing || !standing && !session.getBlueprint().isOnDeck(session.getDeck(), x, z))
            return false;

        ShipBlueprintStruct.togglePlacement(cells, cell);

        return true;
    }

    // Picks the mast at an octant along the length, steps a new one there, or with erasing takes it down
    private boolean pickMast(ShipwrightSessionInstance session, int x, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int index = blueprint.findMast(x);

        if (index != EngineSetting.INDEX_NOT_FOUND) {

            if (!erasing) {
                session.setMastIndex(index);
                return false;
            }

            blueprint.getMasts().remove(index);
            session.setMastIndex(EngineSetting.INDEX_NOT_FOUND);

            return true;
        }

        int foot = x - EngineSetting.SHIP_MAST_OCTANTS / 2;

        if (erasing || !blueprint.canStepMast(foot)
                || blueprint.findLowestDeck(foot) == EngineSetting.SHIP_DECK_NONE
                || blueprint.findLowestDeck(foot + EngineSetting.SHIP_MAST_OCTANTS - 1) == EngineSetting.SHIP_DECK_NONE)
            return false;

        int height = shipwrightManager.resolveMastHeight(session);

        session.setMastIndex(blueprint.addMast(new ShipMastStruct(foot, height, false, true)));

        return true;
    }

    // Sets the helm, the capstan or a piece of furniture down at the cursor, or with erasing takes up whichever
    // of them stands there
    private boolean placeItem(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int cell = resolveCell(session, x, z);
        int place = ShipCellUtility.toPlace(cell);

        if (erasing)
            return takeUpItem(blueprint, cell, place);

        if (!blueprint.isOnDeck(session.getDeck(), x, z))
            return false;

        int item = Math.floorMod(session.getItemIndex(), resolveItemCount());

        if (item == EngineSetting.SHIPWRIGHT_ITEM_HELM) {

            if (cell == blueprint.getHelm())
                return false;

            blueprint.setHelm(cell);

            return true;
        }

        if (item == EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN) {

            if (cell == blueprint.getCapstan())
                return false;

            blueprint.setCapstan(cell);

            return true;
        }

        if (blueprint.findFurniture(cell) != EngineSetting.INDEX_NOT_FOUND)
            return false;

        blueprint.getFurniture().add(new ShipFurnitureStruct(
                shipwrightManager.getFurnishingName(item - EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS), cell));

        return true;
    }

    private boolean takeUpItem(ShipBlueprintStruct blueprint, int cell, int place) {

        int index = blueprint.findFurniture(cell);

        if (index != EngineSetting.INDEX_NOT_FOUND) {
            blueprint.getFurniture().remove(index);
            return true;
        }

        if (blueprint.hasHelm() && ShipCellUtility.toPlace(blueprint.getHelm()) == place) {
            blueprint.setHelm(EngineSetting.SHIP_CELL_NONE);
            return true;
        }

        if (blueprint.hasCapstan() && ShipCellUtility.toPlace(blueprint.getCapstan()) == place) {
            blueprint.setCapstan(EngineSetting.SHIP_CELL_NONE);
            return true;
        }

        return false;
    }

    // True when the deck picked was built with open floor at an octant, so a wall dragged across its edge stops there
    private boolean isOpenFloor(ShipwrightSessionInstance session, int x, int z) {

        ShipLayoutStruct layout = session.getLayout();
        ShipPiece piece = layout.getGrid().get(
                layout.getOriginX() + x, session.getDeck() * EngineSetting.SHIP_DECK_HEIGHT, z);

        return layout.getSectionLayers() > 0 && piece != null && piece.isFloor();
    }

    // The helm, the capstan and every furnishing table the drafting table offers
    private int resolveItemCount() {
        return EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS + shipwrightManager.getFurnishingCount();
    }

    // The packed cell at an octant on the deck picked, facing the way set
    private int resolveCell(ShipwrightSessionInstance session, int x, int z) {
        return ShipCellUtility.pack(session.getDeck(), x, z, session.getSpin());
    }

    // Decks \\

    void stepDeck(ShipwrightSessionInstance session, int step) {
        session.setDeck(clamp(session.getDeck() + step, 0, session.getBlueprint().getDeckCount() - 1));
    }

    // A deck laid on top of the stack, drawn as the deck under it to start from, and picked
    void addDeck(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipBlueprintStruct before = blueprint.copy();

        if (!blueprint.addDeck()) {
            session.setStatus(EngineSetting.SHIPWRIGHT_REFUSAL_DECKS_FULL);
            return;
        }

        session.setDeck(blueprint.getDeckCount() - 1);
        commit(session, before, false);
    }

    // The deck picked taken out with everything standing on it, the decks above lowered into its place
    void removeDeck(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipBlueprintStruct before = blueprint.copy();

        if (!blueprint.removeDeck(session.getDeck())) {
            session.setStatus(EngineSetting.SHIPWRIGHT_REFUSAL_LAST_DECK);
            return;
        }

        blueprint.prunePlacements();
        session.setDeck(Math.min(session.getDeck(), blueprint.getDeckCount() - 1));
        commit(session, before, false);
    }

    // Options \\

    // Hangs the porch off the stern at the deck picked, or takes it down when it already hangs there
    void togglePorch(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipBlueprintStruct before = blueprint.copy();
        int deck = session.getDeck();

        blueprint.setPorchDeck(blueprint.getPorchDeck() == deck ? EngineSetting.SHIP_DECK_NONE : deck);
        commit(session, before, true);
    }

    void turn(ShipwrightSessionInstance session) {
        session.setSpin((session.getSpin() + 1) % EngineSetting.ENCODED_FACE_SPIN_COUNT);
    }

    // Steps the option of the tool in hand: wall height, door or window, the furniture item, or the mast picked
    void stepOption(ShipwrightSessionInstance session, int step) {

        switch (session.getTool()) {
            case WALLS -> session.setHalfWalls(!session.isHalfWalls());
            case OPENINGS -> session.setWindows(!session.isWindows());
            case FURNITURE -> session.setItemIndex(Math.floorMod(session.getItemIndex() + step, resolveItemCount()));
            case MASTS -> stepMast(session, step);
            default -> stepDeck(session, step);
        }
    }

    private void stepMast(ShipwrightSessionInstance session, int step) {

        int count = session.getBlueprint().getMasts().size();

        if (count == 0)
            return;

        int index = session.hasMastPicked() ? Math.floorMod(session.getMastIndex() + step, count) : 0;

        session.setMastIndex(index);
    }

    // Masts \\

    void stepMastHeight(ShipwrightSessionInstance session, int step) {

        if (!session.hasMastPicked())
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();
        ShipMastStruct mast = session.getBlueprint().getMasts().get(session.getMastIndex());

        mast.setHeight(clamp(mast.getHeight() + step,
                EngineSetting.SHIP_MAST_MIN_HEIGHT, EngineSetting.SHIP_MAST_MAX_HEIGHT));
        commit(session, before, true);
    }

    void toggleCrowsNest(ShipwrightSessionInstance session) {

        if (!session.hasMastPicked())
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();
        ShipMastStruct mast = session.getBlueprint().getMasts().get(session.getMastIndex());

        mast.setCrowsNest(!mast.hasCrowsNest());
        commit(session, before, true);
    }

    void toggleRigging(ShipwrightSessionInstance session) {

        if (!session.hasMastPicked())
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();
        ShipMastStruct mast = session.getBlueprint().getMasts().get(session.getMastIndex());

        mast.setRigging(!mast.isRigged());
        commit(session, before, true);
    }

    // Naming \\

    // Names the ship what was typed, as one change, when it holds a letter or digit
    void confirmName(ShipwrightSessionInstance session, String displayName) {

        session.endNaming();

        if (displayName.isEmpty() || displayName.equals(session.getBlueprint().getDisplayName()))
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();

        session.getBlueprint().setDisplayName(displayName);
        commit(session, before, false);
    }

    // History \\

    void undo(ShipwrightSessionInstance session) {

        if (!session.undo())
            return;

        shipwrightManager.compileSession(session);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_UNDONE);
    }

    void redo(ShipwrightSessionInstance session) {

        if (!session.redo())
            return;

        shipwrightManager.compileSession(session);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_REDONE);
    }

    // Commit \\

    // Keeps a change once compiled; a strict change that leaves something built with no room to stand when nothing
    // was is taken back with the reason
    private void commit(ShipwrightSessionInstance session, ShipBlueprintStruct before, boolean strict) {

        boolean hadBuildProblem = session.getLayout().hasBuildProblem();

        session.pushUndo(before);
        shipwrightManager.compileSession(session);

        if (strict && !hadBuildProblem && session.getLayout().hasBuildProblem()) {

            String refusal = session.getLayout().getBuildProblem();

            session.discardLast();
            shipwrightManager.compileSession(session);
            session.setStatus(refusal);
            return;
        }

        session.markDirty();
        session.setStatus(session.getLayout().getProblem());
    }

    // Utility \\

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
