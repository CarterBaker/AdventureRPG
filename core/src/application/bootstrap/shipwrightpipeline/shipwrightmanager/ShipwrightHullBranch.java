package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ShipwrightHullBranch extends BranchPackage {

    /*
     * Lays a blueprint's hull and everything built into it into the octant
     * grid. Every deck's floor lies a deck's height over the one below; the
     * hull's side between two decks is blended layer by layer from the
     * outline of the deck below to the outline of the deck above, so decks
     * drawn wider flare the side out and decks drawn narrower draw it in, and
     * a deck drawn past the end of the one below rakes its stem or its stern
     * up to it. Wherever no deck lies above, the deck below is open to the sky.
     * The hull is shelled one octant thick wherever it meets the outside, its
     * open decks capping it, so the hollow inside is sealed and every raised
     * deck's open end is closed by a bulkhead; interior decks floor the inside
     * at their height. Every open deck is ringed by a bulwark where it looks
     * out over nothing, the lowest of them the sill the sea comes aboard over.
     * A porch hangs off the stern at its deck as wide as the stern is there,
     * railed. A wall rises from its deck to the deck above it under cover, or
     * a deckhouse high in the open; a half wall stands waist high. A stair
     * climbs one octant per step along the way it faces, two octants wide,
     * until it meets a floor level with its last tread, and opens a hatch in
     * every floor over its run so it can be climbed. Every query about where
     * floors lie goes through here.
     */

    // Sections \\

    // The hull's half-beam at every layer and octant along the sheet, each layer between two decks blended from the
    // outline of the deck below to the outline of the deck above, nothing over a deck with none above it
    void laySections(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        int height = EngineSetting.SHIP_DECK_HEIGHT;
        int layers = blueprint.getDeckCount() * height;

        layout.setSectionLayers(layers);

        for (int y = 0; y < layers; y++)
            for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
                layout.setSection(x, y, resolveSection(blueprint, x, y / height, y % height));
    }

    private int resolveSection(ShipBlueprintStruct blueprint, int x, int deck, int rise) {

        int below = blueprint.getOutline(deck, x);

        if (rise == 0)
            return below;

        int above = blueprint.getOutline(deck + 1, x);

        if (above == 0)
            return 0;

        return Math.max(1, Math.round(below + (above - below) * (float) rise / EngineSetting.SHIP_DECK_HEIGHT));
    }

    // Shell \\

    // The sections shelled where they meet the outside: open floors as the weather deck, the bottom and every floor
    // over nothing sealed, interior floors laid as decks, the hollow between them left open
    void layShell(ShipLayoutStruct layout) {

        ShipOctantGridStruct grid = layout.getGrid();

        for (int y = 0; y < layout.getSectionLayers(); y++)
            for (int z = 0; z < EngineSetting.SHIP_SHEET_BREADTH; z++)
                for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++) {

                    if (!layout.isVolume(x, y, z))
                        continue;

                    ShipPiece piece = resolveShellPiece(layout, x, y, z);

                    if (piece != null)
                        grid.set(layout.getOriginX() + x, y, z, piece);
                }
    }

    private ShipPiece resolveShellPiece(ShipLayoutStruct layout, int x, int y, int z) {

        boolean side = !layout.isVolume(x + 1, y, z) || !layout.isVolume(x - 1, y, z)
                || !layout.isVolume(x, y, z + 1) || !layout.isVolume(x, y, z - 1);
        boolean open = !layout.isVolume(x, y + 1, z);
        boolean over = !layout.isVolume(x, y - 1, z);

        if (y % EngineSetting.SHIP_DECK_HEIGHT != 0)
            return side || open || over ? ShipPiece.HULL : null;

        if (open)
            return ShipPiece.WEATHER_DECK;

        if (side)
            return ShipPiece.HULL;

        return over ? ShipPiece.BOTTOM : ShipPiece.DECKS;
    }

    // Bulwarks \\

    // A bulwark round every open deck where it looks out over nothing, the lowest bulwark's top kept as the sill
    void layBulwarks(ShipLayoutStruct layout) {

        ShipOctantGridStruct grid = layout.getGrid();
        int sill = layout.getSectionLayers();

        for (int y = 0; y < layout.getSectionLayers(); y += EngineSetting.SHIP_DECK_HEIGHT)
            for (int z = 0; z < EngineSetting.SHIP_SHEET_BREADTH; z++)
                for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++) {

                    if (grid.get(layout.getOriginX() + x, y, z) != ShipPiece.WEATHER_DECK
                            || !isDeckEdge(layout, x, y, z))
                        continue;

                    sill = Math.min(sill, y + 1 + layBulwark(grid, layout.getOriginX() + x, y, z));
                }

        layout.setSillHeight(sill * EngineSetting.SUB_BLOCK_SIZE);
    }

    // The bulwark over one edge of an open deck, returning how many octants high it stands
    private int layBulwark(ShipOctantGridStruct grid, int gridX, int y, int z) {

        for (int rise = 1; rise <= EngineSetting.SHIP_BULWARK_HEIGHT; rise++) {

            if (!grid.isEmpty(gridX, y + rise, z))
                return rise - 1;

            grid.set(gridX, y + rise, z, ShipPiece.BULWARK);
        }

        return EngineSetting.SHIP_BULWARK_HEIGHT;
    }

    // True when an open floor looks out over nothing on some side and the floor inboard of it is open too, so a
    // narrow ledge left where a deck above draws the side in carries no bulwark
    private boolean isDeckEdge(ShipLayoutStruct layout, int x, int y, int z) {

        boolean edge = !layout.isVolume(x + 1, y, z) || !layout.isVolume(x - 1, y, z)
                || !layout.isVolume(x, y, z + 1) || !layout.isVolume(x, y, z - 1);

        if (!edge)
            return false;

        int inboard = ShipCellUtility.toHalfBreadth(z) == 0 ? z
                : z < EngineSetting.SHIP_SHEET_HALF_BREADTH ? z + 1 : z - 1;

        return !layout.isVolume(x, y + 1, inboard);
    }

    // Porch \\

    // A floor hanging off the stern at the porch's deck, as wide as the stern is there, railed where it looks out
    void layPorch(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        if (!blueprint.hasPorch())
            return;

        int deck = blueprint.getPorchDeck();
        int floor = deck * EngineSetting.SHIP_DECK_HEIGHT;
        int stern = findStern(blueprint, deck);

        if (stern == EngineSetting.INDEX_NOT_FOUND) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_PORCH);
            return;
        }

        ShipOctantGridStruct grid = layout.getGrid();
        int sternX = layout.getOriginX() + stern;

        for (int z = 0; z < EngineSetting.SHIP_SHEET_BREADTH; z++)
            if (blueprint.isOnDeck(deck, stern, z))
                for (int depth = 1; depth <= EngineSetting.SHIP_PORCH_DEPTH; depth++)
                    grid.set(sternX - depth, floor, z, ShipPiece.PORCH);

        for (int z = 0; z < EngineSetting.SHIP_SHEET_BREADTH; z++)
            for (int x = sternX - EngineSetting.SHIP_PORCH_DEPTH; x < sternX; x++)
                if (grid.get(x, floor, z) == ShipPiece.PORCH && isPorchEdge(grid, x, floor, z, sternX))
                    for (int rise = 1; rise <= EngineSetting.SHIP_PORCH_RAIL_HEIGHT; rise++)
                        grid.setIfEmpty(x, floor + rise, z, ShipPiece.RAILS);
    }

    private boolean isPorchEdge(ShipOctantGridStruct grid, int x, int y, int z, int sternX) {
        return isOffPorch(grid, x - 1, y, z, sternX) || isOffPorch(grid, x, y, z + 1, sternX)
                || isOffPorch(grid, x, y, z - 1, sternX);
    }

    private boolean isOffPorch(ShipOctantGridStruct grid, int x, int y, int z, int sternX) {
        return x < sternX && grid.get(x, y, z) != ShipPiece.PORCH;
    }

    // The aftmost octant along the sheet a deck is drawn at, INDEX_NOT_FOUND when it is drawn at none
    private int findStern(ShipBlueprintStruct blueprint, int deck) {

        for (int x = 0; x < EngineSetting.SHIP_SHEET_LENGTH; x++)
            if (blueprint.getOutline(deck, x) > 0)
                return x;

        return EngineSetting.INDEX_NOT_FOUND;
    }

    // Walls \\

    void layWalls(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        layWallList(layout, blueprint.getWalls(), false);
        layWallList(layout, blueprint.getHalfWalls(), true);
    }

    private void layWallList(ShipLayoutStruct layout, IntArrayList walls, boolean half) {

        for (int i = 0; i < walls.size(); i++) {

            int cell = walls.getInt(i);

            layWall(
                    layout,
                    ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT,
                    ShipCellUtility.unpackX(cell),
                    ShipCellUtility.unpackZ(cell),
                    half);
        }
    }

    // A wall rises from its floor to the floor above it under cover, or a deckhouse high in the open; a half wall
    // stands waist high either way
    private void layWall(ShipLayoutStruct layout, int floor, int x, int z, boolean half) {

        ShipOctantGridStruct grid = layout.getGrid();
        int gridX = layout.getOriginX() + x;

        if (!isFloor(grid, gridX, floor, z)) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_WALL_FLOOR);
            return;
        }

        boolean covered = layout.isVolume(x, floor + 1, z);
        int height = half ? EngineSetting.SHIP_HALF_WALL_HEIGHT
                : covered ? EngineSetting.SHIP_DECK_HEIGHT - 1 : EngineSetting.SHIP_DECKHOUSE_HEIGHT;

        for (int y = floor + 1; y <= floor + height; y++) {

            if (!grid.isEmpty(gridX, y, z))
                return;

            grid.set(gridX, y, z, ShipPiece.WALLS);
        }
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

        int level = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int spin = ShipCellUtility.unpackSpin(cell);
        int footX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int footZ = ShipCellUtility.unpackZ(cell);
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);

        if (!isFloor(layout.getGrid(), footX, level, footZ)) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_FLOOR);
            return;
        }

        int rise = findStairRise(layout.getGrid(), footX, footZ, stepX, stepZ, level);

        if (rise == EngineSetting.SHIP_LEVEL_NONE) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_NO_FLOOR);
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

    // The bulwark along a deck's edge opened where a stair climbs onto it
    private void clearLanding(ShipOctantGridStruct grid, int x, int floor, int z) {

        for (int rise = 1; rise <= EngineSetting.SHIP_BULWARK_HEIGHT; rise++)
            if (grid.get(x, floor + rise, z) == ShipPiece.BULWARK)
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

            ShipPiece piece = grid.get(x, y, z);

            if (!grid.contains(x, y, z) || piece == ShipPiece.HULL || piece == ShipPiece.BULWARK) {
                layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_HULL);
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

    // The highest layer the hull reaches at an octant along the sheet and across it, SHIP_LEVEL_NONE when none
    int findColumnTop(ShipLayoutStruct layout, int x, int z) {

        for (int y = layout.getSectionLayers() - 1; y >= 0; y--)
            if (layout.isVolume(x, y, z))
                return y;

        return EngineSetting.SHIP_LEVEL_NONE;
    }

    // The lowest layer the hull reaches at an octant along the sheet and across it, SHIP_LEVEL_NONE when none
    int findKeel(ShipLayoutStruct layout, int x, int z) {

        for (int y = 0; y < layout.getSectionLayers(); y++)
            if (layout.isVolume(x, y, z))
                return y;

        return EngineSetting.SHIP_LEVEL_NONE;
    }

    // How many octants the hull reaches out from the centre line at an octant along the sheet, at any layer
    int findWidestSection(ShipLayoutStruct layout, int x) {

        int widest = 0;

        for (int y = 0; y < layout.getSectionLayers(); y++)
            widest = Math.max(widest, layout.getSection(x, y));

        return widest;
    }
}
