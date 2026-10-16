package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrays;

class ShipwrightHullBranch extends BranchPackage {

    /*
     * Lays a blueprint's hull and everything built into it into the octant
     * grid. The drawn volume is shelled one octant thick wherever it meets the
     * outside, its top capped by the weather deck, so the hollow inside is
     * sealed; every interior deck floors the inside at its layer. A porch hangs
     * off the stern at its level as wide as the stern is there. A level is any
     * layer some floor lies open to the sky or the room above. A wall rises
     * from its level's floor to the floor or deck above it, or stands a
     * deckhouse high in the open; rails ring every open edge of the weather
     * deck at their height, and the porch is always railed. A stair climbs one
     * octant per step along the way it faces, two octants wide, until it meets
     * a floor level with its last tread, and opens a hatch in every floor over
     * its run so it can be climbed — up through a deck or onto a raised one.
     * Every query about where floors lie goes through here.
     */

    // Shell \\

    // The drawn volume shelled where it meets the outside, its top as the weather deck, its inside left hollow
    void layShell(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        ShipOctantGridStruct grid = layout.getGrid();
        int originX = layout.getOriginX();

        for (int y = 0; y < blueprint.getHeight(); y++)
            for (int z = 0; z < blueprint.getBreadth(); z++)
                for (int x = 0; x < blueprint.getLength(); x++) {

                    if (!blueprint.isVolume(x, y, z))
                        continue;

                    if (!blueprint.isVolume(x, y + 1, z))
                        grid.set(originX + x, y, z, ShipPiece.WEATHER_DECK);
                    else if (isShell(blueprint, x, y, z))
                        grid.set(originX + x, y, z, ShipPiece.HULL);
                }
    }

    private boolean isShell(ShipBlueprintStruct blueprint, int x, int y, int z) {
        return !blueprint.isVolume(x + 1, y, z) || !blueprint.isVolume(x - 1, y, z)
                || !blueprint.isVolume(x, y, z + 1) || !blueprint.isVolume(x, y, z - 1)
                || !blueprint.isVolume(x, y - 1, z);
    }

    // Decks \\

    void layDecks(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        ShipOctantGridStruct grid = layout.getGrid();
        IntArrayList decks = blueprint.getDecks();

        for (int i = 0; i < decks.size(); i++) {

            int y = decks.getInt(i);

            for (int z = 0; z < blueprint.getBreadth(); z++)
                for (int x = 0; x < blueprint.getLength(); x++)
                    if (blueprint.isVolume(x, y, z))
                        grid.setIfEmpty(layout.getOriginX() + x, y, z, ShipPiece.DECKS);
        }
    }

    // Porch \\

    // A floor hanging off the stern at the porch level, as wide as the stern is there
    void layPorch(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        if (!blueprint.hasPorch())
            return;

        int level = blueprint.getPorchLevel();
        int stern = findStern(blueprint, level);

        if (stern == EngineSetting.INDEX_NOT_FOUND) {
            layout.reportProblem(EngineSetting.SHIPWRIGHT_PROBLEM_PORCH);
            return;
        }

        ShipOctantGridStruct grid = layout.getGrid();

        for (int z = 0; z < blueprint.getBreadth(); z++) {

            if (!blueprint.isVolume(stern, level, z))
                continue;

            for (int depth = 1; depth <= EngineSetting.SHIP_PORCH_DEPTH; depth++)
                grid.set(layout.getOriginX() + stern - depth, level, z, ShipPiece.PORCH);
        }
    }

