package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import java.util.Arrays;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipFurnitureStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipMastStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSessionInstance;
import application.bootstrap.shipwrightpipeline.shipwright.ShipwrightSheet;
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
     * its ruling left to the sheet's shader. A hull plan inks the layer being
     * drawn, the layers under and over it faint and the whole hull's outline
     * fainter still; a side elevation shows the hull's profile with every deck
     * and the waterline ruled across it and the layer picked shaded; a level
     * plan shows the floor of the level picked with its walls, shell, rails,
     * stairs, doors, furniture, helm, capstan and masts, walls straight from
     * the blueprint so a stroke shows as it is drawn; a deck plan shades the
     * weather deck by height under the masts, the picked one marked. Plans run
     * bow to the right and starboard down. The views are every part's boxes
     * seen from starboard and from above, nearest drawn over farthest, shaded
     * by depth and coloured by what they are made of, with the cargo in its
     * places and the sea below the design waterline; the side view lays every
     * sail out fore and aft behind the spars as a sail plan does; both are drawn at one
     * scale, as fine as fits.
     */

    // Internal
    private ShipwrightHullBranch shipwrightHullBranch;
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
        this.shipwrightHullBranch = get(ShipwrightHullBranch.class);
        this.shipwrightFittingBranch = get(ShipwrightFittingBranch.class);
    }

    // Sheet \\

    // The octants the sheet for a tool spans across and up, written to the session
    void resolveSheetSize(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int width = EngineSetting.SHIP_AFT_MARGIN + blueprint.getLength();
        int height = session.getTool().getSheet().isPlan()
                ? blueprint.getBreadth()
                : blueprint.getHeight() + EngineSetting.SHIP_HEADROOM;

        session.setSheetSize(width, height);
    }

    void drawSheet(ShipwrightSessionInstance session) {

        resolveSheetSize(session);
        Arrays.fill(session.getSheetPixels(), 0, session.getSheetWidth() * session.getSheetHeight()
                * EngineSetting.COLOR_CHANNEL_COUNT, (byte) 0);

        ShipwrightSheet sheet = session.getTool().getSheet();

        switch (sheet) {
            case HULL_PLAN -> drawHullPlan(session);
            case SIDE_ELEVATION -> drawSideElevation(session);
            case LEVEL_PLAN -> drawLevelPlan(session);
            default -> drawDeckPlan(session);
        }
    }

    // The layer being drawn inked, the layers under and over it faint, the whole hull fainter
    private void drawHullPlan(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        int layer = session.getLayer();

        for (int z = 0; z < blueprint.getBreadth(); z++)
            for (int x = 0; x < blueprint.getLength(); x++) {

                int color;

                if (blueprint.isVolume(x, layer, z))
                    color = EngineSetting.SHIPWRIGHT_INK_HULL;
                else if (blueprint.isVolume(x, layer - 1, z))
                    color = EngineSetting.SHIPWRIGHT_INK_BELOW;
                else if (blueprint.isVolume(x, layer + 1, z))
                    color = EngineSetting.SHIPWRIGHT_INK_ABOVE;
                else if (hasColumn(blueprint, x, z))
                    color = EngineSetting.SHIPWRIGHT_INK_OUTLINE;
                else
                    continue;

                writePlan(session, EngineSetting.SHIP_AFT_MARGIN + x, z, color);
            }
    }

    // The hull's profile from starboard, every deck and the waterline ruled across it, the layer picked shaded
    private void drawSideElevation(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();

        for (int y = 0; y < blueprint.getHeight(); y++)
            for (int x = 0; x < blueprint.getLength(); x++) {

                boolean profile = hasProfile(blueprint, x, y);
                int color;

                if (profile && blueprint.hasDeck(y))
                    color = EngineSetting.SHIPWRIGHT_INK_DECK;
                else if (y == blueprint.getWaterline())
                    color = profile
                            ? EngineSetting.SHIPWRIGHT_INK_WATERLINE_HULL
                            : EngineSetting.SHIPWRIGHT_INK_WATERLINE;
                else if (profile)
                    color = EngineSetting.SHIPWRIGHT_INK_PROFILE;
                else if (y == session.getLayer())
                    color = EngineSetting.SHIPWRIGHT_INK_PICKED;
                else
                    continue;

                writeTexel(session, EngineSetting.SHIP_AFT_MARGIN + x, y, color);
            }
    }

    // One level's floor and everything standing on it, seen from above
    private void drawLevelPlan(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();
        ShipLayoutStruct layout = session.getLayout();
        int level = session.getLevel();

        if (level == EngineSetting.SHIP_LEVEL_NONE || layout.getParts().isEmpty())
            return;

        drawLevelGrid(session, layout.getGrid(), level);
        drawWalls(session, blueprint, level);
        drawDoors(session, blueprint, level);
        drawFurniture(session, blueprint, level);
        drawStairFeet(session, blueprint, level);
        drawFitting(session, blueprint.getHelm(), level, 1, EngineSetting.SHIP_FITTING_OCTANTS);
        drawFitting(session, blueprint.getCapstan(), level, EngineSetting.SHIP_FITTING_OCTANTS,
                EngineSetting.SHIP_FITTING_OCTANTS);
        drawMasts(session, blueprint, EngineSetting.SHIPWRIGHT_INK_MAST);
    }

    private void drawLevelGrid(ShipwrightSessionInstance session, ShipOctantGridStruct grid, int level) {

        for (int z = 0; z < grid.getSizeZ(); z++)
            for (int x = 0; x < grid.getSizeX(); x++) {

                ShipPiece floor = grid.get(x, level, z);
                ShipPiece standing = grid.get(x, level + 1, z);
                int color;

                if (standing == ShipPiece.STAIRS)
                    color = EngineSetting.SHIPWRIGHT_INK_STAIRS;
                else if (standing == ShipPiece.RAILS)
                    color = EngineSetting.SHIPWRIGHT_INK_RAIL;
                else if (standing == ShipPiece.WALLS)
                    color = EngineSetting.SHIPWRIGHT_INK_WALL;
                else if (standing != null)
                    color = EngineSetting.SHIPWRIGHT_INK_SHELL;
                else if (floor != null && floor.isFloor())
                    color = EngineSetting.SHIPWRIGHT_INK_FLOOR;
                else if (floor != null)
                    color = EngineSetting.SHIPWRIGHT_INK_SHELL;
                else
                    continue;

                writePlan(session, x, z, color);
            }
    }

    private void drawWalls(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int level) {

        IntArrayList walls = blueprint.getWalls();

        for (int i = 0; i < walls.size(); i++) {

            int cell = walls.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) == level)
                writePlan(
                        session,
                        EngineSetting.SHIP_AFT_MARGIN + ShipCellUtility.unpackX(cell),
                        ShipCellUtility.unpackZ(cell),
                        EngineSetting.SHIPWRIGHT_INK_WALL);
        }
    }

    private void drawDoors(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int level) {

        IntArrayList doors = blueprint.getDoors();

        for (int i = 0; i < doors.size(); i++) {

            int cell = doors.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) != level)
                continue;

            shipwrightFittingBranch.resolveFootprint(
                    EngineSetting.SHIP_DOOR_FURNISHING, ShipCellUtility.unpackSpin(cell));
            drawFootprint(session, cell, EngineSetting.SHIPWRIGHT_INK_DOOR);
        }
    }

    private void drawFurniture(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int level) {

        ObjectArrayList<ShipFurnitureStruct> furniture = blueprint.getFurniture();

        for (int i = 0; i < furniture.size(); i++) {

            ShipFurnitureStruct piece = furniture.get(i);

            if (ShipCellUtility.unpackLevel(piece.getCell()) != level)
                continue;

            shipwrightFittingBranch.resolveFootprint(
                    piece.getFurnishingName(), ShipCellUtility.unpackSpin(piece.getCell()));
            drawFootprint(session, piece.getCell(), EngineSetting.SHIPWRIGHT_INK_FURNITURE);
        }
    }

    private void drawStairFeet(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int level) {

        IntArrayList stairs = blueprint.getStairs();

        for (int i = 0; i < stairs.size(); i++) {

            int cell = stairs.getInt(i);

            if (ShipCellUtility.unpackLevel(cell) == level)
                writePlan(
                        session,
                        EngineSetting.SHIP_AFT_MARGIN + ShipCellUtility.unpackX(cell),
                        ShipCellUtility.unpackZ(cell),
                        EngineSetting.SHIPWRIGHT_INK_STAIR_FOOT);
        }
    }

    private void drawFitting(ShipwrightSessionInstance session, int cell, int level, int sizeX, int sizeZ) {

        if (cell == EngineSetting.SHIP_CELL_NONE || ShipCellUtility.unpackLevel(cell) != level)
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

    // The weather deck shaded by height, every mast on it, the picked one marked
    private void drawDeckPlan(ShipwrightSessionInstance session) {

        ShipBlueprintStruct blueprint = session.getBlueprint();

        for (int z = 0; z < blueprint.getBreadth(); z++)
            for (int x = 0; x < blueprint.getLength(); x++) {

                int top = shipwrightHullBranch.findColumnTop(blueprint, x, z);

                if (top == EngineSetting.SHIP_LEVEL_NONE)
                    continue;

                int alpha = EngineSetting.SHIPWRIGHT_DECK_SHADE_MIN + (255 - EngineSetting.SHIPWRIGHT_DECK_SHADE_MIN)
                        * (top + 1) / blueprint.getHeight();

                writePlan(session, EngineSetting.SHIP_AFT_MARGIN + x, z,
                        (EngineSetting.SHIPWRIGHT_INK_FLOOR & ~0xFF) | alpha);
            }

        drawMasts(session, blueprint, EngineSetting.SHIPWRIGHT_INK_MAST);

        if (!session.hasMastPicked())
            return;

        drawMast(session, blueprint, blueprint.getMasts().get(session.getMastIndex()),
                EngineSetting.SHIPWRIGHT_INK_PICKED_MAST);
    }

    private void drawMasts(ShipwrightSessionInstance session, ShipBlueprintStruct blueprint, int color) {

        for (int i = 0; i < blueprint.getMasts().size(); i++)
            drawMast(session, blueprint, blueprint.getMasts().get(i), color);
    }

    private void drawMast(
            ShipwrightSessionInstance session,
            ShipBlueprintStruct blueprint,
            ShipMastStruct mast,
            int color) {

        int center = blueprint.getHalfBreadth();

        for (int z = center - 1; z <= center; z++)
            for (int x = mast.getX(); x < mast.getX() + EngineSetting.SHIP_MAST_OCTANTS; x++)
                writePlan(session, EngineSetting.SHIP_AFT_MARGIN + x, z, color);
    }

    // Views \\

    // Every part and the cargo from starboard and from above, at the finest scale both fit
    void drawViews(ShipwrightSessionInstance session) {

        ShipLayoutStruct layout = session.getLayout();
        ObjectArrayList<VehiclePartStruct> parts = layout.getParts();
        int maxX = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int maxY = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int maxZ = session.getBlueprint().getBreadth() * EngineSetting.SHIP_OCTANT_SUB_VOXELS;

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
        int waterline = Math.round(layout.getDraft() * EngineSetting.SUB_VOXEL_RESOLUTION / scale);

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

        for (int i = 0; i < EngineSetting.SHIPWRIGHT_VIEW_TEXTURES.length; i++)
            if (EngineSetting.SHIPWRIGHT_VIEW_TEXTURES[i].equals(textureName))
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

    private boolean hasColumn(ShipBlueprintStruct blueprint, int x, int z) {
        return shipwrightHullBranch.findColumnTop(blueprint, x, z) != EngineSetting.SHIP_LEVEL_NONE;
    }

    private boolean hasProfile(ShipBlueprintStruct blueprint, int x, int y) {

        for (int halfZ = 0; halfZ < blueprint.getHalfBreadth(); halfZ++)
            if (blueprint.isHull(x, y, halfZ))
                return true;

        return false;
    }

    private static int ceilDivide(int value, int divisor) {
        return (value + divisor - 1) / divisor;
    }
}
