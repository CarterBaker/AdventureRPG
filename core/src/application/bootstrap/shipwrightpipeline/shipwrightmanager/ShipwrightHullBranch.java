package application.bootstrap.shipwrightpipeline.shipwrightmanager;

import application.bootstrap.shipwrightpipeline.shipwright.ShipBlueprintStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipLayoutStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOctantGridStruct;
import application.bootstrap.shipwrightpipeline.shipwright.ShipOpening;
import application.bootstrap.shipwrightpipeline.shipwright.ShipPiece;
import application.bootstrap.shipwrightpipeline.shipwright.ShipWall;
import application.bootstrap.shipwrightpipeline.util.ShipCellUtility;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class ShipwrightHullBranch extends BranchPackage {

    /*
     * Lays a blueprint's hull and everything built into it into the octant
     * grid. Every deck's floor lies a deck's height over the one below; the
     * hull's side between two decks is blended layer by layer from the
     * outline of the deck below to the outline of the deck above, so decks
     * drawn wider flare the side out and decks drawn a little narrower draw
     * it in, while a deck drawn more than SHIP_TUMBLEHOME_OCTANTS inside the
     * one below rises from it with straight sides, a raised deck standing on
     * the open floor round it; a deck drawn past the end of the one below
     * rakes its stem or its stern up to it. Wherever no deck lies above, the deck below is open to the sky.
     * The hull is shelled one octant thick wherever it meets the outside, its
     * open decks capping it, so the hollow inside is sealed and every raised
     * deck's open end is closed by a bulkhead; interior decks floor the inside
     * at their height. Every open deck is ringed by a bulwark where it looks
     * out over nothing, the lowest of them the sill the sea comes aboard over.
     * The waist is cut out of a deck's floor wherever it is drawn,
     * over open room below, and railed round wherever the floor left
     * standing looks down into it, so a deck cut through its middle is left
     * as gangways either side over the deck below, open to the sky; cut
     * through several decks, the pit runs as deep, and cut through the bottom
     * deck it opens the hull to the sea below. A wall rises from its deck to
     * the deck above it under cover, or a deckhouse high in the open, taking
     * down the waist's rail or the bulwark that stood there; a half wall
     * stands waist high. A railing stands rail high on open floor, or in
     * place of the ship's own side up to the deck above, or of a bulwark, at
     * a deck's edge, lowering the sill to its deck where it opens the side,
     * so a porch or a gallery is a deck railed round instead of walled; a lip
     * does the same with a low sill stepped over, so the side opens as a way
     * in. Every rail is built as a post up each octant under a handrail run
     * out to the rails beside it. A stair
     * always climbs one deck: an octant per step along the way it faces, two
     * octants wide, onto the floor a deck's height up just past its last
     * tread, and opens the floor over its run and over the floor before its
     * foot, so a climber's head clears the deck above from the first step. A
     * ladder stands on two by two octants of floor and climbs to the floor
     * right over it, opening a hatchway there, or in the open up the face of
     * the raised deck it faces. A boarding ladder hangs from the edge of an
     * open deck down the outside of the hull to its keel, clear of the widest
     * of the side below, its bulwark opened where it comes aboard. A hatch
     * lies in a floor over a deck: set on an opening a stair or ladder climbs
     * through it closes the whole of it, anywhere else it cuts its own. A
     * grate or a glass floor lies fixed over the whole of the opening it is
     * set on, the waist's rail along it taken down. Every
     * placement that can be refused says why through its find...Problem
     * query, which the table asks before it sets anything down and the
     * compile asks again before laying it. Every query about where floors lie
     * goes through here.
     */

    // Sections \\

    // The hull's half-beam at every layer and octant along the sheet, each layer between two decks blended from the
    // outline of the deck below to the outline of the deck above, or the deck above's own where it is drawn well inside
    // the one below, nothing over a deck with none above it
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

        if (below - above > EngineSetting.SHIP_TUMBLEHOME_OCTANTS)
            return above;

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

    // One octant of a deck's floor inside the hull cut away over open room below, or through the bottom to the sea;
    // never the side or anything built on it
    private void cutWaist(ShipLayoutStruct layout, int cell) {

        ShipOctantGridStruct grid = layout.getGrid();
        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int x = ShipCellUtility.unpackX(cell);
        int gridX = layout.getOriginX() + x;
        int z = ShipCellUtility.unpackZ(cell);

        if (!layout.isVolume(x, floor, z) || !isFloor(grid, gridX, floor, z) || !grid.isEmpty(gridX, floor + 1, z)
                || !isOpenBelow(layout, x, floor, z)) {
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

        layWallList(layout, blueprint.getWalls(), ShipWall.FULL);
        layWallList(layout, blueprint.getHalfWalls(), ShipWall.HALF);
        layWallList(layout, blueprint.getRailings(), ShipWall.RAILING);
        layWallList(layout, blueprint.getLips(), ShipWall.LIP);
    }

    private void layWallList(ShipLayoutStruct layout, IntArrayList walls, ShipWall wall) {

        for (int i = 0; i < walls.size(); i++) {

            int cell = walls.getInt(i);

            layWall(
                    layout,
                    ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT,
                    ShipCellUtility.unpackX(cell),
                    ShipCellUtility.unpackZ(cell),
                    wall);
        }
    }

    // A wall of any kind over a deck's octant: on open floor in place of the waist's rail or a bulwark standing there,
    // or in place of the ship's side, which is opened up to the deck above and out through all its planking however
    // far the side flares; a full wall rises to the deck above under cover or a deckhouse high in the open, every
    // other kind its own height. Opening the side or a bulwark lowers the sill to the deck, so one the sea reaches is
    // a shape problem
    private void layWall(ShipLayoutStruct layout, int floor, int x, int z, ShipWall wall) {

        int deck = floor / EngineSetting.SHIP_DECK_HEIGHT;
        String problem = findWallProblem(layout, deck, x, z, wall, false);

        if (!problem.isEmpty()) {
            layout.reportBuildProblem(problem);
            return;
        }

        ShipOctantGridStruct grid = layout.getGrid();
        int gridX = layout.getOriginX() + x;

        if (opensSide(layout, deck, x, z, wall))
            layout.lowerOpeningSill((floor + 1) * EngineSetting.SUB_BLOCK_SIZE);

        if (isSide(layout, floor, x, z))
            openSide(grid, gridX, floor, z);
        else
            clearLanding(grid, gridX, floor, z);

        int height = wall != ShipWall.FULL || layout.isVolume(x, floor + 1, z)
                ? wall.getHeight()
                : EngineSetting.SHIP_DECKHOUSE_HEIGHT;

        for (int y = floor + 1; y <= floor + height; y++) {

            if (!grid.isEmpty(gridX, y, z))
                return;

            grid.set(gridX, y, z, wall.getPiece());
        }
    }

    // Why a wall of a kind cannot stand over a deck's octant, SHIP_PROBLEM_NONE when it can: on open floor with nothing
    // on it but the waist's rail or a bulwark, or in place of the ship's side, which every kind but a full wall takes
    // down; one that opens the side or a bulwark only above the waterline the ship last floated at. A wall already
    // painted there, which this one replaces, is passed over
    String findWallProblem(ShipLayoutStruct layout, int deck, int x, int z, ShipWall wall, boolean replacing) {

        ShipOctantGridStruct grid = layout.getGrid();
        int floor = deck * EngineSetting.SHIP_DECK_HEIGHT;
        int gridX = layout.getOriginX() + x;
        ShipPiece standing = grid.get(gridX, floor + 1, z);

        if (isSide(layout, floor, x, z)) {

            if (wall == ShipWall.FULL)
                return EngineSetting.SHIPWRIGHT_PROBLEM_WALL_SIDE;

        } else if (!isFloor(grid, gridX, floor, z) || !replacing && standing != null && standing != ShipPiece.RAILS
                && standing != ShipPiece.BULWARK)
            return EngineSetting.SHIPWRIGHT_PROBLEM_WALL_FLOOR;

        if (opensSide(layout, deck, x, z, wall) && layout.isAfloat()
                && (floor + 1) * EngineSetting.SUB_BLOCK_SIZE <= layout.getDraft())
            return EngineSetting.SHIPWRIGHT_PROBLEM_RAILING_WATER;

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    // True where a deck's floor meets the ship's side, its planking standing over it
    private boolean isSide(ShipLayoutStruct layout, int floor, int x, int z) {
        return layout.isVolume(x, floor, z)
                && layout.getGrid().get(layout.getOriginX() + x, floor, z) == ShipPiece.HULL;
    }

    // True when a wall over an octant would open the ship to the sea there: any kind but a full wall in place of the
    // side, or a railing or lip in place of a bulwark
    private boolean opensSide(ShipLayoutStruct layout, int deck, int x, int z, ShipWall wall) {

        int floor = deck * EngineSetting.SHIP_DECK_HEIGHT;

        if (isSide(layout, floor, x, z))
            return wall != ShipWall.FULL;

        return wall.isEdge()
                && layout.getGrid().get(layout.getOriginX() + x, floor + 1, z) == ShipPiece.BULWARK;
    }

    // The side over a deck's edge octant opened up to the deck above: at every layer the planking, bulwark or rail
    // there and outboard of it cleared, walking out from the centre line as far as the side flares, so the hole goes
    // right through
    private void openSide(ShipOctantGridStruct grid, int gridX, int floor, int z) {

        int step = z >= EngineSetting.SHIP_SHEET_HALF_BREADTH ? 1 : -1;

        for (int y = floor + 1; y < floor + EngineSetting.SHIP_DECK_HEIGHT; y++)
            clearOutboard(grid, gridX, y, z, 0, step);
    }

    // The planking, bulwark or rail met walking outboard from an octant along a step cleared, the empty octants inside
    // a flared side passed over until it is reached; true when any was
    private boolean clearOutboard(ShipOctantGridStruct grid, int x, int y, int z, int stepX, int stepZ) {

        boolean cut = false;

        for (int reach = 0; reach <= EngineSetting.SHIP_OPENING_REACH; reach++) {

            int atX = x + stepX * reach;
            int atZ = z + stepZ * reach;
            ShipPiece piece = grid.get(atX, y, atZ);

            if (piece == ShipPiece.HULL || piece == ShipPiece.BULWARK || piece == ShipPiece.RAILS) {
                grid.clear(atX, y, atZ);
                cut = true;
                continue;
            }

            if (piece != null || cut)
                break;
        }

        return cut;
    }

    // Stairs \\

    void layStairs(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList stairs = blueprint.getStairs();

        for (int i = 0; i < stairs.size(); i++)
            layStair(layout, stairs.getInt(i));
    }

    // One step an octant up per octant along the way it faces, a deck's height in all, onto the floor just past its
    // last tread — a deck over the run, opened above every step, or a raised deck the run climbs onto
    private void layStair(ShipLayoutStruct layout, int cell) {

        String problem = findStairProblem(layout, cell);

        if (!problem.isEmpty()) {
            layout.reportBuildProblem(problem);
            return;
        }

        ShipOctantGridStruct grid = layout.getGrid();
        int rise = EngineSetting.SHIP_DECK_HEIGHT;
        int level = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int spin = ShipCellUtility.unpackSpin(cell);
        int footX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int footZ = ShipCellUtility.unpackZ(cell);
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);
        int acrossX = stepX == 0 ? 1 : 0;
        int acrossZ = stepZ == 0 ? 1 : 0;

        for (int step = 0; step < rise; step++)
            for (int across = 0; across < EngineSetting.SHIP_STAIR_WIDTH; across++)
                layStep(grid, footX + stepX * step + acrossX * across, footZ + stepZ * step + acrossZ * across,
                        level + 1, level + 1 + step, level + rise);

        for (int across = 0; across < EngineSetting.SHIP_STAIR_WIDTH; across++) {

            clearLanding(grid, footX + stepX * rise + acrossX * across, level + rise,
                    footZ + stepZ * rise + acrossZ * across);

            for (int back = 1; back <= EngineSetting.SHIP_STAIR_HEADROOM; back++)
                clearHeadroom(layout, footX - stepX * back + acrossX * across, level + rise,
                        footZ - stepZ * back + acrossZ * across);
        }
    }

    // Why a stair cannot stand with its foot at a cell, SHIP_PROBLEM_NONE when it can: its foot on floor, its run
    // clear of the hull, its bulwarks and anything built under the deck above, and a floor a deck's height up just
    // past its last tread to land on
    String findStairProblem(ShipLayoutStruct layout, int cell) {

        ShipOctantGridStruct grid = layout.getGrid();
        int rise = EngineSetting.SHIP_DECK_HEIGHT;
        int level = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int spin = ShipCellUtility.unpackSpin(cell);
        int footX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int footZ = ShipCellUtility.unpackZ(cell);
        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);
        int acrossX = stepX == 0 ? 1 : 0;
        int acrossZ = stepZ == 0 ? 1 : 0;

        for (int across = 0; across < EngineSetting.SHIP_STAIR_WIDTH; across++) {

            int x = footX + acrossX * across;
            int z = footZ + acrossZ * across;

            if (!isFloor(grid, x, level, z))
                return EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_FLOOR;

            for (int step = 0; step < rise; step++)
                for (int y = level + 1; y <= level + rise; y++) {

                    ShipPiece piece = grid.get(x + stepX * step, y, z + stepZ * step);

                    if (!grid.contains(x + stepX * step, y, z + stepZ * step)
                            || piece == ShipPiece.HULL || piece == ShipPiece.BULWARK)
                        return EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_HULL;

                    if (piece != null && y < level + rise)
                        return EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_BLOCKED;
                }

            if (!isFloor(grid, x + stepX * rise, level + rise, z + stepZ * rise))
                return EngineSetting.SHIPWRIGHT_PROBLEM_STAIRS_NO_FLOOR;
        }

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    // The floor over the approach to a stair's foot opened, so a climber's head clears it from the first step; a
    // floor in the open over nothing is left
    private void clearHeadroom(ShipLayoutStruct layout, int x, int floor, int z) {

        if (isFloor(layout.getGrid(), x, floor, z) && isOpenBelow(layout, x - layout.getOriginX(), floor, z))
            layout.getGrid().clear(x, floor, z);
    }

    // The bulwark along a deck's edge, or a rail or railing, opened where a stair or ladder climbs onto it or a wall
    // replaces it
    private void clearLanding(ShipOctantGridStruct grid, int x, int floor, int z) {

        for (int rise = 1; rise <= EngineSetting.SHIP_BULWARK_HEIGHT; rise++) {

            ShipPiece piece = grid.get(x, floor + rise, z);

            if (piece == ShipPiece.BULWARK || piece == ShipPiece.RAILS)
                grid.clear(x, floor + rise, z);
        }
    }

    // A step's riser filled up to its tread and the floors over it opened
    private void layStep(ShipOctantGridStruct grid, int x, int z, int bottom, int tread, int top) {

        for (int y = bottom; y <= top; y++) {

            if (y <= tread)
                grid.set(x, y, z, ShipPiece.STAIRS);
            else
                grid.clear(x, y, z);
        }
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

        String problem = findLadderProblem(layout, cell);

        if (!problem.isEmpty()) {
            layout.reportBuildProblem(problem);
            return;
        }

        ShipOctantGridStruct grid = layout.getGrid();
        int size = EngineSetting.SHIP_HATCH_OCTANTS;
        int level = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int spin = ShipCellUtility.unpackSpin(cell);
        int minX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int minZ = ShipCellUtility.unpackZ(cell);
        int top = findLadderHead(grid, minX, level, minZ, size);

        if (top != EngineSetting.SHIP_DECK_NONE) {

            clearRect(grid, minX, top, minZ, size, size);
            layout.addLadder(minX, level + 1, minZ, minX + size, top + 1 + EngineSetting.SHIP_LADDER_HANDHOLD,
                    minZ + size, spin);
            return;
        }

        int rise = findFaceRise(grid, minX, level, minZ, size, spin);

        for (int along = 0; along < size; along++)
            clearLanding(grid, resolveBeyondX(minX, size, spin, along), level + rise,
                    resolveBeyondZ(minZ, size, spin, along));

        layout.addLadder(minX, level + 1, minZ, minX + size, level + rise + 1 + EngineSetting.SHIP_LADDER_HANDHOLD,
                minZ + size, spin);
    }

    // Why a ladder cannot stand at a cell, SHIP_PROBLEM_NONE when it can: on open floor, with a floor right over it
    // or, in the open, the face of a raised deck before it to climb to
    String findLadderProblem(ShipLayoutStruct layout, int cell) {

        ShipOctantGridStruct grid = layout.getGrid();
        int size = EngineSetting.SHIP_HATCH_OCTANTS;
        int level = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int minX = layout.getOriginX() + ShipCellUtility.unpackX(cell);
        int minZ = ShipCellUtility.unpackZ(cell);

        if (!isOpenFloor(grid, minX, level, minZ, size, size))
            return EngineSetting.SHIPWRIGHT_PROBLEM_LADDER_FLOOR;

        int top = findLadderHead(grid, minX, level, minZ, size);

        if (top == EngineSetting.SHIP_LEVEL_NONE)
            return EngineSetting.SHIPWRIGHT_PROBLEM_LADDER_BLOCKED;

        if (top == EngineSetting.SHIP_DECK_NONE && findFaceRise(grid, minX, level, minZ, size,
                ShipCellUtility.unpackSpin(cell)) == EngineSetting.SHIP_LEVEL_NONE)
            return EngineSetting.SHIPWRIGHT_PROBLEM_LADDER_NO_FLOOR;

        return EngineSetting.SHIP_PROBLEM_NONE;
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

    // Boarding Ladders \\

    void layBoardingLadders(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList boardingLadders = blueprint.getBoardingLadders();

        for (int i = 0; i < boardingLadders.size(); i++)
            layBoardingLadder(layout, boardingLadders.getInt(i));
    }

    // A rope ladder hung from the edge of an open deck down the side to the keel, kept as the octants from the deck's
    // edge out to the widest the side reaches below it, so it hangs clear of the hull all the way down; the bulwark is
    // opened where it comes aboard
    private void layBoardingLadder(ShipLayoutStruct layout, int cell) {

        String problem = findBoardingProblem(layout, cell);

        if (!problem.isEmpty()) {
            layout.reportBuildProblem(problem);
            return;
        }

        ShipOctantGridStruct grid = layout.getGrid();
        int width = EngineSetting.SHIP_STAIR_WIDTH;
        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int x = ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell);
        int gridX = layout.getOriginX() + x;
        boolean starboard = z >= EngineSetting.SHIP_SHEET_HALF_BREADTH;
        int keel = floor;
        int widest = ShipCellUtility.toHalfBreadth(z) + 1;

        for (int along = 0; along < width; along++) {

            clearLanding(grid, gridX + along, floor, z);
            keel = Math.min(keel, findKeel(layout, x + along, EngineSetting.SHIP_SHEET_HALF_BREADTH));

            for (int y = 0; y <= floor; y++)
                widest = Math.max(widest, layout.getSection(x + along, y));
        }

        int outer = starboard ? ShipCellUtility.toStarboard(widest - 1) : ShipCellUtility.toPort(widest - 1);

        layout.addBoardingLadder(gridX, keel, Math.min(z, outer), gridX + width,
                floor + 1 + EngineSetting.SHIP_LADDER_HANDHOLD, Math.max(z, outer) + 1,
                starboard ? 1 : EngineSetting.ENCODED_FACE_SPIN_COUNT - 1);
    }

    // Why a boarding ladder cannot hang at a cell, SHIP_PROBLEM_NONE when it can: both its octants the outermost of an
    // open deck's floor
    String findBoardingProblem(ShipLayoutStruct layout, int cell) {

        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int x = ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell);
        int halfZ = ShipCellUtility.toHalfBreadth(z);

        for (int along = 0; along < EngineSetting.SHIP_STAIR_WIDTH; along++)
            if (layout.getSection(x + along, floor) != halfZ + 1
                    || layout.getGrid().get(layout.getOriginX() + x + along, floor, z) != ShipPiece.WEATHER_DECK)
                return EngineSetting.SHIPWRIGHT_PROBLEM_BOARDING_EDGE;

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    // Hatches \\

    void layHatches(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        IntArrayList hatches = blueprint.getHatches();

        for (int i = 0; i < hatches.size(); i++)
            layHatch(layout, hatches.getInt(i));
    }

    // A lid over the opening a hatch is set on, or over two by two octants of floor cut for it, over a deck below
    private void layHatch(ShipLayoutStruct layout, int cell) {

        String problem = findHatchProblem(layout, cell);

        if (!problem.isEmpty()) {
            layout.reportBuildProblem(problem);
            return;
        }

        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;
        int x = ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell);
        int gridX = layout.getOriginX() + x;
        int spin = ShipCellUtility.unpackSpin(cell);
        int size = EngineSetting.SHIP_HATCH_OCTANTS;

        if (isOpening(layout, x, floor, z)) {
            layOpeningCover(layout, x, floor, z, spin, ShipOpening.HATCH);
            return;
        }

        clearRect(layout.getGrid(), gridX, floor, z, size, size);
        layout.addHatch(gridX, floor, z, gridX + size, z + size, spin);
    }

    // Why a hatch cannot lie at a cell, SHIP_PROBLEM_NONE when it can: above the bottom deck, on the opening a stair or
    // ladder leaves or on two by two octants of open floor over open room below
    String findHatchProblem(ShipLayoutStruct layout, int cell) {

        ShipOctantGridStruct grid = layout.getGrid();
        int deck = ShipCellUtility.unpackLevel(cell);
        int floor = deck * EngineSetting.SHIP_DECK_HEIGHT;
        int x = ShipCellUtility.unpackX(cell);
        int z = ShipCellUtility.unpackZ(cell);
        int gridX = layout.getOriginX() + x;
        int size = EngineSetting.SHIP_HATCH_OCTANTS;

        if (deck == 0)
            return EngineSetting.SHIPWRIGHT_PROBLEM_HATCH_BOTTOM;

        if (isOpening(layout, x, floor, z))
            return EngineSetting.SHIP_PROBLEM_NONE;

        if (!isOpenFloor(grid, gridX, floor, z, size, size) || !isClearBelow(layout, x, floor, z, size))
            return EngineSetting.SHIPWRIGHT_PROBLEM_HATCH_FLOOR;

        return EngineSetting.SHIP_PROBLEM_NONE;
    }

    // The whole opening a hatch, grate or glass floor is set on covered by it: the run of opening along the length
    // through the cell, widened across while every octant of that run stays open, no larger than the largest lid or
    // cover
    private void layOpeningCover(ShipLayoutStruct layout, int x, int floor, int z, int spin, ShipOpening opening) {

        int limit = opening.isCover()
                ? EngineSetting.SHIP_GRATE_MAX_OCTANTS
                : EngineSetting.SHIP_HATCH_MAX_OCTANTS;
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

        int gridMinX = layout.getOriginX() + minX;
        int gridMaxX = layout.getOriginX() + maxX;

        if (!opening.isCover()) {
            layout.addHatch(gridMinX, floor, minZ, gridMaxX, maxZ, spin);
            return;
        }

        layout.addCover(opening, gridMinX, floor, minZ, gridMaxX, maxZ, spin);
        unrailCover(layout.getGrid(), gridMinX, floor, minZ, gridMaxX, maxZ);
    }

    // The waist rail standing on the floor along each side of a cover taken down, so the cover is walked onto
    private void unrailCover(ShipOctantGridStruct grid, int minX, int floor, int minZ, int maxX, int maxZ) {

        for (int x = minX; x < maxX; x++) {
            unrailRim(grid, x, floor, minZ - 1);
            unrailRim(grid, x, floor, maxZ);
        }

        for (int z = minZ; z < maxZ; z++) {
            unrailRim(grid, minX - 1, floor, z);
            unrailRim(grid, maxX, floor, z);
        }
    }

    private void unrailRim(ShipOctantGridStruct grid, int x, int floor, int z) {

        if (!isFloor(grid, x, floor, z))
            return;

        for (int rise = 1; rise <= EngineSetting.SHIP_WAIST_RAIL_HEIGHT; rise++)
            if (grid.get(x, floor + rise, z) == ShipPiece.RAILS)
                grid.clear(x, floor + rise, z);
    }

    // Covers \\

    // Every grate, then every glass floor, each over the opening it is set on
    void layCovers(ShipBlueprintStruct blueprint, ShipLayoutStruct layout) {

        for (int kind = 0; kind < ShipOpening.VALUES.length; kind++) {

            ShipOpening opening = ShipOpening.VALUES[kind];

            if (!opening.isCover())
                continue;

            IntArrayList covers = blueprint.getOpenings(opening);

            for (int i = 0; i < covers.size(); i++)
                layCover(layout, covers.getInt(i), opening);
        }
    }

    // A fixed grate or glass floor over the whole opening it is set on, a waist or any other hole in a deck's floor,
    // the bottom deck's waist among them
    private void layCover(ShipLayoutStruct layout, int cell, ShipOpening opening) {

        String problem = findCoverProblem(layout, cell);

        if (!problem.isEmpty()) {
            layout.reportBuildProblem(problem);
            return;
        }

        layOpeningCover(
                layout,
                ShipCellUtility.unpackX(cell),
                ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT,
                ShipCellUtility.unpackZ(cell),
                ShipCellUtility.unpackSpin(cell),
                opening);
    }

    // Why a grate or glass floor cannot lie at a cell, SHIP_PROBLEM_NONE when it can: only over an opening in a deck's
    // floor
    String findCoverProblem(ShipLayoutStruct layout, int cell) {

        int floor = ShipCellUtility.unpackLevel(cell) * EngineSetting.SHIP_DECK_HEIGHT;

        return isOpening(layout, ShipCellUtility.unpackX(cell), floor, ShipCellUtility.unpackZ(cell))
                ? EngineSetting.SHIP_PROBLEM_NONE
                : EngineSetting.SHIPWRIGHT_PROBLEM_GRATE_OPENING;
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

    // True when every octant right under a square of floor is open room inside the hull, so a hatch opens onto a deck
    // and never onto the outside where the side curves in under it
    private boolean isClearBelow(ShipLayoutStruct layout, int minX, int floor, int minZ, int size) {

        for (int z = minZ; z < minZ + size; z++)
            for (int x = minX; x < minX + size; x++)
                if (!isOpenBelow(layout, x, floor, z))
                    return false;

        return true;
    }

    // True when the octant right under a deck's floor is open room inside the hull, or the sea under the bottom deck
    boolean isOpenBelow(ShipLayoutStruct layout, int x, int floor, int z) {
        return floor == 0 || layout.isVolume(x, floor - 1, z)
                && layout.getGrid().isEmpty(layout.getOriginX() + x, floor - 1, z);
    }

    // True when a cover already lies over an octant of a deck's floor along the sheet and across it
    boolean isCovered(ShipLayoutStruct layout, ShipOpening opening, int deck, int x, int z) {

        IntArrayList boxes = layout.getFloorBoxes(opening);
        int floor = deck * EngineSetting.SHIP_DECK_HEIGHT;
        int gridX = layout.getOriginX() + x;

        for (int i = 0; i < ShipLayoutStruct.countFittings(boxes); i++)
            if (ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Y) == floor
                    && gridX >= ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_X)
                    && gridX < ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_X)
                    && z >= ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MIN_Z)
                    && z < ShipLayoutStruct.getFittingBound(boxes, i, EngineSetting.BOX_MAX_Z))
                return true;

        return false;
    }

    // Railings \\

    // Every rail octant built into its part as a balustrade: a post up the middle of each, a handrail across the top
    // of the highest, run out to every rail beside it at that height, so a railing reads as posts under a rail and its
    // runs merge end to end; true when there was any
    boolean addRailings(ShipOctantGridStruct grid, VehiclePartStruct part) {

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int post = EngineSetting.SHIP_RAILING_POST_SUB_VOXELS;
        int cap = EngineSetting.SHIP_RAILING_CAP_SUB_VOXELS;
        int capWidth = EngineSetting.SHIP_RAILING_CAP_WIDTH_SUB_VOXELS;
        int postInset = (octant - post) / 2;
        int capInset = (octant - capWidth) / 2;
        boolean any = false;

        for (int y = 0; y < grid.getSizeY(); y++)
            for (int z = 0; z < grid.getSizeZ(); z++)
                for (int x = 0; x < grid.getSizeX(); x++) {

                    if (grid.get(x, y, z) != ShipPiece.RAILS)
                        continue;

                    int minX = x * octant;
                    int minY = y * octant;
                    int minZ = z * octant;
                    int maxY = minY + octant;
                    boolean top = grid.get(x, y + 1, z) != ShipPiece.RAILS;

                    part.addBox(minX + postInset, minY, minZ + postInset, minX + postInset + post,
                            top ? maxY - cap : maxY, minZ + postInset + post);
                    any = true;

                    if (!top)
                        continue;

                    part.addBox(minX + capInset, maxY - cap, minZ + capInset, minX + capInset + capWidth, maxY,
                            minZ + capInset + capWidth);

                    for (int spin = 0; spin < EngineSetting.ENCODED_FACE_SPIN_COUNT; spin++)
                        addHandrailArm(grid, part, x, y, z, spin, maxY - cap, capInset, capWidth);
                }

        return any;
    }

    // The handrail run from the middle of a rail octant to its side facing a quarter turn, where the rail beside it
    // tops out at the same height
    private void addHandrailArm(
            ShipOctantGridStruct grid,
            VehiclePartStruct part,
            int x,
            int y,
            int z,
            int spin,
            int capY,
            int capInset,
            int capWidth) {

        int stepX = ShipCellUtility.stepX(spin);
        int stepZ = ShipCellUtility.stepZ(spin);

        if (grid.get(x + stepX, y, z + stepZ) != ShipPiece.RAILS
                || grid.get(x + stepX, y + 1, z + stepZ) == ShipPiece.RAILS)
            return;

        int octant = EngineSetting.SHIP_OCTANT_SUB_VOXELS;
        int minX = x * octant;
        int minZ = z * octant;
        int fromX = stepX > 0 ? minX + capInset + capWidth : stepX < 0 ? minX : minX + capInset;
        int toX = stepX > 0 ? minX + octant : stepX < 0 ? minX + capInset : minX + capInset + capWidth;
        int fromZ = stepZ > 0 ? minZ + capInset + capWidth : stepZ < 0 ? minZ : minZ + capInset;
        int toZ = stepZ > 0 ? minZ + octant : stepZ < 0 ? minZ + capInset : minZ + capInset + capWidth;

        part.addBox(fromX, capY, fromZ, toX, capY + EngineSetting.SHIP_RAILING_CAP_SUB_VOXELS, toZ);
    }

    // Queries \\

    // True when every octant of a rectangle is floor with nothing standing on it
    boolean isOpenFloor(ShipOctantGridStruct grid, int minX, int floor, int minZ, int sizeX, int sizeZ) {

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
