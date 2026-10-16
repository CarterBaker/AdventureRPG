package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ShipwrightEditBranch extends BranchPackage {

    /*
     * Works every change a drafting table makes to its blueprint. A press on
     * the sheet starts a stroke: a painted tool — the hull, mirrored either
     * side of the centre line, or walls — works every octant the pointer
     * crosses, any other tool works the octant pressed once, setting a thing
     * down or taking up whatever already stands there. Every stroke, and
     * every option worked from the menu, is one change: the blueprint as it
     * stood is kept to undo, the blueprint is compiled, and a change that
     * leaves the ship unbuildable when it was not is taken back and the
     * reason shown; a ship drawing more triangles than a ship may is told so
     * once it is surveyed, and cannot be saved until it draws fewer. Sheet
     * octants arrive as texels, the plan's starboard drawn down.
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
        session.beginStroke(erasing);
    }

    // Works the octant under the pointer, once per octant for a painted tool and only the first for any other
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

        if (changed)
            commit(session, before);
    }

    // One octant worked by the tool in hand, true when the blueprint changed
    private boolean apply(ShipwrightSessionInstance session, int u, int v, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int x = u - EngineSetting.SHIP_AFT_MARGIN;
        int z = session.getSheetHeight() - 1 - v;

        return switch (session.getTool()) {
            case HULL -> paintHull(blueprint, x, session.getLayer(), z, erasing);
            case DECKS -> toggleDeck(blueprint, v, erasing);
            case WALLS -> paintWall(session, x, z, erasing);
            case DOORS -> togglePlacement(session, blueprint.getDoors(), x, z, erasing);
            case STAIRS -> togglePlacement(session, blueprint.getStairs(), x, z, erasing);
            case FURNITURE -> toggleFurniture(session, x, z, erasing);
            case HELM -> placeHelm(session, x, z, erasing);
            case CAPSTAN -> placeCapstan(session, x, z, erasing);
            case MASTS -> pickMast(session, x, erasing);
        };
    }

    private boolean paintHull(ShipBlueprintStruct blueprint, int x, int y, int z, boolean erasing) {

        if (x < 0 || x >= blueprint.getLength() || z < 0 || z >= blueprint.getBreadth())
            return false;

        int halfZ = ShipCellUtility.toHalfBreadth(z, blueprint.getHalfBreadth());

        if (blueprint.isHull(x, y, halfZ) != erasing)
            return false;

        blueprint.setHull(x, y, halfZ, !erasing);

        return true;
    }

    private boolean toggleDeck(ShipBlueprintStruct blueprint, int y, boolean erasing) {

        if (y < 0 || y >= blueprint.getHeight() || erasing && !blueprint.hasDeck(y))
            return false;

        blueprint.toggleDeck(y);

        return true;
    }

    private boolean paintWall(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int level = session.getLevel();

        if (!isOnSheet(blueprint, x, z) || level == EngineSetting.SHIP_LEVEL_NONE
                || blueprint.hasWall(level, x, z) != erasing)
            return false;

        blueprint.setWall(level, x, z, !erasing);

        return true;
    }

    private boolean togglePlacement(
            ShipwrightSessionInstance session,
            IntArrayList cells,
            int x,
            int z,
            boolean erasing) {

        int cell = resolveCell(session, x, z);

        if (cell == EngineSetting.SHIP_CELL_NONE
                || erasing && ShipBlueprintStruct.findPlacement(cells, cell) == EngineSetting.INDEX_NOT_FOUND)
            return false;

        ShipBlueprintStruct.togglePlacement(cells, cell);

        return true;
    }

    private boolean toggleFurniture(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int cell = resolveCell(session, x, z);

        if (cell == EngineSetting.SHIP_CELL_NONE)
            return false;

        int index = blueprint.findFurniture(cell);

        if (index != EngineSetting.INDEX_NOT_FOUND) {
            blueprint.getFurniture().remove(index);
            return true;
        }

        String furnishingName = shipwrightManager.getFurnishingName(session.getFurnishingIndex());

        if (erasing || furnishingName == null)
            return false;

        blueprint.getFurniture().add(new ShipFurnitureStruct(furnishingName, cell));

        return true;
    }

    private boolean placeHelm(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int cell = erasing ? EngineSetting.SHIP_CELL_NONE : resolveCell(session, x, z);

        if (!erasing && cell == EngineSetting.SHIP_CELL_NONE || cell == blueprint.getHelm())
            return false;

        blueprint.setHelm(cell);

        return true;
    }

    private boolean placeCapstan(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int cell = erasing ? EngineSetting.SHIP_CELL_NONE : resolveCell(session, x, z);

        if (!erasing && cell == EngineSetting.SHIP_CELL_NONE || cell == blueprint.getCapstan())
            return false;

        blueprint.setCapstan(cell);

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

        if (erasing || blueprint.getMasts().size() >= EngineSetting.SHIP_MAX_MASTS || !blueprint.canStepMast(foot))
            return false;

        int height = Math.max(EngineSetting.SHIP_MAST_MIN_HEIGHT, Math.min(EngineSetting.SHIP_MAST_MAX_HEIGHT,
                Math.round(blueprint.getLength() * EngineSetting.SHIP_MAST_HEIGHT_SHARE)));

        session.setMastIndex(blueprint.addMast(new ShipMastStruct(foot, height, false, false)));

        return true;
    }

    // The packed cell at an octant on the level picked, facing the way set, SHIP_CELL_NONE when off the sheet
    private int resolveCell(ShipwrightSessionInstance session, int x, int z) {

        int level = session.getLevel();

        if (!isOnSheet(session.getBlueprint(), x, z) || level == EngineSetting.SHIP_LEVEL_NONE)
            return EngineSetting.SHIP_CELL_NONE;

        return ShipCellUtility.pack(level, x, z, session.getSpin());
    }

    private boolean isOnSheet(ShipBlueprintStruct blueprint, int x, int z) {
        return x >= 0 && x < blueprint.getLength() && z >= 0 && z < blueprint.getBreadth();
    }

    // Layers \\

    // Steps the hull layer or the level the tool in hand works on
    void stepLayer(ShipwrightSessionInstance session, int step) {

        if (session.getTool().getSheet().isPlan() && session.getTool() != ShipwrightTool.HULL) {

            int levels = session.getLayout().getLevels().size();

            if (levels > 0)
                session.setLevelIndex(Math.max(0, Math.min(levels - 1, session.getLevelIndex() + step)));

            return;
        }

        session.setLayer(Math.max(0, Math.min(session.getBlueprint().getHeight() - 1, session.getLayer() + step)));
    }

    void copyLayer(ShipwrightSessionInstance session, int step) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int from = session.getLayer() + step;

        if (from < 0 || from >= blueprint.getHeight())
            return;

        ShipBlueprintStruct before = blueprint.copy();

        blueprint.copyLayer(from, session.getLayer());
        commit(session, before);
    }

    void clearLayer(ShipwrightSessionInstance session) {

        ShipBlueprintStruct before = session.getBlueprint().copy();

        session.getBlueprint().clearLayer(session.getLayer());
        commit(session, before);
    }

    void stepWaterline(ShipwrightSessionInstance session, int step) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipBlueprintStruct before = blueprint.copy();

        blueprint.setWaterline(blueprint.getWaterline() + step);
        commit(session, before);
    }

    // Size \\

    void resize(ShipwrightSessionInstance session, int lengthStep, int halfBreadthStep, int heightStep) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int length = clamp(blueprint.getLength() + lengthStep,
                EngineSetting.SHIP_MIN_LENGTH, EngineSetting.SHIP_MAX_LENGTH);
        int halfBreadth = clamp(blueprint.getHalfBreadth() + halfBreadthStep,
                EngineSetting.SHIP_MIN_HALF_BREADTH, EngineSetting.SHIP_MAX_HALF_BREADTH);
        int height = clamp(blueprint.getHeight() + heightStep,
                EngineSetting.SHIP_MIN_HEIGHT, EngineSetting.SHIP_MAX_HEIGHT);

        if (length == blueprint.getLength() && halfBreadth == blueprint.getHalfBreadth()
                && height == blueprint.getHeight())
            return;

        ShipBlueprintStruct before = blueprint.copy();

        blueprint.resize(length, halfBreadth, height);
        session.setLayer(Math.min(session.getLayer(), height - 1));
        commit(session, before);
    }

    // Options \\

    void cycleRails(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipBlueprintStruct before = blueprint.copy();

        blueprint.setRailHeight((blueprint.getRailHeight() + 1) % (EngineSetting.SHIP_MAX_RAIL_HEIGHT + 1));
        commit(session, before);
    }

    // Hangs the porch off the stern at the level picked, or takes it down when it already hangs there
    void togglePorch(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int level = session.getLevel();

        if (level == EngineSetting.SHIP_LEVEL_NONE)
            return;

        ShipBlueprintStruct before = blueprint.copy();

        blueprint.setPorchLevel(blueprint.getPorchLevel() == level ? EngineSetting.SHIP_LEVEL_NONE : level);
        commit(session, before);
    }

    void turn(ShipwrightSessionInstance session) {
        session.setSpin((session.getSpin() + 1) % EngineSetting.ENCODED_FACE_SPIN_COUNT);
    }

    void stepFurnishing(ShipwrightSessionInstance session, int step) {

        int count = shipwrightManager.getFurnishingCount();

        if (count > 0)
            session.setFurnishingIndex(Math.floorMod(session.getFurnishingIndex() + step, count));
    }

    // Masts \\

    void stepMastHeight(ShipwrightSessionInstance session, int step) {

        if (!session.hasMastPicked())
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();
        ShipMastStruct mast = session.getBlueprint().getMasts().get(session.getMastIndex());

        mast.setHeight(clamp(mast.getHeight() + step,
                EngineSetting.SHIP_MAST_MIN_HEIGHT, EngineSetting.SHIP_MAST_MAX_HEIGHT));
        commit(session, before);
    }

    void toggleCrowsNest(ShipwrightSessionInstance session) {

        if (!session.hasMastPicked())
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();
        ShipMastStruct mast = session.getBlueprint().getMasts().get(session.getMastIndex());

        mast.setCrowsNest(!mast.hasCrowsNest());
        commit(session, before);
    }

    void toggleRigging(ShipwrightSessionInstance session) {

        if (!session.hasMastPicked())
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();
        ShipMastStruct mast = session.getBlueprint().getMasts().get(session.getMastIndex());

        mast.setRigging(!mast.isRigged());
        commit(session, before);
    }

    // Naming \\

    // Names the ship what was typed, as one change, when it holds a letter or digit
    void confirmName(ShipwrightSessionInstance session, String displayName) {

        session.endNaming();

        if (displayName.isEmpty() || displayName.equals(session.getBlueprint().getDisplayName()))
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();

        session.getBlueprint().setDisplayName(displayName);
        commit(session, before);
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

    // Keeps a change once it compiles no worse than the blueprint before it, or takes it back with the reason
    private void commit(ShipwrightSessionInstance session, ShipBlueprintStruct before) {

        boolean hadProblem = session.getLayout().hasProblem();

        session.pushUndo(before);
        shipwrightManager.compileSession(session);

        if (session.getLayout().hasProblem() && !hadProblem) {

            String refusal = session.getLayout().getProblem();

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
