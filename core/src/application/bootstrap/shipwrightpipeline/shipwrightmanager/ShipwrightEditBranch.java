package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipClimb;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOpening;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPickStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.shipwright.ShipWall;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTab;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import application.bootstrap.soundpipeline.soundcuemanager.SoundCueManager;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ShipwrightEditBranch extends BranchPackage {

    /*
     * Works every change a drafting table makes to its blueprint, always on
     * the deck picked. Holding draw or erase starts a stroke at the cursor: a
     * deck's outline is drawn out to the cursor at every octant along the
     * sheet it crosses, mirrored either side of the centre line; the waist is
     * cut through the open floor it passes over, mirrored too, erase filling
     * it back in and taking up any cover laid there, and every octant of the
     * bottom deck's waist a stroke cuts, or uncovers, is glazed over with a
     * glass floor so the sea shows below — one taken up afterwards leaves a
     * diving bay open to the sea; and walls, half walls, railings and lips
     * are painted alike on the open floor it passes over, in place of the
     * waist's rail or a bulwark standing there, and all but a full wall in
     * place of the ship's side, opening it right through, above the
     * waterline wherever they open the ship to the sea; erase takes any of
     * them down and so puts the side or bulwark back.
     * Every other tool works the
     * octant the stroke began on once, the same way for everything: erase
     * takes away whatever stands there, found over its whole footprint;
     * draw over something already set down picks it up into the hand — the
     * tool, the option and the quarter turn that set it down — so the next
     * draw sets it down again wherever it is wanted, but a cannon in hand
     * drawn over a gun platform with none on it is set down on it, where the
     * platform stands and facing its way; draw anywhere else sets the thing
     * in hand down, or refuses with the reason when it cannot stand there.
     * Why the thing in hand could not stand at an octant is one rule per
     * kind, asked for the ghost under the cursor and again before anything
     * is set down, and enforced once more by the compile. Only the
     * outline and the waist are mirrored. A mast is only stepped or taken
     * down; its height, sails and rigging are worked out from the hull. A
     * door or a window is set on the wall or side it is cut through and turns
     * itself to face across it, a boarding ladder faces outboard, and taking
     * a wall down takes every door and window in it along. The catalogue is
     * four tabs, each listing the options of several tools, picked by index
     * or stepped through, every option taken into the hand opening the tab it
     * is listed in and kept as the one that tab hands back. Every
     * stroke, and every option worked from the menu, is one change: the
     * blueprint as it stood is kept to undo and the blueprint is compiled.
     * Redrawing a deck takes up whatever no longer stands on it and is always
     * kept, its shape's problems only shown; any other change that leaves
     * something built with no room to stand when nothing was is taken back
     * and the reason shown. Sheet octants arrive as texels, the plan's
     * starboard drawn down.
     */

    // Internal
    private ShipwrightManager shipwrightManager;
    private ShipwrightHullBranch shipwrightHullBranch;
    private ShipwrightFittingBranch shipwrightFittingBranch;
    private ShipwrightInspectBranch shipwrightInspectBranch;
    private SoundCueManager soundCueManager;

    // Cues
    private int placeCueID;
    private int refusedCueID;

    // Scratch — main thread only
    private ShipPickStruct pickScratch;
    private ShipPickStruct footprintScratch;
    private IntArrayList glazedScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.pickScratch = new ShipPickStruct();
        this.footprintScratch = new ShipPickStruct();
        this.glazedScratch = new IntArrayList();
    }

    @Override
    protected void get() {
        this.shipwrightManager = get(ShipwrightManager.class);
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
        this.shipwrightInspectBranch = get(ShipwrightInspectBranch.class);
        this.soundCueManager = get(SoundCueManager.class);
    }

    @Override
    protected void awake() {

        // Cues
        this.placeCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIPWRIGHT_PLACE);
        this.refusedCueID = soundCueManager.getCueIDFromCueName(EngineSetting.SOUND_CUE_SHIPWRIGHT_REFUSED);
    }

    // Stroke \\

    void beginStroke(ShipwrightSessionInstance session, boolean erasing) {

        if (session.isStroking())
            return;

        session.beginStroke(erasing, session.getLayout().hasBuildProblem());
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_NONE);

        if (isGlazing(session))
            collectGlazed(session, session.getBlueprint());
    }

    // Works the octant under the cursor, once per octant for a painted hand and only the first for any other; walls,
    // the waist and windows are built again at every octant they change, so the sheet shows them as they are painted
    // and erased
    void strokeAt(ShipwrightSessionInstance session, int u, int v) {

        if (!session.isStroking() || !session.moveStroke(u, v))
            return;

        if (!session.isStrokeStart() && !isPaintedHand(session))
            return;

        if (!apply(session, u, v, session.isStrokeErasing()))
            return;

        session.markStrokeChanged();

        if (session.getTool() != ShipwrightTool.DECK && isPaintedHand(session))
            shipwrightManager.compileSession(session);
    }

    // True when the thing in hand is painted octant by octant as the cursor is dragged: the deck, the waist, every
    // wall and windows
    boolean isPaintedHand(ShipwrightSessionInstance session) {
        return session.getTool().isPainted()
                || session.getTool() == ShipwrightTool.OPENINGS && session.getOpening() == ShipOpening.WINDOW;
    }

    // True when a stroke cuts the bottom deck's waist, whose octants are glazed once it ends
    private boolean isGlazing(ShipwrightSessionInstance session) {
        return session.getTool() == ShipwrightTool.WAIST && session.getDeck() == 0;
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
            commit(session, before, false, session.getStatus(), session.hadStrokeBuildProblem());
            return;
        }

        if (commit(session, before, true, session.getStatus(), session.hadStrokeBuildProblem()) && isGlazing(session))
            glazeWaist(session, before);
    }

    // One octant worked by the tool in hand, true when the blueprint changed
    private boolean apply(ShipwrightSessionInstance session, int u, int v, boolean erasing) {

        int x = toSheetX(u);
        int z = toSheetZ(v);

        if (x < 0 || x >= EngineSetting.SHIP_SHEET_LENGTH || z < 0 || z >= EngineSetting.SHIP_SHEET_BREADTH)
            return false;

        return switch (session.getTool()) {
            case DECK -> drawOutline(session.getBlueprint(), session.getDeck(), x, z, erasing);
            case WAIST -> paintWaist(session, x, z, erasing);
            case WALLS -> paintWall(session, x, z, erasing);
            case OPENINGS -> session.getOpening() == ShipOpening.WINDOW
                    ? paintWindow(session, x, z, erasing)
                    : pickOrPlace(session, x, z, erasing);
            default -> pickOrPlace(session, x, z, erasing);
        };
    }

    // Paints a window on the wall or side at an octant, found across the side however it curves, or with erase takes
    // the window there away, as walls are painted: a stroke dragged along a wall glazes every octant it crosses
    private boolean paintWindow(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        IntArrayList windows = session.getBlueprint().getWindows();
        int cell = resolveWallCell(session, ShipOpening.WINDOW, x, z);
        int index = ShipBlueprintStruct.findPlacement(windows, cell);

        if (index == EngineSetting.INDEX_NOT_FOUND)
            index = ShipBlueprintStruct.findPlacement(windows, ShipCellUtility.pack(session.getDeck(), x, z, 0));

        if (erasing) {

            if (index == EngineSetting.INDEX_NOT_FOUND)
                return false;

            windows.removeInt(index);

            return true;
        }

        if (index != EngineSetting.INDEX_NOT_FOUND)
            return false;

        String problem = findOpeningProblem(session, x, z, cell);

        if (!problem.isEmpty()) {

            if (session.isStrokeStart())
                refuse(session, problem);

            return false;
        }

        ShipBlueprintStruct.togglePlacement(windows, cell);

        return true;
    }

    // The deck's outline drawn out to the cursor at its octant along the sheet, or taken back to nothing there
    private boolean drawOutline(ShipBlueprintStruct blueprint, int deck, int x, int z, boolean erasing) {

        int halfBeam = erasing ? 0 : ShipCellUtility.toHalfBreadth(z) + 1;

        if (blueprint.getOutline(deck, x) == halfBeam)
            return false;

        blueprint.setOutline(deck, x, halfBeam);

        return true;
    }

    // Cuts the waist through the deck picked at an octant and its mirror, or fills both back in with any cover set at
    // either; only open floor with open room or the sea below it is cut, so a stroke dragged across the deck stops at
    // whatever stands on it
    private boolean paintWaist(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int deck = session.getDeck();
        int mirrorZ = EngineSetting.SHIP_SHEET_BREADTH - 1 - z;

        if (erasing) {

            if (!blueprint.hasWaist(deck, x, z))
                return false;

            blueprint.setWaist(deck, x, z, false);
            removeCovers(blueprint, ShipCellUtility.pack(deck, x, z, 0));
            removeCovers(blueprint, ShipCellUtility.pack(deck, x, mirrorZ, 0));

            return true;
        }


        if (blueprint.hasWaist(deck, x, z) || !isCuttableFloor(session, x, z)
                || !isCuttableFloor(session, x, mirrorZ))
            return false;

        blueprint.setWaist(deck, x, z, true);

        return true;
    }

    // True when the deck picked is drawn and was built with open floor at an octant, nothing standing on it but the
    // rail round the waist already cut beside it, and open room or, under the bottom deck, the sea below
    private boolean isCuttableFloor(ShipwrightSessionInstance session, int x, int z) {

        ShipLayoutStruct layout = session.getLayout();
        int gridX = layout.getOriginX() + x;
        int floor = session.getDeck() * EngineSetting.SHIP_DECK_HEIGHT;
        ShipPiece standing = layout.getGrid().get(gridX, floor + 1, z);

        return session.getBlueprint().isOnDeck(session.getDeck(), x, z) && isOpenFloor(session, x, z)
                && (standing == null || standing == ShipPiece.RAILS)
                && shipwrightHullBranch.isOpenBelow(layout, x, floor, z);
    }

    // Paints the wall picked, any kind under the one rule: on open floor, in place of the waist's rail or a bulwark,
    // and every kind but a full wall in place of the side; erase takes a wall down with its doors and windows, putting
    // back whatever it stood in place of
    private boolean paintWall(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipWall wall = session.getWall();
        int deck = session.getDeck();

        if (erasing) {

            if (!blueprint.hasWall(deck, x, z))
                return false;

            int place = ShipCellUtility.pack(deck, x, z, 0);

            blueprint.setWall(deck, x, z, null);
            removePlacement(blueprint.getDoors(), place);
            removePlacement(blueprint.getWindows(), place);

            return true;
        }

        if (blueprint.getWall(deck, x, z) == wall)
            return false;

        String problem = shipwrightHullBranch.findWallProblem(session.getLayout(), deck, x, z, wall,
                blueprint.hasWall(deck, x, z));

        if (!problem.isEmpty()) {

            if (session.isStrokeStart())
                refuse(session, problem);

            return false;
        }

        blueprint.setWall(deck, x, z, wall);

        return true;
    }

    // Glazing \\

    // Every octant of the bottom deck's waist a glass floor lay over before a stroke, so one the stroke uncovers is
    // glazed again
    private void collectGlazed(ShipwrightSessionInstance session, ShipBlueprintStruct before) {

        IntArrayList waists = before.getWaists();

        glazedScratch.clear();

        for (int i = 0; i < waists.size(); i++) {

            int cell = waists.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) == 0 && shipwrightHullBranch.isCovered(session.getLayout(),
                    ShipOpening.GLASS, 0, ShipCellUtility.unpackX(cell), ShipCellUtility.unpackZ(cell)))
                glazedScratch.add(cell);
        }
    }

    // A glass floor laid over every octant of the bottom deck's waist the stroke cut or uncovered that no cover lies
    // over, the ship compiled after each, so the sea is seen below wherever the waist was not left open on purpose
    private void glazeWaist(ShipwrightSessionInstance session, ShipBlueprintStruct before) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        IntArrayList waists = blueprint.getWaists();

        for (int i = 0; i < waists.size(); i++) {

            int cell = waists.getInt(i);
            int x = ShipCellUtility.unpackX(cell);
            int z = ShipCellUtility.unpackZ(cell);

            if (ShipCellUtility.unpackLevel(cell) != 0
                    || before.hasWaist(0, x, z) && !glazedScratch.contains(cell)
                    || isCovered(session.getLayout(), x, z)
                    || ShipBlueprintStruct.findPlacement(blueprint.getGlassFloors(), cell)
                            != EngineSetting.INDEX_NOT_FOUND)
                continue;

            ShipBlueprintStruct.togglePlacement(blueprint.getGlassFloors(), cell);
            shipwrightManager.compileSession(session);
        }

        glazedScratch.clear();
    }

    // True when a grate or a glass floor lies over an octant of the bottom deck
    private boolean isCovered(ShipLayoutStruct layout, int x, int z) {
        return shipwrightHullBranch.isCovered(layout, ShipOpening.GLASS, 0, x, z)
                || shipwrightHullBranch.isCovered(layout, ShipOpening.GRATE, 0, x, z);
    }

    // Every grate or glass floor set at a place taken up
    private static void removeCovers(ShipBlueprintStruct blueprint, int place) {

        for (int i = 0; i < ShipOpening.VALUES.length; i++)
            if (ShipOpening.VALUES[i].isCover())
                removePlacement(blueprint.getOpenings(ShipOpening.VALUES[i]), place);
    }

    // Pick \\

    // Erase takes away whatever stands at an octant; draw picks it up into the hand, or with nothing there — or a gun
    // platform for the cannon in hand — sets the thing in hand down
    private boolean pickOrPlace(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipPickStruct pick = pickScratch;

        shipwrightInspectBranch.pick(session, x, z, pick);

        if (pick.isEmpty() || !erasing && isMounting(session, pick))
            return !erasing && place(session, x, z);

        takeAway(session, pick);

        if (erasing) {
            session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_REMOVED
                    + shipwrightManager.getOptionName(session, pick.getTool(), pick.getOption()));
            return true;
        }

        hold(session, pick.getTool(),
                pick.getTool() == ShipwrightTool.MASTS ? EngineSetting.INDEX_NOT_FOUND : pick.getOption());
        session.setSpin(ShipCellUtility.unpackSpin(pick.getCell()));

        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_PICKED_UP
                + shipwrightManager.getOptionName(session, pick.getTool(), pick.getOption()));

        return true;
    }

    // Whatever a pick found taken out of the blueprint
    private void takeAway(ShipwrightSessionInstance session, ShipPickStruct pick) {

        ShipBlueprintStruct blueprint = session.getBlueprint();

        switch (pick.getTool()) {
            case OPENINGS -> blueprint.getOpenings(ShipOpening.VALUES[pick.getOption()]).removeInt(pick.getIndex());
            case STAIRS -> blueprint.getClimbs(ShipClimb.VALUES[pick.getOption()]).removeInt(pick.getIndex());
            case MASTS -> {
                blueprint.getMasts().remove(pick.getIndex());
                session.setMastIndex(EngineSetting.INDEX_NOT_FOUND);
            }
            default -> takeAwayItem(blueprint, pick);
        }
    }

    private void takeAwayItem(ShipBlueprintStruct blueprint, ShipPickStruct pick) {

        if (pick.getIndex() != EngineSetting.INDEX_NOT_FOUND)
            blueprint.getFurniture().remove(pick.getIndex());
        else if (pick.getOption() == EngineSetting.SHIPWRIGHT_ITEM_HELM)
            blueprint.setHelm(EngineSetting.SHIP_CELL_NONE);
        else
            blueprint.setCapstan(EngineSetting.SHIP_CELL_NONE);
    }

    // Place \\

    // The thing in hand set down at an octant, or refused with the reason it cannot stand there
    private boolean place(ShipwrightSessionInstance session, int x, int z) {

        String problem = findPlacementProblem(session, x, z);

        if (!problem.isEmpty())
            return refuse(session, problem);

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int cell = resolvePlacementCell(session, x, z);

        switch (session.getTool()) {
            case OPENINGS -> ShipBlueprintStruct.togglePlacement(blueprint.getOpenings(session.getOpening()), cell);
            case STAIRS -> ShipBlueprintStruct.togglePlacement(blueprint.getClimbs(session.getClimb()), cell);
            case MASTS -> session.setMastIndex(blueprint.addMast(new ShipMastStruct(ShipCellUtility.unpackX(cell))));
            default -> placeItem(session, cell);
        }

        return true;
    }

    // Sets the helm, the capstan or a piece of furniture down at a cell
    private void placeItem(ShipwrightSessionInstance session, int cell) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int item = resolveHeldItem(session);

        if (item == EngineSetting.SHIPWRIGHT_ITEM_HELM)
            blueprint.setHelm(cell);
        else if (item == EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN)
            blueprint.setCapstan(cell);
        else
            blueprint.getFurniture().add(new ShipFurnitureStruct(shipwrightManager.getItemFurnishingName(item), cell));
    }

    // Why the thing in hand could not be set down at an octant of the deck picked, SHIP_PROBLEM_NONE when it could:
    // the one rule the ghost, the table and, once more, the compile all keep
    String findPlacementProblem(ShipwrightSessionInstance session, int x, int z) {

        if (session.getTool().isPainted())
            return EngineSetting.SHIP_PROBLEM_NONE;

        if (!session.getBlueprint().isOnDeck(session.getDeck(), x, z))
            return EngineSetting.SHIPWRIGHT_PROBLEM_OFF_DECK;

        ShipLayoutStruct layout = session.getLayout();
        int cell = resolvePlacementCell(session, x, z);

        return switch (session.getTool()) {
            case OPENINGS -> findOpeningProblem(session, x, z, cell);
            case STAIRS -> switch (session.getClimb()) {
                case STAIR -> shipwrightHullBranch.findStairProblem(layout, cell);
                case LADDER -> shipwrightHullBranch.findLadderProblem(layout, cell);
                case BOARDING -> shipwrightHullBranch.findBoardingProblem(layout, cell);
            };
            case MASTS -> findMastProblem(session.getBlueprint(), ShipCellUtility.unpackX(cell));
            default -> findItemProblem(session, cell);
        };
    }

    // A hatch where a hatch may lie, a cover over an opening, a gun platform facing the side; a door or window only
    // on a wall or the side
    private String findOpeningProblem(ShipwrightSessionInstance session, int x, int z, int cell) {

        if (session.getOpening() == ShipOpening.HATCH)
            return shipwrightHullBranch.findHatchProblem(session.getLayout(), cell);

        if (session.getOpening().isCover())
            return shipwrightHullBranch.findCoverProblem(session.getLayout(), cell);

        if (session.getOpening() == ShipOpening.PLATFORM)
            return findPlatformProblem(session, cell);

        return shipwrightFittingBranch.isOpeningWall(session.getLayout(), session.getDeck(), x, z,
                resolveOpeningSill(session.getOpening()))
                ? EngineSetting.SHIP_PROBLEM_NONE
                : EngineSetting.SHIPWRIGHT_PROBLEM_OPENING_WALL;
    }

    // A mast's foot on the sheet clear of every other mast, over the hull at both its octants
    private String findMastProblem(ShipBlueprintStruct blueprint, int foot) {

        if (!blueprint.canStepMast(foot)
                || blueprint.findLowestDeck(foot) == EngineSetting.SHIP_DECK_NONE
                || blueprint.findLowestDeck(foot + EngineSetting.SHIP_MAST_OCTANTS - 1) == EngineSetting.SHIP_DECK_NONE)
            return EngineSetting.SHIPWRIGHT_PROBLEM_MAST_ROOM;

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    // A gun platform's whole footprint clear of anything else set down, then the platform's own rule
    private String findPlatformProblem(ShipwrightSessionInstance session, int cell) {

        ShipPickStruct footprint = footprintScratch;
        ShipPickStruct pick = pickScratch;

        shipwrightInspectBranch.resolveFootprint(session, ShipwrightTool.OPENINGS, ShipOpening.PLATFORM.ordinal(), cell,
                footprint);

        for (int z = footprint.getMinZ(); z < footprint.getMaxZ(); z++)
            for (int x = footprint.getMinX(); x < footprint.getMaxX(); x++) {

                shipwrightInspectBranch.pick(session, x, z, pick);

                if (!pick.isEmpty())
                    return EngineSetting.SHIPWRIGHT_PROBLEM_OCCUPIED;
            }

        return shipwrightFittingBranch.findPlatformProblem(session.getLayout(), cell);
    }

    // An item's whole footprint on open floor with nothing else set down over any of it; a cannon on a gun platform
    private String findItemProblem(ShipwrightSessionInstance session, int cell) {

        ShipPickStruct footprint = footprintScratch;
        ShipPickStruct pick = pickScratch;
        int item = resolveHeldItem(session);

        if (isCannon(item))
            return shipwrightFittingBranch.findCannonProblem(session.getBlueprint(), cell,
                    EngineSetting.INDEX_NOT_FOUND);

        shipwrightInspectBranch.resolveFootprint(session, ShipwrightTool.FURNITURE, item, cell, footprint);

        for (int z = footprint.getMinZ(); z < footprint.getMaxZ(); z++)
            for (int x = footprint.getMinX(); x < footprint.getMaxX(); x++) {

                if (!session.getBlueprint().isOnDeck(session.getDeck(), x, z) || !isClearFloor(session, x, z))
                    return EngineSetting.SHIPWRIGHT_PROBLEM_FURNITURE_FLOOR;

                shipwrightInspectBranch.pick(session, x, z, pick);

                if (!pick.isEmpty())
                    return EngineSetting.SHIPWRIGHT_PROBLEM_OCCUPIED;
            }

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    // The packed cell the thing in hand is set down at from an octant: facing the way set, a door or window across
    // its wall, a boarding ladder outboard, a mast's foot centred on the octant along the length, a cannon where the
    // gun platform under the octant stands
    int resolvePlacementCell(ShipwrightSessionInstance session, int x, int z) {

        int deck = session.getDeck();
        int spin = session.getSpin();

        return switch (session.getTool()) {
            case OPENINGS -> session.getOpening().isTurned()
                    ? ShipCellUtility.pack(deck, x, z, spin)
                    : resolveWallCell(session, session.getOpening(), x, z);
            case STAIRS -> ShipCellUtility.pack(deck, x, z, session.getClimb() == ShipClimb.BOARDING
                    ? resolveOutboardSpin(z) : spin);
            case MASTS -> ShipCellUtility.pack(0, Math.max(0, x - EngineSetting.SHIP_MAST_OCTANTS / 2),
                    EngineSetting.SHIP_SHEET_HALF_BREADTH, 0);
            case FURNITURE -> isCannon(resolveHeldItem(session))
                    ? resolveMountCell(session, x, z, ShipCellUtility.pack(deck, x, z, spin))
                    : ShipCellUtility.pack(deck, x, z, spin);
            default -> ShipCellUtility.pack(deck, x, z, spin);
        };
    }

    // The cell a door or window set at an octant of the deck picked is kept at: the wall, bulwark or planking found
    // across the side from it at the opening's height, however the side curves, facing across that wall
    private int resolveWallCell(ShipwrightSessionInstance session, ShipOpening opening, int x, int z) {

        ShipLayoutStruct layout = session.getLayout();
        int deck = session.getDeck();
        int sill = resolveOpeningSill(opening);
        int wallZ = shipwrightFittingBranch.resolveOpeningZ(layout, deck, x, z, sill);
        int cellZ = wallZ == EngineSetting.INDEX_NOT_FOUND ? z : wallZ;

        return ShipCellUtility.pack(deck, x, cellZ,
                shipwrightFittingBranch.resolveOpeningSpin(layout, deck, x, cellZ, sill, session.getSpin()));
    }

    // The cell of the gun platform whose footprint covers an octant of the deck picked, the cell given when none does
    private int resolveMountCell(ShipwrightSessionInstance session, int x, int z, int cell) {

        IntArrayList platforms = session.getBlueprint().getGunPlatforms();

        for (int i = 0; i < platforms.size(); i++) {

            int platform = platforms.getInt(i);

            if (ShipCellUtility.unpackLevel(platform) != session.getDeck())
                continue;

            shipwrightInspectBranch.resolveFootprint(session, ShipwrightTool.OPENINGS, ShipOpening.PLATFORM.ordinal(),
                    platform, footprintScratch);

            if (footprintScratch.covers(x, z))
                return platform;
        }

        return cell;
    }

    // True when the cannon in hand would be set down on the gun platform a pick found, none standing on it yet
    boolean isMounting(ShipwrightSessionInstance session, ShipPickStruct pick) {
        return session.getTool() == ShipwrightTool.FURNITURE && isCannon(resolveHeldItem(session))
                && pick.getTool() == ShipwrightTool.OPENINGS && pick.getOption() == ShipOpening.PLATFORM.ordinal()
                && shipwrightFittingBranch.findCannonProblem(session.getBlueprint(), pick.getCell(),
                        EngineSetting.INDEX_NOT_FOUND).isEmpty();
    }

    private boolean isCannon(int item) {
        return item >= EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS
                && shipwrightManager.getItemFurnishingName(item).equals(EngineSetting.SHIP_CANNON_FURNISHING);
    }

    // The quarter turn facing away from the centre line on the side an octant lies
    private static int resolveOutboardSpin(int z) {
        return z >= EngineSetting.SHIP_SHEET_HALF_BREADTH ? 1 : EngineSetting.ENCODED_FACE_SPIN_COUNT - 1;
    }

    private static int resolveOpeningSill(ShipOpening opening) {
        return opening == ShipOpening.WINDOW ? EngineSetting.SHIP_WINDOW_SILL : 1;
    }

    private static void removePlacement(IntArrayList cells, int place) {

        int index = ShipBlueprintStruct.findPlacement(cells, place);

        if (index != EngineSetting.INDEX_NOT_FOUND)
            cells.removeInt(index);
    }

    // A placement turned down with the reason shown, nothing changed
    private boolean refuse(ShipwrightSessionInstance session, String reason) {

        session.setStatus(reason);
        soundCueManager.playCue(refusedCueID);

        return false;
    }

    // True when the deck picked was built with open floor at an octant, so a wall dragged across its edge stops there
    private boolean isOpenFloor(ShipwrightSessionInstance session, int x, int z) {

        ShipLayoutStruct layout = session.getLayout();
        ShipPiece piece = layout.getGrid().get(
                layout.getOriginX() + x, session.getDeck() * EngineSetting.SHIP_DECK_HEIGHT, z);

        return layout.getSectionLayers() > 0 && piece != null && piece.isFloor();
    }

    // True when the deck picked was built with open floor at an octant and nothing standing on it
    private boolean isClearFloor(ShipwrightSessionInstance session, int x, int z) {

        ShipLayoutStruct layout = session.getLayout();

        return layout.getSectionLayers() > 0 && shipwrightHullBranch.isOpenFloor(layout.getGrid(),
                layout.getOriginX() + x, session.getDeck() * EngineSetting.SHIP_DECK_HEIGHT, z, 1, 1);
    }

    // The helm, the capstan and every furnishing table the drafting table offers
    private int resolveItemCount() {
        return EngineSetting.SHIPWRIGHT_ITEM_FURNISHINGS + shipwrightManager.getFurnishingCount();
    }

    // The furniture item in hand, within the drafting table's list
    private int resolveHeldItem(ShipwrightSessionInstance session) {
        return Math.floorMod(session.getItemIndex(), resolveItemCount());
    }

    // The octant along the sheet and across its full breadth a texel of the sheet lies on, starboard drawn down
    static int toSheetX(int u) {
        return u - EngineSetting.SHIP_AFT_MARGIN;
    }

    static int toSheetZ(int v) {
        return EngineSetting.SHIP_SHEET_BREADTH - 1 - v;
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
        commit(session, before, false, EngineSetting.SHIPWRIGHT_STATUS_NONE, session.getLayout().hasBuildProblem());
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
        commit(session, before, false, EngineSetting.SHIPWRIGHT_STATUS_NONE, session.getLayout().hasBuildProblem());
    }

    // Options \\

    // Turns what is set down next a quarter turn either way
    void turn(ShipwrightSessionInstance session, int step) {
        session.setSpin(Math.floorMod(session.getSpin() + step, EngineSetting.ENCODED_FACE_SPIN_COUNT));
    }

    // Catalogue \\

    // How many options the tab open lists: its fixed entries, then every mast stepped or every furnishing after them
    int getOptionCount(ShipwrightSessionInstance session) {
        return session.getTab().getEntryCount() + resolveFollowingCount(session, session.getTab());
    }

    private int resolveFollowingCount(ShipwrightSessionInstance session, ShipwrightTab tab) {

        if (!tab.hasFollowing())
            return 0;

        return switch (tab.getFollowingTool()) {
            case MASTS -> session.getBlueprint().getMasts().size();
            case FURNITURE -> resolveItemCount() - tab.getFollowingOffset();
            default -> 0;
        };
    }

    // The entry of the tab open the option in hand is listed as, INDEX_NOT_FOUND when it is not listed there or no
    // mast is picked
    int getOptionIndex(ShipwrightSessionInstance session) {

        ShipwrightTab tab = session.getTab();
        ShipwrightTool tool = session.getTool();
        int option = getHeldOption(session);

        if (option == EngineSetting.INDEX_NOT_FOUND)
            return option;

        int entry = tab.findEntry(tool, option);

        if (entry != EngineSetting.INDEX_NOT_FOUND || !tab.follows(tool, option))
            return entry;

        return tab.getEntryCount() + option - tab.getFollowingOffset();
    }

    // The option of the tool in hand held, INDEX_NOT_FOUND for no mast picked; the deck and the waist have one
    int getHeldOption(ShipwrightSessionInstance session) {
        return switch (session.getTool()) {
            case WALLS -> session.getWall().ordinal();
            case OPENINGS -> session.getOpening().ordinal();
            case STAIRS -> session.getClimb().ordinal();
            case FURNITURE -> resolveHeldItem(session);
            case MASTS -> session.hasMastPicked() ? session.getMastIndex() : EngineSetting.INDEX_NOT_FOUND;
            default -> 0;
        };
    }

    // The tool an entry of the tab open works
    ShipwrightTool getEntryTool(ShipwrightSessionInstance session, int entry) {

        ShipwrightTab tab = session.getTab();

        return entry < tab.getEntryCount() ? tab.getEntryTool(entry) : tab.getFollowingTool();
    }

    // The option of its tool an entry of the tab open stands for
    int getEntryOption(ShipwrightSessionInstance session, int entry) {

        ShipwrightTab tab = session.getTab();

        return entry < tab.getEntryCount()
                ? tab.getEntryOption(entry)
                : tab.getFollowingOffset() + entry - tab.getEntryCount();
    }

    // Takes the option an entry of the tab open stands for into the hand
    void pickOption(ShipwrightSessionInstance session, int entry) {

        if (entry < 0 || entry >= getOptionCount(session))
            return;

        hold(session, getEntryTool(session, entry), getEntryOption(session, entry));
    }

    // Takes a tool's option into the hand, opening the tab it is listed in and keeping it as the one that tab holds
    private void hold(ShipwrightSessionInstance session, ShipwrightTool tool, int option) {

        ShipwrightTab tab = ShipwrightTab.of(tool, option);

        session.setTool(tab, tool);

        switch (tool) {
            case WALLS -> session.setWall(ShipWall.VALUES[option]);
            case OPENINGS -> session.setOpening(ShipOpening.VALUES[option]);
            case STAIRS -> session.setClimb(ShipClimb.VALUES[option]);
            case FURNITURE -> session.setItemIndex(option);
            case MASTS -> session.setMastIndex(option);
            default -> {
            }
        }

        session.setTabEntry(tab, getOptionIndex(session));
    }

    // Opens a tab, taking up again the option it last held, or its first, or for the masts a new mast
    void selectTab(ShipwrightSessionInstance session, ShipwrightTab tab) {

        session.setTab(tab);

        int entry = session.getTabEntry(tab);

        if (entry >= 0 && entry < getOptionCount(session)) {
            pickOption(session, entry);
            return;
        }

        if (tab.getFirstTool() == ShipwrightTool.MASTS) {
            hold(session, ShipwrightTool.MASTS, EngineSetting.INDEX_NOT_FOUND);
            return;
        }

        pickOption(session, 0);
    }

    // Steps the option in hand through the tab open, round either end
    void stepOption(ShipwrightSessionInstance session, int step) {

        int count = getOptionCount(session);

        if (count == 0)
            return;

        int index = getOptionIndex(session);

        pickOption(session, index == EngineSetting.INDEX_NOT_FOUND
                ? (step > 0 ? 0 : count - 1)
                : Math.floorMod(index + step, count));
    }

    // Naming \\

    // Names the ship what was typed, as one change, when it holds a letter or digit
    void confirmName(ShipwrightSessionInstance session, String displayName) {

        session.endNaming();

        if (displayName.isEmpty() || displayName.equals(session.getBlueprint().getDisplayName()))
            return;

        ShipBlueprintStruct before = session.getBlueprint().copy();

        session.getBlueprint().setDisplayName(displayName);
        commit(session, before, false, EngineSetting.SHIPWRIGHT_STATUS_NONE, session.getLayout().hasBuildProblem());
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

    // Keeps a change once compiled, showing what was done or the problem it left, true when it was kept; a strict
    // change that leaves something built with no room to stand when nothing was before it is taken back with the
    // reason
    private boolean commit(
            ShipwrightSessionInstance session,
            ShipBlueprintStruct before,
            boolean strict,
            String done,
            boolean hadBuildProblem) {

        session.pushUndo(before);
        shipwrightManager.compileSession(session);

        if (strict && !hadBuildProblem && session.getLayout().hasBuildProblem()) {

            String refusal = session.getLayout().getBuildProblem();

            session.discardLast();
            shipwrightManager.compileSession(session);
            session.setStatus(refusal);
            soundCueManager.playCue(refusedCueID);
            return false;
        }

        session.markDirty();
        session.setStatus(session.getLayout().hasProblem() ? session.getLayout().getProblem() : done);
        soundCueManager.playCue(placeCueID);

        return true;
    }

    // Utility \\

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
