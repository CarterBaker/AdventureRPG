package application.bootstrap.worldpipeline.liquidmanager;

import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;

class LiquidBasinBranch extends BranchPackage {

    /*
     * Decides whether slowing water can fill the sealed basin it spreads
     * across. A basin fed by a permanent body joins it; one holding enough
     * volume per cell is levelled and settled; one being poured into holds its
     * rim while filling. Large pooled bodies turn permanent. Each basin
     * resolves at most once per flow pass.
     */

    private static final Direction3Vector[] LATERAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    private static final Direction3Vector[] BODY_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST,
            Direction3Vector.DOWN
    };

    // Internal
    private LiquidManager liquidManager;

    // State
    private boolean layerEnclosed;
    private int layerSourceLevel;
    private boolean blocked;

    // Scratch — basin layer scan
    private LiquidCellStruct[] layerCells;
    private LiquidVisitedStruct layerVisited;
    private LiquidCellStruct neighbor;
    private LiquidCellStruct floor;

    // Scratch — basins already resolved this pass
    private LiquidVisitedStruct passVisited;
    private LiquidVisitedStruct passFed;

    // Scratch — permanence scan
    private LiquidCellStruct[] bodyCells;
    private LiquidVisitedStruct bodyVisited;

    // Base \\

    @Override
    protected void create() {

        // Scratch — basin layer scan
        this.layerCells = createCells(EngineSetting.LIQUID_BASIN_SCAN_LIMIT);
        this.layerVisited = new LiquidVisitedStruct();
        this.neighbor = new LiquidCellStruct();
        this.floor = new LiquidCellStruct();

        // Scratch — basins already resolved this pass
        this.passVisited = new LiquidVisitedStruct();
        this.passFed = new LiquidVisitedStruct();

        // Scratch — permanence scan
        this.bodyCells = createCells(EngineSetting.LIQUID_PERMANENCE_THRESHOLD);
        this.bodyVisited = new LiquidVisitedStruct();
    }

    @Override
    protected void get() {
        this.liquidManager = get(LiquidManager.class);
    }

    private LiquidCellStruct[] createCells(int count) {

        LiquidCellStruct[] cells = new LiquidCellStruct[count];

        for (int i = 0; i < count; i++)
            cells[i] = new LiquidCellStruct();

        return cells;
    }

    // Pass \\

    void beginPass() {
        passVisited.clear();
        passFed.clear();
    }

    // Fill \\

    boolean tryFill(LiquidCellStruct origin, short liquidBlockID) {

        blocked = false;

        if (passVisited.isVisited(origin))
            return false;

        int cellCount = scanLayer(origin, liquidBlockID);

        if (!layerEnclosed) {
            markResolved(cellCount);
            return false;
        }

        if (!claimLayer(cellCount)) {
            blocked = true;
            return false;
        }

        markResolved(cellCount);

        if (layerSourceLevel > EngineSetting.LIQUID_LEVEL_EMPTY) {
            joinSource(cellCount, liquidBlockID);
            return true;
        }

        int volume = measureVolume(cellCount);

        if (volume < cellCount * EngineSetting.LIQUID_BASIN_MIN_DEPTH) {

            if (isPouredInto(cellCount, liquidBlockID))
                markFed(cellCount);

            return false;
        }

        levelOut(cellCount, liquidBlockID, volume);
        promoteIfLarge(cellCount, liquidBlockID);

        return true;
    }

    // Layer Scan \\

    private int scanLayer(LiquidCellStruct origin, short liquidBlockID) {

        layerVisited.clear();
        layerEnclosed = false;
        layerSourceLevel = EngineSetting.LIQUID_LEVEL_EMPTY;

        if (!hasSealedFloor(origin, liquidBlockID))
            return 0;

        int cellCount = 0;
        int front = 0;

        layerVisited.visit(origin);
        layerCells[cellCount++].set(origin);

        while (front < cellCount) {

            LiquidCellStruct current = layerCells[front++];

            for (int d = 0; d < LATERAL_DIRECTIONS.length; d++) {

                if (!liquidManager.resolveNeighbor(current, LATERAL_DIRECTIONS[d], neighbor))
                    return cellCount;

                if (!layerVisited.hasRoomFor(neighbor.getSubChunkInstance()))
                    return cellCount;

                if (layerVisited.isVisited(neighbor))
                    continue;

                layerVisited.visit(neighbor);

                int neighborLevel = liquidManager.getFillableLevel(neighbor, liquidBlockID);

                if (neighborLevel == EngineSetting.LIQUID_LEVEL_BLOCKED)
                    continue;

                if (neighborLevel > EngineSetting.LIQUID_LEVEL_EMPTY && liquidManager.isPermanent(neighbor)) {
                    layerSourceLevel = Math.max(layerSourceLevel, neighborLevel);
                    continue;
                }

                if (cellCount == layerCells.length || !hasSealedFloor(neighbor, liquidBlockID))
                    return cellCount;

                layerCells[cellCount++].set(neighbor);
            }
        }

        layerEnclosed = true;
        return cellCount;
    }

    private boolean hasSealedFloor(LiquidCellStruct cell, short liquidBlockID) {

        if (!liquidManager.resolveNeighbor(cell, Direction3Vector.DOWN, floor))
            return true;

        int floorLevel = liquidManager.getFillableLevel(floor, liquidBlockID);

        return floorLevel == EngineSetting.LIQUID_LEVEL_BLOCKED || floorLevel >= EngineSetting.LIQUID_LEVEL_MAX;
    }

    private boolean claimLayer(int cellCount) {

        for (int i = 0; i < cellCount; i++)
            if (!liquidManager.claimChunk(layerCells[i].getChunkInstance()))
                return false;

        return true;
    }

    private void markResolved(int cellCount) {
        markLayer(passVisited, cellCount);
    }

    private void markFed(int cellCount) {
        markLayer(passFed, cellCount);
    }

    private void markLayer(LiquidVisitedStruct visited, int cellCount) {

        for (int i = 0; i < cellCount; i++) {

            LiquidCellStruct cell = layerCells[i];

            if (visited.hasRoomFor(cell.getSubChunkInstance()))
                visited.visit(cell);
        }
    }

    // Volume \\

    private boolean isPouredInto(int cellCount, short liquidBlockID) {

        for (int i = 0; i < cellCount; i++)
            if (liquidManager.resolveNeighbor(layerCells[i], Direction3Vector.UP, neighbor)
                    && liquidManager.getBlock(neighbor) == liquidBlockID)
                return true;

        return false;
    }

    private int measureVolume(int cellCount) {

        int volume = 0;

        for (int i = 0; i < cellCount; i++)
            volume += liquidManager.getLevel(layerCells[i]);

        return volume;
    }

    // Settle \\

    private void joinSource(int cellCount, short liquidBlockID) {

        for (int i = 0; i < cellCount; i++) {

            LiquidCellStruct cell = layerCells[i];
            int level = Math.max(liquidManager.getLevel(cell), layerSourceLevel);

            liquidManager.writeLiquid(cell, liquidBlockID, level);
            liquidManager.setPermanent(cell, true);
        }

        deactivateLayer(cellCount);
    }

    private void levelOut(int cellCount, short liquidBlockID, int volume) {

        int share = volume / cellCount;
        int remainder = volume % cellCount;

        for (int i = 0; i < cellCount; i++) {

            LiquidCellStruct cell = layerCells[i];
            int level = share + (i < remainder ? 1 : 0);

            if (liquidManager.getBlock(cell) != liquidBlockID || liquidManager.getLevel(cell) != level)
                liquidManager.writeLiquid(cell, liquidBlockID, level);
        }

        deactivateLayer(cellCount);
    }

    private void deactivateLayer(int cellCount) {
        for (int i = 0; i < cellCount; i++)
            liquidManager.deactivate(layerCells[i]);
    }

    // Permanence \\

    private void promoteIfLarge(int cellCount, short liquidBlockID) {

        int threshold = EngineSetting.LIQUID_PERMANENCE_THRESHOLD;

        if (cellCount >= threshold) {
            markPermanent(layerCells, cellCount);
            return;
        }

        bodyVisited.clear();

        int bodyCount = 0;
        int front = 0;

        for (int i = 0; i < cellCount; i++) {
            bodyVisited.visit(layerCells[i]);
            bodyCells[bodyCount++].set(layerCells[i]);
        }

        while (front < bodyCount && bodyCount < threshold) {

            LiquidCellStruct current = bodyCells[front++];

            for (int d = 0; d < BODY_DIRECTIONS.length && bodyCount < threshold; d++) {

                if (!liquidManager.resolveNeighbor(current, BODY_DIRECTIONS[d], neighbor))
                    continue;

                if (!bodyVisited.hasRoomFor(neighbor.getSubChunkInstance()))
                    return;

                if (bodyVisited.isVisited(neighbor))
                    continue;

                bodyVisited.visit(neighbor);

                if (liquidManager.getBlock(neighbor) == liquidBlockID)
                    bodyCells[bodyCount++].set(neighbor);
            }
        }

        if (bodyCount >= threshold)
            markPermanent(bodyCells, bodyCount);
    }

    private void markPermanent(LiquidCellStruct[] cells, int count) {
        for (int i = 0; i < count; i++)
            if (liquidManager.claimChunk(cells[i].getChunkInstance()))
                liquidManager.setPermanent(cells[i], true);
    }

    // Accessible \\

    boolean isBlocked() {
        return blocked;
    }

    boolean isFed(LiquidCellStruct cell) {
        return passFed.isVisited(cell);
    }
}
