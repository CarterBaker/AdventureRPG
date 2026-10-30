package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import java.util.Arrays;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOpening;
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
     * session's own pixels, one texel per octant on the sheet, the paper and
     * its ruling left to the sheet's shader. The sheet is always the plan of
     * the deck picked. While a deck is drawn its outline is inked, blue where
     * it lies under the waterline, the decks under and over it faint, and the
     * hull's outline where the water meets it ruled across; with any other
     * tool it is the deck as it was built — floor, planking, bulwarks, stairs
     * and hatches — with every wall, half wall, door, window, stair foot,
     * piece of furniture, the helm and the capstan on it straight from the
     * blueprint, so a stroke shows as it is drawn, and every hatch and ladder
     * where it was built. While a stair, ladder or hatch is in hand its whole
     * run or footprint is laid faintly under the cursor, red where it would
     * leave the deck, so it is placed knowing it fits. Every mast is marked,
     * the picked one brighter. Plans run bow to the right and starboard down. The
     * views are every part's boxes seen from starboard and from above,
     * nearest drawn over farthest, shaded by depth and coloured by what they
     * are made of, with the cargo in its places and the sea below the
     * waterline the ship floats at; the side view lays every sail out fore and
     * aft behind the spars as a sail plan does; both are drawn at one scale,
     * as fine as fits.
     */

    // Internal
    private ShipwrightFittingBranch shipwrightFittingBranch;

    // Scratch — main thread only
    private int[] depthScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.depthScratch = new int[Math.max(
                EngineSetting.SHIPWRIGHT_SIDE_TEXELS_X * EngineSetting.SHIPWRIGHT_SIDE_TEXELS_Y,
                EngineSetting.SHIPWRIGHT_TOP_TEXELS_X * EngineSetting.SHIPWRIGHT_TOP_TEXELS_Y)];
    }

    @Override
    protected void get() {
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
    }

    // Sheet \\

    // The octants the sheet spans across and up, written to the session
    void resolveSheetSize(ShipwrightSessionInstance session) {
        session.setSheetSize(EngineSetting.SHIP_AFT_MARGIN + EngineSetting.SHIP_SHEET_LENGTH,
                EngineSetting.SHIP_SHEET_BREADTH);
    }

    void drawSheet(ShipwrightSessionInstance session) {

        resolveSheetSize(session);
        Arrays.fill(session.getSheetPixels(), 0, session.getSheetWidth() * session.getSheetHeight()
                * EngineSetting.COLOR_CHANNEL_COUNT, (byte) 0);

        if (session.getTool() == ShipwrightTool.DECK)
            drawOutlines(session);
        else
            drawDeck(session);

        drawMasts(session);
    }

    // The deck picked inked, blue under the waterline, the decks under and over it faint, the waterline ruled
    private void drawOutlines(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipLayoutStruct layout = session.getLayout();
        int deck = session.getDeck();
        boolean submerged = layout.isAfloat()
                && (deck * EngineSetting.SHIP_DECK_HEIGHT + 1) * EngineSetting.SUB_BLOCK_SIZE <= layout.getDraft();

        for (int z = 0; z < EngineSetting.SHIP_SHEET_BREADTH; z++)
            for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++) {

                int color;

                if (blueprint.isOnDeck(deck, x, z))
                    color = submerged ? EngineSetting.SHIPWRIGHT_INK_SUBMERGED : EngineSetting.SHIPWRIGHT_INK_HULL;
                else if (blueprint.isOnDeck(deck - 1, x, z))
                    color = EngineSetting.SHIPWRIGHT_INK_BELOW;
                else if (blueprint.isOnDeck(deck + 1, x, z))
                    color = EngineSetting.SHIPWRIGHT_INK_ABOVE;
                else
                    continue;

                writePlan(session, EngineSetting.SHIP_AFT_MARGIN + x, z, color);
            }

        drawWaterline(session, layout);
    }

    // The ring where the water meets the hull at the waterline the ship floats at
    private void drawWaterline(ShipwrightSessionInstance session, ShipLayoutStruct layout) {

        if (!layout.isAfloat())
            return;

        int layer = Math.max(0, (int) Math.ceil(layout.getDraft() / EngineSetting.SUB_BLOCK_SIZE) - 1);

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++) {

            int halfBeam = layout.getWaterlineSection(x, layer);
            int sternward = layout.getWaterlineSection(x - 1, layer);
            int bowward = layout.getWaterlineSection(x + 1, layer);

            for (int halfZ = 0; halfZ < halfBeam; halfZ++)
                if (halfZ == halfBeam - 1 || halfZ >= sternward || halfZ >= bowward) {
                    writePlan(session, EngineSetting.SHIP_AFT_MARGIN + x, ShipCellUtility.toStarboard(halfZ),
                            EngineSetting.SHIPWRIGHT_INK_WATERLINE);
                    writePlan(session, EngineSetting.SHIP_AFT_MARGIN + x, ShipCellUtility.toPort(halfZ),
                            EngineSetting.SHIPWRIGHT_INK_WATERLINE);
                }
        }
    }

    // The deck picked as it was built, and everything set down on it
    private void drawDeck(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipLayoutStruct layout = session.getLayout();
        int deck = session.getDeck();

        if (layout.getSectionLayers() > 0)
            drawFloor(session, layout.getGrid(), deck * EngineSetting.SHIP_DECK_HEIGHT);

        drawCells(session, blueprint.getWalls(), deck, EngineSetting.SHIPWRIGHT_INK_WALL);
        drawCells(session, blueprint.getHalfWalls(), deck, EngineSetting.SHIPWRIGHT_INK_HALF_WALL);
        drawCells(session, blueprint.getWindows(), deck, EngineSetting.SHIPWRIGHT_INK_WINDOW);
        drawCells(session, blueprint.getStairs(), deck, EngineSetting.SHIPWRIGHT_INK_STAIR_FOOT);
        drawDoors(session, blueprint, deck);
        drawFurniture(session, blueprint, deck);
        drawFitting(session, blueprint.getHelm(), deck, 1, EngineSetting.SHIP_FITTING_OCTANTS);
        drawFitting(session, blueprint.getCapstan(), deck, EngineSetting.SHIP_FITTING_OCTANTS,
                EngineSetting.SHIP_FITTING_OCTANTS);
        drawFittingBoxes(session, layout.getHatchBoxes(), deck * EngineSetting.SHIP_DECK_HEIGHT,
                EngineSetting.SHIPWRIGHT_INK_HATCH);
        drawFittingBoxes(session, layout.getLadderBoxes(), deck * EngineSetting.SHIP_DECK_HEIGHT + 1,
                EngineSetting.SHIPWRIGHT_INK_LADDER);
        drawGhost(session);
    }

    // Every hatch or ladder whose octant box starts at a layer, where it was built
    private void drawFittingBoxes(ShipwrightSessionInstance session, IntArrayList boxes, int layer, int color) {

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++) {

            if (ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Y) != layer)
                continue;

            for (int z = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Z);
                    z < ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Z); z++)
                for (int x = ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_X);
                        x < ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_X); x++)
                    writePlan(session, x, z, color);
        }
    }

    // Ghost \\

    // The whole of what the tool in hand would set down at the cursor, faint where it fits and red where it leaves
    // the deck: a stair's run from the floor before its foot to its landing, or a ladder's or hatch's square
    private void drawGhost(ShipwrightSessionInstance session) {

        ShipwrightTool tool = session.getTool();
        boolean square = tool == ShipwrightTool.STAIRS && session.isLadders()
                || tool == ShipwrightTool.OPENINGS && session.getOpening() == ShipOpening.HATCH;

        if (tool != ShipwrightTool.STAIRS && !square)
            return;

        int x = session.getCursorU() - EngineSetting.SHIP_AFT_MARGIN;
        int z = EngineSetting.SHIP_SHEET_BREADTH - 1 - session.getCursorV();

        if (square) {
            drawGhostRect(session, x, z, EngineSetting.SHIP_HATCH_OCTANTS, EngineSetting.SHIP_HATCH_OCTANTS);
            return;
        }

        int spin = session.getSpin();
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);
        int acrossX = stepX == 0 ? 1 : 0;
        int acrossZ = stepZ == 0 ? 1 : 0;
        boolean fits = true;

        for (int pass = 0; pass < 2; pass++)
            for (int along = -EngineSetting.SHIP_STAIR_HEADROOM; along <= EngineSetting.SHIP_DECK_HEIGHT; along++)
                for (int across = 0; across < EngineSetting.SHIP_STAIR_WIDTH; across++) {

                    int atX = x + stepX * along + acrossX * across;
                    int atZ = z + stepZ * along + acrossZ * across;

                    if (pass == 0)
                        fits &= session.getBlueprint().isOnDeck(session.getDeck(), atX, atZ);
                    else
                        writeGhost(session, atX, atZ, fits);
                }
    }

    private void drawGhostRect(ShipwrightSessionInstance session, int x, int z, int sizeX, int sizeZ) {

        boolean fits = true;

        for (int atZ = z; atZ < z + sizeZ; atZ++)
            for (int atX = x; atX < x + sizeX; atX++)
                fits &= session.getBlueprint().isOnDeck(session.getDeck(), atX, atZ);

        for (int atZ = z; atZ < z + sizeZ; atZ++)
            for (int atX = x; atX < x + sizeX; atX++)
                writeGhost(session, atX, atZ, fits);
    }

    private void writeGhost(ShipwrightSessionInstance session, int x, int z, boolean fits) {
        writePlan(session, EngineSetting.SHIP_AFT_MARGIN + x, z,
                fits ? EngineSetting.SHIPWRIGHT_INK_GHOST : EngineSetting.SHIPWRIGHT_INK_GHOST_BLOCKED);
    }

    private void drawFloor(ShipwrightSessionInstance session, ShipOctantGridStruct grid, int floor) {

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
                else if (standing != null)
                    color = EngineSetting.SHIPWRIGHT_INK_SHELL;
                else if (piece != null && piece.isFloor())
                    color = EngineSetting.SHIPWRIGHT_INK_FLOOR;
                else if (piece != null)
                    color = EngineSetting.SHIPWRIGHT_INK_SHELL;
                else
                    continue;

                writePlan(session, x, z, color);
            }
    }

    private void drawCells(ShipwrightSessionInstance session, IntArrayList cells, int deck, int color) {

        for (int i = 0; i < cells.size(); i++) {

            int cell = cells.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) == deck)
                writePlan(
                        session,
                        EngineSetting.SHIP_AFT_MARGIN + ShipCellUtility.unpackX(cell),
                        ShipCellUtility.unpackZ(cell),
                        color);
        }
    }

    private void drawDoors(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int deck) {

        IntArrayList doors = blueprint.getDoors();

        for (int i = 0; i < doors.size(); i++) {

            int cell = doors.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) != deck)
                continue;

            shipwrightFittingBranch.resolveFootprint(
                    EngineSetting.SHIP_DOOR_FURNISHING, ShipCellUtility.unpackSpin(cell));
            drawFootprint(session, cell, EngineSetting.SHIPWRIGHT_INK_DOOR);
        }
    }

    private void drawFurniture(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int deck) {

        ObjectArrayList<ShipFurnitureStruct> furniture = blueprint.getFurniture();

        for (int i = 0; i < furniture.size(); i++) {

            ShipFurnitureStruct piece = furniture.get(i);

            if (ShipCellUtility.unpackLevel(piece.getCell()) != deck
                    || !shipwrightFittingBranch.isKnownFurnishing(piece.getFurnishingName()))
                continue;

            shipwrightFittingBranch.resolveFootprint(
                    piece.getFurnishingName(), ShipCellUtility.unpackSpin(piece.getCell()));
            drawFootprint(session, piece.getCell(), EngineSetting.SHIPWRIGHT_INK_FURNITURE);
        }
    }

    private void drawFitting(ShipwrightSessionInstance session, int cell, int deck, int sizeX, int sizeZ) {

        if (cell == EngineSetting.SHIP_CELL_NONE || ShipCellUtility.unpackLevel(cell) != deck)
            return;

        int startX = EngineSetting.SHIP_AFT_MARGIN + ShipCellUtility.unpackX(cell);
        int startZ = ShipCellUtility.unpackZ(cell);

        for (int z = startZ; z < startZ + sizeZ; z++)
            for (int x = startX; x < startX + sizeX; x++)
                writePlan(session, x, z, EngineSetting.SHIPWRIGHT_INK_FITTING);
    }

    // The octants the footprint last resolved covers from a cell
    private void drawFootprint(ShipwrightSessionInstance session, int cell, int color) {

        int startX = EngineSetting.SHIP_AFT_MARGIN + ShipCellUtility.unpackX(cell);
        int startZ = ShipCellUtility.unpackZ(cell);

        for (int z = startZ; z < startZ + shipwrightFittingBranch.getFootprint(EngineSetting.AXIS_Z); z++)
            for (int x = startX; x < startX + shipwrightFittingBranch.getFootprint(EngineSetting.AXIS_X); x++)
                writePlan(session, x, z, color);
    }

    // Every mast on the centre line, the one picked brighter while masts are being stepped
    private void drawMasts(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int center = EngineSetting.SHIP_SHEET_HALF_BREADTH;
        boolean picking = session.getTool() == ShipwrightTool.MASTS;

        for (int i = 0; i < blueprint.getMasts().size(); i++) {

            ShipMastStruct mast = blueprint.getMasts().get(i);
            int color = picking && i == session.getMastIndex()
                    ? EngineSetting.SHIPWRIGHT_INK_PICKED_MAST
                    : EngineSetting.SHIPWRIGHT_INK_MAST;

            for (int z = center - 1; z <= center; z++)
                for (int x = mast.getX(); x < mast.getX() + EngineSetting.SHIP_MAST_OCTANTS; x++)
                    writePlan(session, EngineSetting.SHIP_AFT_MARGIN + x, z, color);
        }
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

    // A plan texel at an octant along the grid and across the full breadth, starboard drawn down
    private void writePlan(ShipwrightSessionInstance session, int x, int z, int color) {
        writeTexel(session, x, session.getSheetHeight() - 1 - z, color);
    }

    private void writeTexel(ShipwrightSessionInstance session, int u, int v, int color) {

        if (u < 0 || v < 0 || u >= session.getSheetWidth() || v >= session.getSheetHeight())
            return;

        writePixel(session.getSheetPixels(), session.getSheetWidth(), u, v, color);
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
