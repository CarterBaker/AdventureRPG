package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import java.util.Arrays;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipClimb;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOpening;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPickStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightTool;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartRole;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ShipwrightDraftBranch extends BranchPackage {

    /*
     * Draws a drafting table's sheet and its top and side views into the
     * session's own pixels, SHIPWRIGHT_INK_SCALE texels a side to every
     * octant on the sheet, the paper and its ruling left to the sheet's
     * shader. The sheet is always the plan of the deck picked, the sea never
     * drawn on it. While a deck is drawn its outline is inked as floor inside
     * planking, the decks under and over it faint; with any other tool it is
     * the deck as it was built — floor, planking, bulwarks, rails, lips, gun
     * platforms and the wells and bays left open in it; either way the waist
     * cut through it and every wall, half wall, railing and lip, and over it
     * everything set down drawn whole as what it is: a stair's run in treads
     * climbing darker, a ladder's rails and rungs, a hatch's, grate's or glass
     * floor's grating with its hinge, a window's panes, a door, a gun
     * platform, a cannon, the helm, the capstan and every piece of furniture
     * over its footprint, its front edge marked the way it faces. In the
     * sheet's aft margin a gauge shows where the water sits in the deck
     * picked: full when the whole deck lies under it, empty when the deck
     * stands clear of it, and filled as far up the deck as the waterline
     * crosses it. Whatever stands under the cursor is outlined; with
     * nothing there, the thing in hand is drawn whole at the cursor as it
     * would be set down, red where it could not stand. Every mast is marked,
     * the picked one brighter. Plans run bow to the right and starboard down. The
     * views are every part's boxes seen from starboard and from above,
     * nearest drawn over farthest, shaded by depth and coloured by what they
     * are made of, with the cargo in its places and the sea below the
     * waterline the ship floats at; the side view lays every sail out fore and
     * aft behind the spars as a sail plan does; both are drawn at one scale,
     * as fine as fits.
     */

    // Internal
    private ShipwrightManager shipwrightManager;
    private ShipwrightFittingBranch shipwrightFittingBranch;
    private ShipwrightInspectBranch shipwrightInspectBranch;
    private ShipwrightEditBranch shipwrightEditBranch;

    // Scratch — main thread only
    private int[] depthScratch;
    private ShipPickStruct footprint;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.depthScratch = new int[Math.max(
                EngineSetting.SHIPWRIGHT_SIDE_TEXELS_X * EngineSetting.SHIPWRIGHT_SIDE_TEXELS_Y,
                EngineSetting.SHIPWRIGHT_TOP_TEXELS_X * EngineSetting.SHIPWRIGHT_TOP_TEXELS_Y)];
        this.footprint = new ShipPickStruct();
    }

    @Override
    protected void get() {
        this.shipwrightManager = get(ShipwrightManager.class);
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
        this.shipwrightInspectBranch = get(ShipwrightInspectBranch.class);
        this.shipwrightEditBranch = get(ShipwrightEditBranch.class);
    }

    // Sheet \\

    // The octants the sheet spans across and up, written to the session
    void resolveSheetSize(ShipwrightSessionInstance session) {
        session.setSheetSize(EngineSetting.SHIP_AFT_MARGIN + EngineSetting.SHIP_SHEET_LENGTH,
                EngineSetting.SHIP_SHEET_BREADTH);
    }

    void drawSheet(ShipwrightSessionInstance session) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;

        resolveSheetSize(session);
        Arrays.fill(session.getSheetPixels(), 0, session.getSheetWidth() * scale * session.getSheetHeight() * scale
                * EngineSetting.COLOR_CHANNEL_COUNT, (byte) 0);

        if (session.getTool() == ShipwrightTool.DECK)
            drawOutlines(session);
        else
            drawDeck(session);

        drawStanding(session);
        drawMasts(session);
        drawGauge(session);

        if (!session.getTool().isPainted())
            drawHand(session);
    }

    // The deck picked inked as floor inside its planking, the decks under and over it faint
    private void drawOutlines(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int deck = session.getDeck();

        for (int z = 0; z < EngineSetting.SHIP_SHEET_BREADTH; z++)
            for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++) {

                int color;

                if (blueprint.isOnDeck(deck, x, z))
                    color = isOutlineRim(blueprint, deck, x, z)
                            ? EngineSetting.SHIPWRIGHT_INK_SHELL
                            : EngineSetting.SHIPWRIGHT_INK_FLOOR;
                else if (blueprint.isOnDeck(deck - 1, x, z))
                    color = EngineSetting.SHIPWRIGHT_INK_BELOW;
                else if (blueprint.isOnDeck(deck + 1, x, z))
                    color = EngineSetting.SHIPWRIGHT_INK_ABOVE;
                else
                    continue;

                fillOctant(session, EngineSetting.SHIP_AFT_MARGIN + x, z, color);
            }
    }

    // True when an octant of a deck's outline lies at its edge, some octant beside it off the deck
    private boolean isOutlineRim(ShipBlueprintStruct blueprint, int deck, int x, int z) {

        for (int spin = 0; spin < EngineSetting.ENCODED_FACE_SPIN_COUNT; spin++)
            if (!blueprint.isOnDeck(deck, x + ShipCellUtility.stepX(spin), z + ShipCellUtility.stepZ(spin)))
                return true;

        return false;
    }

    // The deck picked as it was built
    private void drawDeck(ShipwrightSessionInstance session) {

        ShipLayoutStruct layout = session.getLayout();

        if (layout.getSectionLayers() > 0)
            drawFloor(session, layout, session.getDeck() * EngineSetting.SHIP_DECK_HEIGHT);
    }

    // Everything set down on the deck picked drawn as what it is
    private void drawStanding(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipLayoutStruct layout = session.getLayout();
        int deck = session.getDeck();

        drawCells(session, blueprint.getWaists(), deck, EngineSetting.SHIPWRIGHT_INK_WAIST);
        drawCells(session, blueprint.getWalls(), deck, EngineSetting.SHIPWRIGHT_INK_WALL);
        drawCells(session, blueprint.getHalfWalls(), deck, EngineSetting.SHIPWRIGHT_INK_HALF_WALL);
        drawCells(session, blueprint.getRailings(), deck, EngineSetting.SHIPWRIGHT_INK_RAILING);
        drawCells(session, blueprint.getLips(), deck, EngineSetting.SHIPWRIGHT_INK_LIP);
        drawHatches(session, layout.getHatchBoxes(), layout, deck * EngineSetting.SHIP_DECK_HEIGHT,
                EngineSetting.SHIPWRIGHT_INK_HATCH);
        drawHatches(session, layout.getGrateBoxes(), layout, deck * EngineSetting.SHIP_DECK_HEIGHT,
                EngineSetting.SHIPWRIGHT_INK_GRATE);
        drawHatches(session, layout.getGlassFloorBoxes(), layout, deck * EngineSetting.SHIP_DECK_HEIGHT,
                EngineSetting.SHIPWRIGHT_INK_GLASS_FLOOR);
        drawPlacements(session, ShipwrightTool.OPENINGS, ShipOpening.PLATFORM.ordinal(), blueprint.getGunPlatforms(),
                deck);
        drawPlacements(session, ShipwrightTool.STAIRS, ShipClimb.STAIR.ordinal(), blueprint.getStairs(), deck);
        drawPlacements(session, ShipwrightTool.STAIRS, ShipClimb.LADDER.ordinal(), blueprint.getLadders(), deck);
        drawPlacements(session, ShipwrightTool.STAIRS, ShipClimb.BOARDING.ordinal(), blueprint.getBoardingLadders(),
                deck);
        drawPlacements(session, ShipwrightTool.OPENINGS, ShipOpening.WINDOW.ordinal(), blueprint.getWindows(), deck);
        drawPlacements(session, ShipwrightTool.OPENINGS, ShipOpening.DOOR.ordinal(), blueprint.getDoors(), deck);
        drawFurniture(session, blueprint, deck);
        drawItem(session, EngineSetting.SHIPWRIGHT_ITEM_HELM, blueprint.getHelm(), deck);
        drawItem(session, EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN, blueprint.getCapstan(), deck);
    }

    private void drawFloor(ShipwrightSessionInstance session, ShipLayoutStruct layout, int floor) {

        ShipOctantGridStruct grid = layout.getGrid();

        for (int z = 0; z < grid.getSizeZ(); z++)
            for (int x = 0; x < grid.getSizeX(); x++) {

                ShipPiece piece = grid.get(x, floor, z);
                ShipPiece standing = grid.get(x, floor + 1, z);
                int color;

                if (standing == ShipPiece.STAIRS)
                    color = EngineSetting.SHIPWRIGHT_INK_STAIRS;
                else if (standing == ShipPiece.BULWARK || standing == ShipPiece.RAILS)
                    color = EngineSetting.SHIPWRIGHT_INK_RAIL;
                else if (standing == ShipPiece.WALLS)
                    color = EngineSetting.SHIPWRIGHT_INK_WALL;
                else if (standing == ShipPiece.LIPS)
                    color = EngineSetting.SHIPWRIGHT_INK_LIP;
                else if (standing == ShipPiece.PLATFORMS)
                    color = EngineSetting.SHIPWRIGHT_INK_PLATFORM;
                else if (standing != null)
                    color = EngineSetting.SHIPWRIGHT_INK_SHELL;
                else if (piece != null && piece.isFloor())
                    color = EngineSetting.SHIPWRIGHT_INK_FLOOR;
                else if (piece != null)
                    color = EngineSetting.SHIPWRIGHT_INK_SHELL;
                else if (layout.isVolume(x - layout.getOriginX(), floor, z))
                    color = EngineSetting.SHIPWRIGHT_INK_WELL;
                else
                    continue;

                fillOctant(session, x, z, color);
            }
    }

    private void drawCells(ShipwrightSessionInstance session, IntArrayList cells, int deck, int color) {

        for (int i = 0; i < cells.size(); i++) {

            int cell = cells.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) == deck)
                fillOctant(session, EngineSetting.SHIP_AFT_MARGIN + ShipCellUtility.unpackX(cell),
                        ShipCellUtility.unpackZ(cell), color);
        }
    }

    // Every hatch lid or grate lying in a floor, where it was built: a grating, its hinge drawn heavy
    private void drawHatches(
            ShipwrightSessionInstance session,
            IntArrayList boxes,
            ShipLayoutStruct layout,
            int floor,
            int color) {

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++) {

            if (ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Y) != floor)
                continue;

            footprint.setFootprint(
                    ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_X) - layout.getOriginX(),
                    ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Z),
                    ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_X) - layout.getOriginX(),
                    ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Z));
            drawGrating(session, footprint, shipwrightFittingBranch.resolveHingeSpin(boxes, i), color);
        }
    }

    // Every placement of one tool's option on a deck, each over its whole footprint
    private void drawPlacements(ShipwrightSessionInstance session, ShipwrightTool tool, int option,
            IntArrayList cells, int deck) {

        for (int i = 0; i < cells.size(); i++) {

            int cell = cells.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) != deck)
                continue;

            shipwrightInspectBranch.resolveFootprint(session, tool, option, cell, footprint);
            drawGlyph(session, tool, option, ShipCellUtility.unpackSpin(cell), footprint, false);
        }
    }

    private void drawFurniture(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int deck) {

        ObjectArrayList<ShipFurnitureStruct> furniture = blueprint.getFurniture();

        for (int i = 0; i < furniture.size(); i++) {

            ShipFurnitureStruct piece = furniture.get(i);

            if (ShipCellUtility.unpackLevel(piece.getCell()) == deck)
                drawItem(session, shipwrightManager.findItemIndex(piece.getFurnishingName()), piece.getCell(), deck);
        }
    }

    private void drawItem(ShipwrightSessionInstance session, int item, int cell, int deck) {

        if (cell == EngineSetting.SHIP_CELL_NONE || ShipCellUtility.unpackLevel(cell) != deck)
            return;

        shipwrightInspectBranch.resolveFootprint(session, ShipwrightTool.FURNITURE, item, cell, footprint);
        drawGlyph(session, ShipwrightTool.FURNITURE, item, ShipCellUtility.unpackSpin(cell), footprint, false);
    }

    // Every mast on the centre line, the one picked brighter while masts are being stepped
    private void drawMasts(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        boolean picking = session.getTool() == ShipwrightTool.MASTS;

        for (int i = 0; i < blueprint.getMasts().size(); i++) {

            ShipMastStruct mast = blueprint.getMasts().get(i);

            footprint.setFootprint(mast.getX(), EngineSetting.SHIP_SHEET_HALF_BREADTH - 1,
                    mast.getX() + EngineSetting.SHIP_MAST_OCTANTS, EngineSetting.SHIP_SHEET_HALF_BREADTH + 1);
            fillFootprint(session, footprint, picking && i == session.getMastIndex()
                    ? EngineSetting.SHIPWRIGHT_INK_PICKED_MAST
                    : EngineSetting.SHIPWRIGHT_INK_MAST);
        }
    }

    // Gauge \\

    // The water gauge in the sheet's aft margin, the deck picked from its floor at the bottom to the floor above at the
    // top, filled as far up as the waterline the ship floats at crosses it
    private void drawGauge(ShipwrightSessionInstance session) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;
        ShipLayoutStruct layout = session.getLayout();
        int minX = EngineSetting.SHIPWRIGHT_GAUGE_X * scale;
        int maxX = minX + EngineSetting.SHIPWRIGHT_GAUGE_WIDTH * scale;
        int minZ = EngineSetting.SHIPWRIGHT_GAUGE_MARGIN * scale;
        int maxZ = (EngineSetting.SHIP_SHEET_BREADTH - EngineSetting.SHIPWRIGHT_GAUGE_MARGIN) * scale;
        float deckBottom = session.getDeck() * EngineSetting.SHIP_DECK_HEIGHT * EngineSetting.SUB_BLOCK_SIZE;
        float deckHeight = EngineSetting.SHIP_DECK_HEIGHT * EngineSetting.SUB_BLOCK_SIZE;
        float share = layout.isAfloat()
                ? Math.max(0f, Math.min(1f, (layout.getDraft() - deckBottom) / deckHeight))
                : 0f;
        int waterZ = minZ + 1 + Math.round((maxZ - minZ - 2) * share);

        fillPlan(session, minX, minZ, maxX, maxZ, EngineSetting.SHIPWRIGHT_INK_GAUGE_FRAME);
        fillPlan(session, minX + 1, minZ + 1, maxX - 1, maxZ - 1, EngineSetting.SHIPWRIGHT_INK_GAUGE_EMPTY);
        fillPlan(session, minX + 1, minZ + 1, maxX - 1, waterZ, EngineSetting.SHIPWRIGHT_INK_GAUGE_WATER);
    }

    // Hand \\

    // Whatever stands under the cursor outlined, to be picked up or taken away; with nothing there, the thing in
    // hand drawn whole at the cursor as it would be set down, red where it could not stand
    private void drawHand(ShipwrightSessionInstance session) {

        ShipPickStruct hover = session.getHover();

        if (!hover.isEmpty()) {
            outlineFootprint(session, hover, EngineSetting.SHIPWRIGHT_INK_HIGHLIGHT);
            return;
        }

        int x = ShipwrightEditBranch.toSheetX(session.getCursorU());
        int z = ShipwrightEditBranch.toSheetZ(session.getCursorV());
        int option = shipwrightEditBranch.getHeldOption(session);

        if (x < 0 || x >= EngineSetting.SHIP_SHEET_LENGTH || z < 0 || z >= EngineSetting.SHIP_SHEET_BREADTH
                || option == EngineSetting.INDEX_NOT_FOUND && session.getTool() != ShipwrightTool.MASTS)
            return;

        int cell = shipwrightEditBranch.resolvePlacementCell(session, x, z);

        shipwrightInspectBranch.resolveFootprint(session, session.getTool(), option, cell, footprint);
        drawGlyph(session, session.getTool(), option, ShipCellUtility.unpackSpin(cell), footprint, true);
    }

    // Glyphs \\

    // One thing set down drawn over its footprint as what it is — a stair its treads climbing darker, a ladder its
    // rails and rungs, a window its panes, anything that faces a way its front edge — or, as the ghost of the thing
    // in hand, in the ghost's ink with its front edge and treads
    private void drawGlyph(
            ShipwrightSessionInstance session,
            ShipwrightTool tool,
            int option,
            int spin,
            ShipPickStruct footprint,
            boolean ghost) {

        int ghostColor = session.getPlacementProblem().isEmpty()
                ? EngineSetting.SHIPWRIGHT_INK_GHOST
                : EngineSetting.SHIPWRIGHT_INK_GHOST_BLOCKED;

        if (tool == ShipwrightTool.STAIRS && option == ShipClimb.STAIR.ordinal()) {
            drawStair(session, footprint, spin, ghost, ghostColor);
            return;
        }

        if (tool == ShipwrightTool.STAIRS) {
            drawLadder(session, footprint, spin, ghost ? ghostColor : option == ShipClimb.BOARDING.ordinal()
                    ? EngineSetting.SHIPWRIGHT_INK_BOARDING : EngineSetting.SHIPWRIGHT_INK_LADDER);
            return;
        }

        if (tool == ShipwrightTool.OPENINGS && ShipOpening.VALUES[option].isFloor()) {
            drawGrating(session, footprint, spin, ghost ? ghostColor : resolveFloorInk(ShipOpening.VALUES[option]));
            return;
        }

        if (tool == ShipwrightTool.OPENINGS && option == ShipOpening.WINDOW.ordinal()) {
            fillFootprint(session, footprint, ghost ? ghostColor : EngineSetting.SHIPWRIGHT_INK_WINDOW);
            crossFootprint(session, footprint, EngineSetting.SHIPWRIGHT_INK_PANE);
            return;
        }

        fillFootprint(session, footprint, ghost ? ghostColor : resolveItemInk(tool, option));
        outlineFootprint(session, footprint, EngineSetting.SHIPWRIGHT_INK_OUTLINE);

        if (tool != ShipwrightTool.MASTS)
            edgeFootprint(session, footprint, spin, EngineSetting.SHIPWRIGHT_INK_FACING);
    }

    private static int resolveFloorInk(ShipOpening opening) {
        return switch (opening) {
            case GRATE -> EngineSetting.SHIPWRIGHT_INK_GRATE;
            case GLASS -> EngineSetting.SHIPWRIGHT_INK_GLASS_FLOOR;
            default -> EngineSetting.SHIPWRIGHT_INK_HATCH;
        };
    }

    private int resolveItemInk(ShipwrightTool tool, int option) {

        if (tool == ShipwrightTool.OPENINGS)
            return option == ShipOpening.PLATFORM.ordinal()
                    ? EngineSetting.SHIPWRIGHT_INK_PLATFORM
                    : EngineSetting.SHIPWRIGHT_INK_DOOR;

        if (tool == ShipwrightTool.MASTS)
            return EngineSetting.SHIPWRIGHT_INK_MAST;

        if (option == EngineSetting.SHIPWRIGHT_ITEM_HELM || option == EngineSetting.SHIPWRIGHT_ITEM_CAPSTAN)
            return EngineSetting.SHIPWRIGHT_INK_FITTING;

        if (option == shipwrightManager.findItemIndex(EngineSetting.SHIP_CANNON_FURNISHING))
            return EngineSetting.SHIPWRIGHT_INK_CANNON;

        return EngineSetting.SHIPWRIGHT_INK_FURNITURE;
    }

    // A stair's run, every step a shade darker as it climbs and a tread line at its foot end, its head edge marked
    private void drawStair(ShipwrightSessionInstance session, ShipPickStruct footprint, int spin, boolean ghost,
            int ghostColor) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);
        int run = Math.max(1, EngineSetting.SHIP_DECK_HEIGHT - 1);

        for (int z = footprint.getMinZ(); z < footprint.getMaxZ(); z++)
            for (int x = footprint.getMinX(); x < footprint.getMaxX(); x++) {

                int step = stepX > 0 ? x - footprint.getMinX()
                        : stepX < 0 ? footprint.getMaxX() - 1 - x
                                : stepZ > 0 ? z - footprint.getMinZ() : footprint.getMaxZ() - 1 - z;
                int color = ghost ? ghostColor : blendInk(EngineSetting.SHIPWRIGHT_INK_STAIR_LOW,
                        EngineSetting.SHIPWRIGHT_INK_STAIR_HIGH, (float) step / run);
                int texelX = (EngineSetting.SHIP_AFT_MARGIN + x) * scale;
                int texelZ = z * scale;

                fillPlan(session, texelX, texelZ, texelX + scale, texelZ + scale, color);

                int treadX = stepX > 0 ? texelX : stepX < 0 ? texelX + scale - 1 : texelX;
                int treadZ = stepZ > 0 ? texelZ : stepZ < 0 ? texelZ + scale - 1 : texelZ;

                fillPlan(session, treadX, treadZ, stepX != 0 ? treadX + 1 : texelX + scale,
                        stepZ != 0 ? treadZ + 1 : texelZ + scale, EngineSetting.SHIPWRIGHT_INK_TREAD);
            }

        edgeFootprint(session, footprint, spin, EngineSetting.SHIPWRIGHT_INK_FACING);
    }

    // A ladder's two rails along the way it climbs, and a rung across them every other texel
    private void drawLadder(ShipwrightSessionInstance session, ShipPickStruct footprint, int spin, int color) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;
        int minX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMinX()) * scale;
        int minZ = footprint.getMinZ() * scale;
        int maxX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMaxX()) * scale;
        int maxZ = footprint.getMaxZ() * scale;
        boolean alongX = ShipCellUtility.stepX(spin) != 0;
        int rung = EngineSetting.SHIPWRIGHT_INK_RUNG;

        fillPlan(session, minX, minZ, maxX, maxZ, color);

        if (alongX) {

            fillPlan(session, minX, minZ, maxX, minZ + 1, rung);
            fillPlan(session, minX, maxZ - 1, maxX, maxZ, rung);

            for (int x = minX + 1; x < maxX; x += 2)
                fillPlan(session, x, minZ, x + 1, maxZ, rung);

            return;
        }

        fillPlan(session, minX, minZ, minX + 1, maxZ, rung);
        fillPlan(session, maxX - 1, minZ, maxX, maxZ, rung);

        for (int z = minZ + 1; z < maxZ; z += 2)
            fillPlan(session, minX, z, maxX, z + 1, rung);
    }

    // A grating of bars every other texel, its hinge edge drawn heavy
    private void drawGrating(ShipwrightSessionInstance session, ShipPickStruct footprint, int hinge, int color) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;
        int minX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMinX()) * scale;
        int minZ = footprint.getMinZ() * scale;
        int maxX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMaxX()) * scale;
        int maxZ = footprint.getMaxZ() * scale;

        fillPlan(session, minX, minZ, maxX, maxZ, color);

        for (int x = minX; x < maxX; x += 2)
            fillPlan(session, x, minZ, x + 1, maxZ, EngineSetting.SHIPWRIGHT_INK_GRATING);

        for (int z = minZ; z < maxZ; z += 2)
            fillPlan(session, minX, z, maxX, z + 1, EngineSetting.SHIPWRIGHT_INK_GRATING);

        edgeFootprint(session, footprint, hinge, EngineSetting.SHIPWRIGHT_INK_HINGE);
    }

    // Footprints \\

    private void fillFootprint(ShipwrightSessionInstance session, ShipPickStruct footprint, int color) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;

        fillPlan(session,
                (EngineSetting.SHIP_AFT_MARGIN + footprint.getMinX()) * scale, footprint.getMinZ() * scale,
                (EngineSetting.SHIP_AFT_MARGIN + footprint.getMaxX()) * scale, footprint.getMaxZ() * scale, color);
    }

    // The texels just inside a footprint's rim
    private void outlineFootprint(ShipwrightSessionInstance session, ShipPickStruct footprint, int color) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;
        int minX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMinX()) * scale;
        int minZ = footprint.getMinZ() * scale;
        int maxX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMaxX()) * scale;
        int maxZ = footprint.getMaxZ() * scale;

        fillPlan(session, minX, minZ, maxX, minZ + 1, color);
        fillPlan(session, minX, maxZ - 1, maxX, maxZ, color);
        fillPlan(session, minX, minZ, minX + 1, maxZ, color);
        fillPlan(session, maxX - 1, minZ, maxX, maxZ, color);
    }

    // The rim of a footprint on the side a quarter turn faces, a texel deep
    private void edgeFootprint(ShipwrightSessionInstance session, ShipPickStruct footprint, int spin, int color) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;
        int minX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMinX()) * scale;
        int minZ = footprint.getMinZ() * scale;
        int maxX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMaxX()) * scale;
        int maxZ = footprint.getMaxZ() * scale;
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);

        fillPlan(session,
                stepX > 0 ? maxX - 1 : minX, stepZ > 0 ? maxZ - 1 : minZ,
                stepX < 0 ? minX + 1 : maxX, stepZ < 0 ? minZ + 1 : maxZ, color);
    }

    // A cross through the middle of a footprint, a texel wide
    private void crossFootprint(ShipwrightSessionInstance session, ShipPickStruct footprint, int color) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;
        int minX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMinX()) * scale;
        int minZ = footprint.getMinZ() * scale;
        int maxX = (EngineSetting.SHIP_AFT_MARGIN + footprint.getMaxX()) * scale;
        int maxZ = footprint.getMaxZ() * scale;
        int middleX = (minX + maxX) / 2;
        int middleZ = (minZ + maxZ) / 2;

        fillPlan(session, middleX, minZ, middleX + 1, maxZ, color);
        fillPlan(session, minX, middleZ, maxX, middleZ + 1, color);
    }

    // A colour between two, a share of the way from the first to the second
    private static int blendInk(int from, int to, float share) {

        int blended = 0;

        for (int shift = 0; shift < Integer.SIZE; shift += Byte.SIZE) {

            int a = (from >>> shift) & 0xFF;
            int b = (to >>> shift) & 0xFF;

            blended |= Math.round(a + (b - a) * share) << shift;
        }

        return blended;
    }

    // Views \\

    // Every part and the cargo from starboard and from above, at the finest scale both fit
    void drawViews(ShipwrightSessionInstance session) {

        ShipLayoutStruct layout = session.getLayout();
        ObjectArrayList<VehiclePartStruct> parts = layout.getParts();
        int maxX = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int maxY = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int maxZ = EngineSetting.SHIP_SHEET_BREADTH * EngineSetting.SHIP_OCTANT_SUB_VOXELS;

        for (int i = 0; i < parts.size(); i++) {
            maxX = Math.max(maxX, parts.get(i).getMaxX());
            maxY = Math.max(maxY, parts.get(i).getMaxY());
            maxZ = Math.max(maxZ, parts.get(i).getMaxZ());
        }

        int scale = Math.max(EngineSetting.SHIPWRIGHT_VIEW_SUB_VOXELS, Math.max(
                ceilDivide(maxX, EngineSetting.SHIPWRIGHT_SIDE_TEXELS_X),
                Math.max(ceilDivide(maxY, EngineSetting.SHIPWRIGHT_SIDE_TEXELS_Y),
                        ceilDivide(maxZ, EngineSetting.SHIPWRIGHT_TOP_TEXELS_Y))));
        int width = ceilDivide(maxX, scale);
        int sideHeight = ceilDivide(maxY, scale);
        int topHeight = ceilDivide(maxZ, scale);

        session.setViewSizes(width, sideHeight, width, topHeight);

        drawSide(session, layout, scale, width, sideHeight, maxZ);
        drawTop(session, layout, scale, width, topHeight, maxY, maxZ);
    }

    private void drawSide(ShipwrightSessionInstance session, ShipLayoutStruct layout, int scale, int width,
            int height, int maxZ) {

        byte[] pixels = session.getSidePixels();
        int waterline = layout.isAfloat()
                ? Math.round(layout.getDraft() * EngineSetting.SUB_VOXEL_RESOLUTION / scale) : 0;

        Arrays.fill(pixels, 0, width * height * EngineSetting.COLOR_CHANNEL_COUNT, (byte) 0);
        Arrays.fill(depthScratch, 0, width * height, Integer.MIN_VALUE);

        for (int v = 0; v < Math.min(waterline, height); v++)
            for (int u = 0; u < width; u++)
                writePixel(pixels, width, u, v, EngineSetting.SHIPWRIGHT_COLOR_SEA);

        ObjectArrayList<VehiclePartStruct> parts = layout.getParts();

        for (int i = 0; i < parts.size(); i++)
            if (parts.get(i).getRole() == VehiclePartRole.SAIL)
                drawSailPlan(pixels, width, height, scale, parts.get(i));

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);
            int color = resolveMaterialColor(part.getTextureName());

            for (int box = 0; box < part.getBoxCount(); box++)
                fillBox(pixels, width, height, scale,
                        part.getBoxBound(box, EngineSetting.BOX_MIN_X),
                        part.getBoxBound(box, EngineSetting.BOX_MIN_Y),
                        part.getBoxBound(box, EngineSetting.BOX_MAX_X),
                        part.getBoxBound(box, EngineSetting.BOX_MAX_Y),
                        part.getBoxBound(box, EngineSetting.BOX_MAX_Z),
                        shadeColor(color, part.getBoxBound(box, EngineSetting.BOX_MAX_Z), maxZ));

            drawQuads(pixels, width, height, scale, part, false, maxZ, shadeColor(color, part.getMaxZ(), maxZ));
        }

        for (int slot = 0; slot < layout.getCargoCount(); slot++)
            fillBox(pixels, width, height, scale,
                    layout.getCargoBound(slot, EngineSetting.BOX_MIN_X),
                    layout.getCargoBound(slot, EngineSetting.BOX_MIN_Y),
                    layout.getCargoBound(slot, EngineSetting.BOX_MAX_X),
                    layout.getCargoBound(slot, EngineSetting.BOX_MAX_Y),
                    layout.getCargoBound(slot, EngineSetting.BOX_MAX_Z),
                    shadeColor(EngineSetting.SHIPWRIGHT_COLOR_CARGO,
                            layout.getCargoBound(slot, EngineSetting.BOX_MAX_Z), maxZ));

        if (waterline > 0 && waterline < height)
            for (int u = 0; u < width; u++)
                writePixel(pixels, width, u, waterline, EngineSetting.SHIPWRIGHT_COLOR_WATERLINE);
    }

    private void drawTop(ShipwrightSessionInstance session, ShipLayoutStruct layout, int scale, int width,
            int height, int maxY, int maxZ) {

        byte[] pixels = session.getTopPixels();

        Arrays.fill(pixels, 0, width * height * EngineSetting.COLOR_CHANNEL_COUNT, (byte) 0);
        Arrays.fill(depthScratch, 0, width * height, Integer.MIN_VALUE);

        ObjectArrayList<VehiclePartStruct> parts = layout.getParts();

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);
            int color = resolveMaterialColor(part.getTextureName());

            for (int box = 0; box < part.getBoxCount(); box++)
                fillBox(pixels, width, height, scale,
                        part.getBoxBound(box, EngineSetting.BOX_MIN_X),
                        maxZ - part.getBoxBound(box, EngineSetting.BOX_MAX_Z),
                        part.getBoxBound(box, EngineSetting.BOX_MAX_X),
                        maxZ - part.getBoxBound(box, EngineSetting.BOX_MIN_Z),
                        part.getBoxBound(box, EngineSetting.BOX_MAX_Y),
                        shadeColor(color, part.getBoxBound(box, EngineSetting.BOX_MAX_Y), maxY));

            drawQuads(pixels, width, height, scale, part, true, maxZ, shadeColor(color, part.getMaxY(), maxY));
        }

        for (int slot = 0; slot < layout.getCargoCount(); slot++)
            fillBox(pixels, width, height, scale,
                    layout.getCargoBound(slot, EngineSetting.BOX_MIN_X),
                    maxZ - layout.getCargoBound(slot, EngineSetting.BOX_MAX_Z),
                    layout.getCargoBound(slot, EngineSetting.BOX_MAX_X),
                    maxZ - layout.getCargoBound(slot, EngineSetting.BOX_MIN_Z),
                    layout.getCargoBound(slot, EngineSetting.BOX_MAX_Y),
                    shadeColor(EngineSetting.SHIPWRIGHT_COLOR_CARGO,
                            layout.getCargoBound(slot, EngineSetting.BOX_MAX_Y), maxY));
    }

    // A sail's canvas laid out fore and aft about its mast at a share of its breadth, behind every spar, so the
    // side view shows the sail plan
    private void drawSailPlan(byte[] pixels, int width, int height, int scale, VehiclePartStruct sail) {

        int middle = (sail.getMinX() + sail.getMaxX()) / 2;
        int half = Math.round((sail.getMaxZ() - sail.getMinZ()) * 0.5f * EngineSetting.SHIPWRIGHT_SAIL_PLAN_SHARE);

        fillBox(pixels, width, height, scale, middle - half, sail.getMinY(), middle + half, sail.getMaxY(),
                Integer.MIN_VALUE + 1, EngineSetting.SHIPWRIGHT_COLOR_SAIL_PLAN);
    }

    // Every rope quad of a part as the four edges it is drawn between, seen from starboard or from above
    private void drawQuads(
            byte[] pixels,
            int width,
            int height,
            int scale,
            VehiclePartStruct part,
            boolean fromAbove,
            int maxZ,
            int color) {

        int depth = fromAbove ? part.getMaxY() : part.getMaxZ();

        for (int quad = 0; quad < part.getQuadCount(); quad++)
            for (int corner = 0; corner < EngineSetting.QUAD_VERTEX_COUNT; corner++) {

                int next = (corner + 1) % EngineSetting.QUAD_VERTEX_COUNT;

                drawLine(pixels, width, height, depth, color,
                        part.getQuadCorner(quad, corner, EngineSetting.AXIS_X) / scale,
                        resolveViewV(part, quad, corner, fromAbove, maxZ) / scale,
                        part.getQuadCorner(quad, next, EngineSetting.AXIS_X) / scale,
                        resolveViewV(part, quad, next, fromAbove, maxZ) / scale);
            }
    }

    // A quad corner's height in the side view, or its breadth from the far side in the top view, in sub-voxels
    private int resolveViewV(VehiclePartStruct part, int quad, int corner, boolean fromAbove, int maxZ) {
        return fromAbove
                ? maxZ - part.getQuadCorner(quad, corner, EngineSetting.AXIS_Z)
                : part.getQuadCorner(quad, corner, EngineSetting.AXIS_Y);
    }

    private void drawLine(byte[] pixels, int width, int height, int depth, int color, int u0, int v0, int u1,
            int v1) {

        int steps = Math.max(1, Math.max(Math.abs(u1 - u0), Math.abs(v1 - v0)));

        for (int step = 0; step <= steps; step++) {

            int u = u0 + (u1 - u0) * step / steps;
            int v = v0 + (v1 - v0) * step / steps;

            if (u < 0 || v < 0 || u >= width || v >= height || depthScratch[u + v * width] > depth)
                continue;

            depthScratch[u + v * width] = depth;
            writePixel(pixels, width, u, v, color);
        }
    }

    // A box's face toward the viewer, kept wherever it lies nearer than what is drawn
    private void fillBox(
            byte[] pixels,
            int width,
            int height,
            int scale,
            int minU,
            int minV,
            int maxU,
            int maxV,
            int depth,
            int color) {

        int startU = Math.max(0, minU / scale);
        int startV = Math.max(0, minV / scale);
        int endU = Math.min(width, Math.max(startU + 1, ceilDivide(maxU, scale)));
        int endV = Math.min(height, Math.max(startV + 1, ceilDivide(maxV, scale)));

        for (int v = startV; v < endV; v++)
            for (int u = startU; u < endU; u++) {

                int index = u + v * width;

                if (depthScratch[index] > depth)
                    continue;

                depthScratch[index] = depth;
                writePixel(pixels, width, u, v, color);
            }
    }

    private int resolveMaterialColor(String textureName) {

        for (int i = 0; i < EngineSetting.SHIP_MATERIAL_TEXTURES.length; i++)
            if (EngineSetting.SHIP_MATERIAL_TEXTURES[i].equals(textureName))
                return EngineSetting.SHIPWRIGHT_VIEW_COLORS[i];

        return EngineSetting.SHIPWRIGHT_COLOR_DEFAULT;
    }

    // A colour darkened toward the far side the farther its face lies from the viewer
    private int shadeColor(int color, int depth, int maxDepth) {

        float shade = Math.max(EngineSetting.SHIPWRIGHT_VIEW_SHADE_FAR, Math.min(1f,
                EngineSetting.SHIPWRIGHT_VIEW_SHADE_FAR
                        + (1f - EngineSetting.SHIPWRIGHT_VIEW_SHADE_FAR) * depth / Math.max(1, maxDepth)));
        int red = Math.round(((color >>> 24) & 0xFF) * shade);
        int green = Math.round(((color >>> 16) & 0xFF) * shade);
        int blue = Math.round(((color >>> 8) & 0xFF) * shade);

        return (red << 24) | (green << 16) | (blue << 8) | (color & 0xFF);
    }

    // Texels \\

    // Every texel of one octant of the plan, at an octant along the grid and across the full breadth
    private void fillOctant(ShipwrightSessionInstance session, int x, int z, int color) {

        int scale = EngineSetting.SHIPWRIGHT_INK_SCALE;

        fillPlan(session, x * scale, z * scale, (x + 1) * scale, (z + 1) * scale, color);
    }

    // Every plan texel of a rectangle, in texels along the grid and across the full breadth, starboard drawn down
    private void fillPlan(ShipwrightSessionInstance session, int minX, int minZ, int maxX, int maxZ, int color) {

        int width = session.getSheetWidth() * EngineSetting.SHIPWRIGHT_INK_SCALE;
        int height = session.getSheetHeight() * EngineSetting.SHIPWRIGHT_INK_SCALE;

        for (int z = Math.max(0, minZ); z < Math.min(height, maxZ); z++)
            for (int x = Math.max(0, minX); x < Math.min(width, maxX); x++)
                writePixel(session.getSheetPixels(), width, x, height - 1 - z, color);
    }

    private void writePixel(byte[] pixels, int width, int u, int v, int color) {

        int index = (u + v * width) * EngineSetting.COLOR_CHANNEL_COUNT;

        pixels[index] = (byte) (color >>> 24);
        pixels[index + 1] = (byte) (color >>> 16);
        pixels[index + 2] = (byte) (color >>> 8);
        pixels[index + 3] = (byte) color;
    }

    // Utility \\

    private static int ceilDivide(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
