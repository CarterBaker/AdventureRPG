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
     * railed. The waist is cut out of a deck's floor wherever it is drawn,
     * over open room below, and railed round wherever the floor left
     * standing looks down into it, so a deck cut through its middle is left
     * as gangways either side over the deck below, open to the sky; cut
     * through several decks, the pit runs as deep. A wall rises from its
     * deck to the deck above it under cover, or
     * a deckhouse high in the open; a half wall stands waist high. A stair
     * climbs one octant per step along the way it faces, two octants wide,
     * until it meets a floor level with its last tread, and opens the floor
     * over its run and over the floor before its foot, so a climber's head
     * clears the deck above from the first step. A ladder stands on two by
     * two octants of floor and climbs to the floor right over it, opening a
     * hatchway there, or in the open up the face of the raised deck it faces.
     * A hatch lies in a floor over a deck: set on an opening a stair or
     * ladder climbs through it closes the whole of it, anywhere else it cuts
     * its own. Every query about where floors lie goes through here.
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

    // Waist \\

    // The waist cut out of every deck's floor where it is drawn, then railed round wherever a floor left standing
    // looks down into it
    void layWaists(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList waists = blueprint.getWaists();

        for (int i = 0; i < waists.size(); i++)
            cutWaist(layout, waists.getInt(i));

        for (int i = 0; i < waists.size(); i++)
            railWaist(layout, waists.getInt(i));
    }

    // One octant of a deck's floor inside the hull cut away over open room below; never the bottom deck, the side or
    // anything built on it
    private void cutWaist(ShipLayoutStruct layout, int cell) {

        ShipOctantGridStruct grid = layout.getGrid();
        int deck = ShipCellUtility.unpackLevel(cell);
        int floor = deck * EngineSetting.SHIP_DECK_HEIGHT;
        int x = ShipCellUtility.unpackX(cell);
        int gridX = layout.getOriginX() + x;
        int z = ShipCellUtility.unpackZ(cell);

        if (deck == 0) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_WAIST_BOTTOM);
            return;
        }

        if (!layout.isVolume(x, floor, z) || !isFloor(grid, gridX, floor, z) || !grid.isEmpty(gridX, floor + 1, z)
                || !grid.isEmpty(gridX, floor - 1, z)) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_WAIST_FLOOR);
            return;
        }

        grid.clear(gridX, floor, z);
    }

    // A rail waist high on every floor octant beside a cut one
    private void railWaist(ShipLayoutStruct layout, int cell) {

        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int gridX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell);

        for (int spin = 0; spin < EngineSetting.ENCODED_FACE_SPIN_COUNT; spin++)
            railRim(layout.getGrid(), gridX + ShipCellUtility.stepX(spin), floor, z + ShipCellUtility.stepZ(spin));
    }

    private void railRim(ShipOctantGridStruct grid, int x, int floor, int z) {

        if (!isFloor(grid, x, floor, z))
            return;

        for (int rise = 1; rise <= EngineSetting.SHIP_WAIST_RAIL_HEIGHT; rise++)
            grid.setIfEmpty(x, floor + rise, z, ShipPiece.RAILS);
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

        for (int across = 0; across < EngineSetting.SHIP_STAIR_WIDTH; across++) {

            clearLanding(
                    layout.getGrid(),
                    footX + stepX * rise + acrossX * across,
                    level + rise,
                    footZ + stepZ * rise + acrossZ * across);

            for (int back = 1; back <= EngineSetting.SHIP_STAIR_HEADROOM; back++)
                clearHeadroom(
                        layout.getGrid(),
                        footX - stepX * back + acrossX * across,
                        level + rise,
                        footZ - stepZ * back + acrossZ * across);
        }
    }

    // The floor over the approach to a stair's foot opened, so a climber's head clears it from the first step; a
    // floor in the open over nothing is left
    private void clearHeadroom(ShipOctantGridStruct grid, int x, int floor, int z) {

        if (isFloor(grid, x, floor, z) && grid.isEmpty(x, floor - 1, z))
            grid.clear(x, floor, z);
    }

    // The bulwark along a deck's edge, or the rail round a porch or the waist, opened where a stair or ladder climbs
    // onto it
    private void clearLanding(ShipOctantGridStruct grid, int x, int floor, int z) {

        for (int rise = 1; rise <= EngineSetting.SHIP_BULWARK_HEIGHT; rise++) {

            ShipPiece piece = grid.get(x, floor + rise, z);

            if (piece == ShipPiece.BULWARK || piece == ShipPiece.RAILS)
                grid.clear(x, floor + rise, z);
        }
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

    // Ladders \\

    void layLadders(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList ladders = blueprint.getLadders();

        for (int i = 0; i < ladders.size(); i++)
            layLadder(layout, ladders.getInt(i));
    }

    // Up from two by two octants of open floor to the floor right over them, a hatchway opened there, or in the open
    // up the face of the raised deck the ladder faces, its bulwark opened where the climber steps onto it
    private void layLadder(ShipLayoutStruct layout, int cell) {

        ShipOctantGridStruct grid = layout.getGrid();
        int size = EngineSetting.SHIP_HATCH_OCTANTS;
        int level = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int spin = ShipCellUtility.unpackSpin(cell);
        int minX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int minZ = ShipCellUtility.unpackZ(cell);

        if (!isOpenFloor(grid, minX, level, minZ, size, size)) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_LADDER_FLOOR);
            return;
        }

        int top = findLadderHead(grid, minX, level, minZ, size);

        if (top == EngineSetting.SHIP_LEVEL_NONE) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_LADDER_BLOCKED);
            return;
        }

        if (top != EngineSetting.SHIP_DECK_NONE) {

            clearRect(grid, minX, top, minZ, size, size);
            layout.addLadder(minX, level + 1, minZ, minX + size, top + 1 + EngineSetting.SHIP_LADDER_HANDHOLD,
                    minZ + size, spin);
            return;
        }

        int rise = findFaceRise(grid, minX, level, minZ, size, spin);

        if (rise == EngineSetting.SHIP_LEVEL_NONE) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_LADDER_NO_FLOOR);
            return;
        }

        for (int along = 0; along < size; along++)
            clearLanding(grid, resolveBeyondX(minX, size, spin, along), level + rise,
                    resolveBeyondZ(minZ, size, spin, along));

        layout.addLadder(minX, level + 1, minZ, minX + size, level + rise + 1 + EngineSetting.SHIP_LADDER_HANDHOLD,
                minZ + size, spin);
    }

    // The floor right over a ladder's footprint: its layer once every octant of it is floor, SHIP_DECK_NONE when
    // nothing stands over it within the tallest climb, SHIP_LEVEL_NONE when anything else is in the way
    private int findLadderHead(ShipOctantGridStruct grid, int minX, int level, int minZ, int size) {

        for (int y = level + 1; y <= level + EngineSetting.SHIP_STAIR_MAX_RISE; y++) {

            int floors = 0;
            int built = 0;

            for (int z = minZ; z < minZ + size; z++)
                for (int x = minX; x < minX + size; x++) {

                    if (!grid.isEmpty(x, y, z))
                        built++;

                    if (isFloor(grid, x, y, z))
                        floors++;
                }

            if (built == 0)
                continue;

            return floors == size * size ? y : EngineSetting.SHIP_LEVEL_NONE;
        }

        return EngineSetting.SHIP_DECK_NONE;
    }

    // The octants a ladder in the open climbs: the first rise at which the floor just beyond the edge it faces lies,
    // SHIP_LEVEL_NONE when none does within the tallest climb
    private int findFaceRise(ShipOctantGridStruct grid, int minX, int level, int minZ, int size, int spin) {

        for (int rise = 1; rise <= EngineSetting.SHIP_STAIR_MAX_RISE; rise++) {

            boolean landing = true;

            for (int along = 0; along < size && landing; along++)
                landing = isFloor(grid, resolveBeyondX(minX, size, spin, along), level + rise,
                        resolveBeyondZ(minZ, size, spin, along));

            if (landing)
                return rise;
        }

        return EngineSetting.SHIP_LEVEL_NONE;
    }

    // One octant of the row just beyond the edge of a square footprint that a quarter turn faces
    private int resolveBeyondX(int minX, int size, int spin, int along) {

        int stepX = ShipCellUtility.stepX(spin);

        if (stepX > 0)
            return minX + size;

        return stepX < 0 ? minX - 1 : minX + along;
    }

    private int resolveBeyondZ(int minZ, int size, int spin, int along) {

        int stepZ = ShipCellUtility.stepZ(spin);

        if (stepZ > 0)
            return minZ + size;

        return stepZ < 0 ? minZ - 1 : minZ + along;
    }

    // Hatches \\

    void layHatches(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList hatches = blueprint.getHatches();

        for (int i = 0; i < hatches.size(); i++)
            layHatch(layout, hatches.getInt(i));
    }

    // A lid over the opening a hatch is set on, or over two by two octants of floor cut for it, over a deck below
    private void layHatch(ShipLayoutStruct layout, int cell) {

        ShipOctantGridStruct grid = layout.getGrid();
        int deck = ShipCellUtility.unpackLevel(cell);
        int floor = deck * EngineSetting.SHIP_DECK_HEIGHT;
        int x = ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell);
        int gridX = layout.getOriginX() + x;
        int spin = ShipCellUtility.unpackSpin(cell);
        int size = EngineSetting.SHIP_HATCH_OCTANTS;

        if (deck == 0) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_HATCH_BOTTOM);
            return;
        }

        if (isOpening(layout, x, floor, z)) {
            layOpeningLid(layout, x, floor, z, spin);
            return;
        }

        if (!isOpenFloor(grid, gridX, floor, z, size, size) || !isClearBelow(grid, gridX, floor, z, size)) {
            layout.reportBuildProblem(EngineSetting.SHIPWRIGHT_PROBLEM_HATCH_FLOOR);
            return;
        }

        clearRect(grid, gridX, floor, z, size, size);
        layout.addHatch(gridX, floor, z, gridX + size, z + size, spin);
    }

    // The whole opening a hatch is set on closed by its lid: the run of opening along the length through the cell,
    // widened across while every octant of that run stays open, no larger than the largest lid
    private void layOpeningLid(ShipLayoutStruct layout, int x, int floor, int z, int spin) {

        int limit = EngineSetting.SHIP_HATCH_MAX_OCTANTS;
        int minX = x;
        int maxX = x + 1;

        while (maxX - minX < limit && isOpening(layout, minX - 1, floor, z))
            minX--;

        while (maxX - minX < limit && isOpening(layout, maxX, floor, z))
            maxX++;

        int minZ = z;
        int maxZ = z + 1;

        while (maxZ - minZ < limit && isOpeningRun(layout, minX, maxX, floor, minZ - 1))
            minZ--;

        while (maxZ - minZ < limit && isOpeningRun(layout, minX, maxX, floor, maxZ))
            maxZ++;

        layout.addHatch(layout.getOriginX() + minX, floor, minZ, layout.getOriginX() + maxX, maxZ, spin);
    }

    private boolean isOpeningRun(ShipLayoutStruct layout, int minX, int maxX, int floor, int z) {

        for (int x = minX; x < maxX; x++)
            if (!isOpening(layout, x, floor, z))
                return false;

        return true;
    }

    // True where a deck's floor lies open inside the hull, as a stair or ladder leaves it
    private boolean isOpening(ShipLayoutStruct layout, int x, int floor, int z) {
        return layout.isVolume(x, floor, z) && layout.getGrid().isEmpty(layout.getOriginX() + x, floor, z);
    }

    // True when nothing is built in the octants right under a square of floor, so a hatch opens onto a deck
    private boolean isClearBelow(ShipOctantGridStruct grid, int minX, int floor, int minZ, int size) {

        for (int z = minZ; z < minZ + size; z++)
            for (int x = minX; x < minX + size; x++)
                if (!grid.isEmpty(x, floor - 1, z))
                    return false;

        return true;
    }

    // Queries \\

    // True when every octant of a rectangle is floor with nothing standing on it
    private boolean isOpenFloor(ShipOctantGridStruct grid, int minX, int floor, int minZ, int sizeX, int sizeZ) {

        for (int z = minZ; z < minZ + sizeZ; z++)
            for (int x = minX; x < minX + sizeX; x++)
                if (!isFloor(grid, x, floor, z) || !grid.isEmpty(x, floor + 1, z))
                    return false;

        return true;
    }

    private void clearRect(ShipOctantGridStruct grid, int minX, int y, int minZ, int sizeX, int sizeZ) {

        for (int z = minZ; z < minZ + sizeZ; z++)
            for (int x = minX; x < minX + sizeX; x++)
                grid.clear(x, y, z);
    }

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