    // The aftmost octant along the length the hull reaches at a layer, INDEX_NOT_FOUND when it reaches none
    private int findStern(ShipBlueprintStruct blueprint, int y) {

        for (int x = 0; x < blueprint.getLength(); x++)
            for (int z = 0; z < blueprint.getBreadth(); z++)
                if (blueprint.isVolume(x, y, z))
                    return x;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Levels \\

    // Every layer a floor lies open at, sorted
    void collectLevels(ShipLayoutStruct layout) {

        ShipOctantGridStruct grid = layout.getGrid();

        for (int y = 0; y < grid.getSizeY(); y++)
            for (int z = 0; z < grid.getSizeZ(); z++)
                for (int x = 0; x < grid.getSizeX(); x++)
                    if (isFloor(grid, x, y, z) && grid.isEmpty(x, y + 1, z))
                        layout.addLevel(y);

        IntArrays.quickSort(layout.getLevels().elements(), 0, layout.getLevels().size());
    }

    // Walls \\

    void layWalls(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList walls = blueprint.getWalls();

        for (int i = 0; i < walls.size(); i++) {

            int cell = walls.getInt(i);

            layWall(
                    blueprint,
                    layout,
                    ShipCellUtility.unpackLevel(cell),
                    ShipCellUtility.unpackX(cell),
                    ShipCellUtility.unpackZ(cell));
        }
    }

    // A wall rises from its floor to the next floor above it under cover, or a deckhouse high in the open
    private void layWall(ShipBlueprintStruct blueprint, ShipLayoutStruct layout, int level, int x, int z) {

        ShipOctantGridStruct grid = layout.getGrid();
        int gridX = layout.getOriginX() + x;

        if (!isFloor(grid, gridX, level, z))
            return;

        boolean covered = blueprint.isVolume(x, level + 1, z);
        int top = covered ? grid.getSizeY() : level + 1 + EngineSetting.SHIP_DECKHOUSE_HEIGHT;

        for (int y = level + 1; y < Math.min(top, grid.getSizeY()); y++) {

            if (!grid.isEmpty(gridX, y, z) || covered && !blueprint.isVolume(x, y, z))
                return;

            grid.set(gridX, y, z, ShipPiece.WALLS);
        }
    }

    // Rails \\

    // Rails along every open edge of the weather deck at the blueprint's height, and round the porch always
    void layRails(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        ShipOctantGridStruct grid = layout.getGrid();

        for (int y = 0; y < grid.getSizeY(); y++)
            for (int z = 0; z < grid.getSizeZ(); z++)
                for (int x = 0; x < grid.getSizeX(); x++) {

                    ShipPiece piece = grid.get(x, y, z);

                    if (piece == ShipPiece.WEATHER_DECK)
                        layRail(blueprint, layout, x, y, z, blueprint.getRailHeight());
                    else if (piece == ShipPiece.PORCH)
                        layRail(blueprint, layout, x, y, z, EngineSetting.SHIP_PORCH_RAIL_HEIGHT);
                }
    }

    private void layRail(ShipBlueprintStruct blueprint, ShipLayoutStruct layout, int x, int y, int z, int height) {

        if (height <= 0 || !isOpenEdge(blueprint, layout, x, y, z))
            return;

        ShipOctantGridStruct grid = layout.getGrid();

        for (int rise = 1; rise <= height; rise++) {

            if (!grid.isEmpty(x, y + rise, z))
                return;

            grid.set(x, y + rise, z, ShipPiece.RAILS);
        }
    }

    // True when the floor at an octant looks out over nothing on some side
    private boolean isOpenEdge(ShipBlueprintStruct blueprint, ShipLayoutStruct layout, int x, int y, int z) {
        return isOutboard(blueprint, layout, x + 1, y, z) || isOutboard(blueprint, layout, x - 1, y, z)
                || isOutboard(blueprint, layout, x, y, z + 1) || isOutboard(blueprint, layout, x, y, z - 1);
    }

    private boolean isOutboard(ShipBlueprintStruct blueprint, ShipLayoutStruct layout, int x, int y, int z) {
        return layout.getGrid().isEmpty(x, y, z) && !blueprint.isVolume(x - layout.getOriginX(), y, z);
    }

    // Stairs \\

    void layStairs(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList stairs = blueprint.getStairs();

        for (int i = 0; i < stairs.size(); i++)
            layStair(layout, stairs.getInt(i));
    }

    // One step an octant up per octant along the way it faces, until the next octant along holds a floor level with
    // its last tread — a deck over the run, opened above every step, or a raised deck the run climbs onto
    private void layStair(ShipLayoutStruct layout, int cell) {

        int level = ShipCellUtility.unpackLevel(cell);
        int spin = ShipCellUtility.unpackSpin(cell);
        int footX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int footZ = ShipCellUtility.unpackZ(cell);
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);
        int rise = findStairRise(layout.getGrid(), footX, footZ, stepX, stepZ, level);

        if (rise == EngineSetting.SHIP_LEVEL_NONE) {
            layout.reportProblem(EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_NO_FLOOR);
            return;
        }

        int acrossX = stepX == 0 ? 1 : 0;
        int acrossZ = stepZ == 0 ? 1 : 0;

        for (int step = 0; step < rise; step++)
            for (int across = 0; across < EngineSetting.SHIP_STAIR_WIDTH; across++) {

                int x = footX + stepX * step + acrossX * across;
                int z = footZ + stepZ * step + acrossZ * across;

                if (!layStep(layout, x, z, level + 1, level + 1 + step, level + rise))
                    return;
            }

        for (int across = 0; across < EngineSetting.SHIP_STAIR_WIDTH; across++)
            clearLanding(
                    layout.getGrid(),
                    footX + stepX * rise + acrossX * across,
                    level + rise,
                    footZ + stepZ * rise + acrossZ * across);
    }

