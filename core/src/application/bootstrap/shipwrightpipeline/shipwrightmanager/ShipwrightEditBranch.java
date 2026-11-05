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
     * cut through the open floor it passes over, mirrored too; and walls and
     * half walls are painted on the open floor it passes over, railings on
     * open floor or in place of the ship's side or bulwark at the deck's edge
     * above the waterline, erase taking any of them down and so putting the
     * side or bulwark back. Every other tool works the
     * octant the stroke began on once, the same way for everything: erase
     * takes away whatever stands there, found over its whole footprint;
     * draw over something already set down picks it up into the hand — the
     * tool, the option and the quarter turn that set it down — so the next
     * draw sets it down again wherever it is wanted; draw anywhere else sets
     * the thing in hand down, or refuses with the reason when it cannot stand
     * there. Why the thing in hand could not stand at an octant is one rule
     * per kind, asked for the ghost under the cursor and again before
     * anything is set down, and enforced once more by the compile. Only the
     * outline and the waist are mirrored. A mast is only stepped or taken
     * down; its height, sails and rigging are worked out from the hull. A
     * door or a window is set on the wall or side it is cut through and turns
     * itself to face across it, a boarding ladder faces outboard, and taking
     * a wall down takes every door and window in it along. Every tool's
     * options form its catalogue, picked by index or stepped through. Every
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

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.pickScratch = new ShipPickStruct();
        this.footprintScratch = new ShipPickStruct();
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

        session.beginStroke(erasing);
        session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_NONE);
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
            commit(session, before, false, session.getStatus());
            return;
        }

        commit(session, before, true, session.getStatus());
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
            default -> pickOrPlace(session, x, z, erasing);
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

    // Cuts the waist through the deck picked at an octant and its mirror, or fills both back in; only open floor with
    // open room below it is cut, so a stroke dragged across the deck stops at whatever stands on it
    private boolean paintWaist(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int deck = session.getDeck();

        if (erasing) {

            if (!blueprint.hasWaist(deck, x, z))
                return false;

            blueprint.setWaist(deck, x, z, false);

            return true;
        }

        if (deck == 0) {

            if (session.isStrokeStart())
                refuse(session, EngineSetting.SHIPWRIGHT_PROBLEM_WAIST_BOTTOM);

            return false;
        }

        int mirrorZ = EngineSetting.SHIP_SHEET_BREADTH - 1 - z;

        if (blueprint.hasWaist(deck, x, z) || !isCuttableFloor(session, x, z)
                || !isCuttableFloor(session, x, mirrorZ))
            return false;

        blueprint.setWaist(deck, x, z, true);

        return true;
    }

    // True when the deck picked is drawn and was built with open floor at an octant, nothing standing on it but the
    // rail round the waist already cut beside it, and open room below
    private boolean isCuttableFloor(ShipwrightSessionInstance session, int x, int z) {

        ShipLayoutStruct layout = session.getLayout();
        int gridX = layout.getOriginX() + x;
        int floor = session.getDeck() * EngineSetting.SHIP_DECK_HEIGHT;
        ShipPiece standing = layout.getGrid().get(gridX, floor + 1, z);

        return session.getBlueprint().isOnDeck(session.getDeck(), x, z) && isOpenFloor(session, x, z)
                && (standing == null || standing == ShipPiece.RAILS)
                && layout.getGrid().isEmpty(gridX, floor - 1, z);
    }

    // Paints the wall picked on open floor, a half wall or a railing also in place of the side or a bulwark at the deck's edge;
    // erase takes a wall down with its doors and windows, putting back whatever it stood in place of
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

        if (wall == ShipWall.RAILING) {

            String problem = shipwrightHullBranch.findRailingProblem(session.getLayout(), deck, x, z);

            if (!problem.isEmpty()) {

                if (session.isStrokeStart())
                    refuse(session, problem);

                return false;
            }
        } else if (!isOpenFloor(session, x, z) && (wall != ShipWall.HALF
                || !shipwrightHullBranch.findRailingProblem(session.getLayout(), deck, x, z).isEmpty()))
            return false;

        blueprint.setWall(deck, x, z, wall);

        return true;
    }

    // Pick \\

    // Erase takes away whatever stands at an octant; draw picks it up into the hand, or with nothing there sets the
    // thing in hand down
    private boolean pickOrPlace(ShipwrightSessionInstance session, int x, int z, boolean erasing) {

        ShipPickStruct pick = pickScratch;

        shipwrightInspectBranch.pick(session, x, z, pick);

        if (pick.isEmpty())
            return !erasing && place(session, x, z);

        takeAway(session, pick);

        if (erasing) {
            session.setStatus(EngineSetting.SHIPWRIGHT_STATUS_REMOVED
                    + shipwrightManager.getOptionName(session, pick.getTool(), pick.getOption()));
            return true;
        }

        session.setTool(pick.getTool());
        session.setSpin(ShipCellUtility.unpackSpin(pick.getCell()));

        if (pick.getTool() != ShipwrightTool.MASTS)
            pickOption(session, pick.getOption());

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
        int item = Math.floorMod(session.getItemIndex(), resolveItemCount());

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

    // A hatch where a hatch may lie, a grate over an opening; a door or window only on a wall or the side
    private String findOpeningProblem(ShipwrightSessionInstance session, int x, int z, int cell) {

        if (session.getOpening() == ShipOpening.HATCH)
            return shipwrightHullBranch.findHatchProblem(session.getLayout(), cell);

        if (session.getOpening() == ShipOpening.GRATE)
            return shipwrightHullBranch.findGrateProblem(session.getLayout(), cell);

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

    // An item's whole footprint on open floor with nothing else set down over any of it
    private String findItemProblem(ShipwrightSessionInstance session, int cell) {

        ShipPickStruct footprint = footprintScratch;
        ShipPickStruct pick = pickScratch;
        int item = Math.floorMod(session.getItemIndex(), resolveItemCount());

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
    // its wall, a boarding ladder outboard, a mast's foot centred on the octant along the length
    int resolvePlacementCell(ShipwrightSessionInstance session, int x, int z) {

        int deck = session.getDeck();
        int spin = session.getSpin();

        return switch (session.getTool()) {
            case OPENINGS -> session.getOpening().isFloor()
                    ? ShipCellUtility.pack(deck, x, z, spin)
                    : ShipCellUtility.pack(deck, x, z, shipwrightFittingBranch.resolveOpeningSpin(
                            session.getLayout(), deck, x, z, resolveOpeningSill(session.getOpening()), spin));
            case STAIRS -> ShipCellUtility.pack(deck, x, z, session.getClimb() == ShipClimb.BOARDING
                    ? resolveOutboardSpin(z) : spin);
            case MASTS -> ShipCellUtility.pack(0, Math.max(0, x - EngineSetting.SHIP_MAST_OCTANTS / 2),
                    EngineSetting.SHIP_SHEET_HALF_BREADTH, 0);
            default -> ShipCellUtility.pack(deck, x, z, spin);
        };
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
        commit(session, before, false, EngineSetting.SHIPWRIGHT_STATUS_NONE);
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
        commit(session, before, false, EngineSetting.SHIPWRIGHT_STATUS_NONE);
    }

    // Options \\

    // Turns what is set down next a quarter turn either way
    void turn(ShipwrightSessionInstance session, int step) {
        session.setSpin(Math.floorMod(session.getSpin() + step, EngineSetting.ENCODED_FACE_SPIN_COUNT));
    }

    // Catalogue \\

    // How many options the tool in hand offers: walls, openings, climbs, furniture items or masts; none for the deck
    // and the waist
    int getOptionCount(ShipwrightSessionInstance session) {
        return switch (session.getTool()) {
            case WALLS -> ShipWall.VALUES.length;
            case OPENINGS -> ShipOpening.VALUES.length;
            case STAIRS -> ShipClimb.VALUES.length;
            case FURNITURE -> resolveItemCount();
            case MASTS -> session.getBlueprint().getMasts().size();
            default -> 0;
        };
    }

    // The option of the tool in hand picked, INDEX_NOT_FOUND when none is
    int getOptionIndex(ShipwrightSessionInstance session) {
        return switch (session.getTool()) {
            case WALLS -> session.getWall().ordinal();
            case OPENINGS -> session.getOpening().ordinal();
            case STAIRS -> session.getClimb().ordinal();
            case FURNITURE -> Math.floorMod(session.getItemIndex(), resolveItemCount());
            case MASTS -> session.hasMastPicked() ? session.getMastIndex() : EngineSetting.INDEX_NOT_FOUND;
            default -> EngineSetting.INDEX_NOT_FOUND;
        };
    }

    // Picks an option of the tool in hand by its index in the tool's catalogue
    void pickOption(ShipwrightSessionInstance session, int index) {

        if (index < 0 || index >= getOptionCount(session))
            return;

        switch (session.getTool()) {
            case WALLS -> session.setWall(ShipWall.VALUES[index]);
            case OPENINGS -> session.setOpening(ShipOpening.VALUES[index]);
            case STAIRS -> session.setClimb(ShipClimb.VALUES[index]);
            case FURNITURE -> session.setItemIndex(index);
            case MASTS -> session.setMastIndex(index);
            default -> {
            }
        }
    }

    // Steps the option of the tool in hand through its catalogue, round either end; the deck and the waist step the
    // deck
    void stepOption(ShipwrightSessionInstance session, int step) {

        if (session.getTool() == ShipwrightTool.DECK || session.getTool() == ShipwrightTool.WAIST) {
            stepDeck(session, step);
            return;
        }

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
        commit(session, before, false, EngineSetting.SHIPWRIGHT_STATUS_NONE);
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

    // Keeps a change once compiled, showing what was done or the problem it left; a strict change that leaves something
    // built with no room to stand when nothing was is taken back with the reason
    private void commit(ShipwrightSessionInstance session, ShipBlueprintStruct before, boolean strict, String done) {

        boolean hadBuildProblem = session.getLayout().hasBuildProblem();

        session.pushUndo(before);
        shipwrightManager.compileSession(session);

        if (strict && !hadBuildProblem && session.getLayout().hasBuildProblem()) {

            String refusal = session.getLayout().getBuildProblem();

            session.discardLast();
            shipwrightManager.compileSession(session);
            session.setStatus(refusal);
            soundCueManager.playCue(refusedCueID);
            return;
        }

        session.markDirty();
        session.setStatus(session.getLayout().hasProblem() ? session.getLayout().getProblem() : done);
        soundCueManager.playCue(placeCueID);
    }

    // Utility \\

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