    // The rail along a deck's edge opened where a stair climbs onto it
    private void clearLanding(ShipOctantGridStruct grid, int x, int floor, int z) {

        for (int rise = 1; rise <= EngineSetting.SHIP_MAX_RAIL_HEIGHT; rise++)
            if (grid.get(x, floor + rise, z) == ShipPiece.RAILS)
                grid.clear(x, floor + rise, z);
    }

    // The octants a stair rises: the first run whose next octant along holds a floor that high, SHIP_LEVEL_NONE
    // when none does within the tallest stair
    private int findStairRise(ShipOctantGridStruct grid, int footX, int footZ, int stepX, int stepZ, int level) {

        for (int rise = 1; rise <= EngineSetting.SHIP_STAIR_MAX_RISE; rise++)
            if (isFloor(grid, footX + stepX * rise, level + rise, footZ + stepZ * rise))
                return rise;

        return EngineSetting.SHIP_LEVEL_NONE;
    }

    // A step's riser filled up to its tread and the floors over it opened; false when it runs into the hull
    private boolean layStep(ShipLayoutStruct layout, int x, int z, int bottom, int tread, int top) {

        ShipOctantGridStruct grid = layout.getGrid();

        for (int y = bottom; y <= top; y++) {

            if (!grid.contains(x, y, z) || grid.get(x, y, z) == ShipPiece.HULL) {
                layout.reportProblem(EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_HULL);
                return false;
            }

            if (y <= tread)
                grid.set(x, y, z, ShipPiece.STAIRS);
            else
                grid.clear(x, y, z);
        }

        return true;
    }

    // Queries \\

    boolean isFloor(ShipOctantGridStruct grid, int x, int y, int z) {

        ShipPiece piece = grid.get(x, y, z);

        return piece != null && piece.isFloor();
    }

    // The highest layer the drawn volume reaches in one column, SHIP_LEVEL_NONE when it reaches none
    int findColumnTop(ShipBlueprintStruct blueprint, int x, int z) {

        for (int y = blueprint.getHeight() - 1; y >= 0; y--)
            if (blueprint.isVolume(x, y, z))
                return y;

        return EngineSetting.SHIP_LEVEL_NONE;
    }

    int findKeel(ShipBlueprintStruct blueprint, int x, int z) {

        for (int y = 0; y < blueprint.getHeight(); y++)
            if (blueprint.isVolume(x, y, z))
                return y;

        return EngineSetting.SHIP_LEVEL_NONE;
    }
}
