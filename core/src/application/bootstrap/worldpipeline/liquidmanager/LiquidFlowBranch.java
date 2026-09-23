package application.bootstrap.worldpipeline.liquidmanager;

import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.BiomeFieldUtility;
import application.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;

class LiquidFlowBranch extends BranchPackage {

    /*
     * Advances one subchunk's active liquid cells by one Terraria-style step:
     * pour straight down, then level out sideways toward lower neighbors,
     * favoring drops, splitting evenly otherwise, and passing no more than
     * LIQUID_SPREAD_RATE per neighbor so water holds its shape as it runs.
     * Wetting dry ground costs LIQUID_SPREAD_LOSS, thin water asks
     * LiquidBasinBranch whether it can fill its basin, and a thin rim that
     * cannot move evaporates inward. Permanent cells pour uncapped and give
     * without ever losing level.
     */

    private static final Direction3Vector[] LATERAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    // Internal
    private LiquidManager liquidManager;
    private LiquidBasinBranch liquidBasinBranch;

    // State
    private long passCount;
    private boolean reverseOrder;
    private boolean blocked;

    // Scratch — step snapshot
    private int[] activeCells;
    private LiquidCellStruct cell;
    private LiquidCellStruct below;
    private LiquidCellStruct probe;

    // Scratch — one spread target per lateral direction
    private LiquidCellStruct[] targets;
    private int[] targetLevels;
    private boolean[] targetDrops;

    // Base \\

    @Override
    protected void create() {

        // Scratch — step snapshot
        this.activeCells = new int[ChunkCoordinate3Int.BLOCK_COORDINATE_COUNT];
        this.cell = new LiquidCellStruct();
        this.below = new LiquidCellStruct();
        this.probe = new LiquidCellStruct();

        // Scratch — one spread target per lateral direction
        this.targets = new LiquidCellStruct[LATERAL_DIRECTIONS.length];
        this.targetLevels = new int[LATERAL_DIRECTIONS.length];
        this.targetDrops = new boolean[LATERAL_DIRECTIONS.length];

        for (int i = 0; i < targets.length; i++)
            targets[i] = new LiquidCellStruct();
    }

    @Override
    protected void get() {

        // Internal
        this.liquidManager = get(LiquidManager.class);
        this.liquidBasinBranch = get(LiquidBasinBranch.class);
    }

    // Flow \\

    void flow(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance) {

        int activeCount = subChunkInstance.collectActiveLiquid(activeCells);

        passCount++;
        reverseOrder = !reverseOrder;
        liquidBasinBranch.beginPass();

        for (int i = 0; i < activeCount; i++) {

            int cellIndex = activeCells[reverseOrder ? activeCount - 1 - i : i];

            cell.set(chunkInstance, subChunkInstance, ChunkCoordinate3Int.getBlockCoordinate(cellIndex));
            stepCell();
        }

        if (!subChunkInstance.hasActiveLiquid())
            liquidManager.markTouched(chunkInstance, subChunkInstance);
    }

    private void stepCell() {

        short liquidBlockID = liquidManager.getBlock(cell);

        if (!liquidManager.isLiquid(liquidBlockID))
            return;

        int level = liquidManager.getLevel(cell);

        if (level <= EngineSetting.LIQUID_LEVEL_EMPTY) {
            liquidManager.clearLiquid(cell);
            return;
        }

        boolean permanent = liquidManager.isPermanent(cell);
        blocked = false;

        int fallen = fall(liquidBlockID, level);
        int remaining = drain(liquidBlockID, level, fallen, permanent);

        if (remaining <= EngineSetting.LIQUID_LEVEL_EMPTY)
            return;

        if (!permanent
                && remaining <= EngineSetting.LIQUID_BASIN_TRIGGER_LEVEL
                && liquidBasinBranch.tryFill(cell, liquidBlockID))
            return;

        int spread = spread(liquidBlockID, remaining, permanent);
        drain(liquidBlockID, remaining, spread, permanent);

        if (fallen > 0 || spread > 0)
            return;

        settle(liquidBlockID, remaining, permanent);
    }

    // Gravity \\

    private int fall(short liquidBlockID, int level) {

        if (!liquidManager.resolveNeighbor(cell, Direction3Vector.DOWN, below))
            return 0;

        int belowLevel = liquidManager.getFillableLevel(below, liquidBlockID);

        if (belowLevel == EngineSetting.LIQUID_LEVEL_BLOCKED)
            return 0;

        int capacity = EngineSetting.LIQUID_LEVEL_MAX - belowLevel;

        if (capacity <= 0)
            return 0;

        if (!liquidManager.claimChunk(below.getChunkInstance())) {
            blocked = true;
            return 0;
        }

        int amount = Math.min(level, capacity);
        liquidManager.writeLiquid(below, liquidBlockID, belowLevel + amount);

        return amount;
    }

    // Lateral Spread \\

    private int spread(short liquidBlockID, int level, boolean permanent) {

        int targetCount = collectSpreadTargets(liquidBlockID, level);

        if (targetCount == 0)
            return 0;

        targetCount = keepOnlyTargetsBelowShare(level, targetCount);

        int pool = level;
        int loss = 0;

        for (int i = 0; i < targetCount; i++) {

            pool += targetLevels[i];

            if (targetLevels[i] == EngineSetting.LIQUID_LEVEL_EMPTY)
                loss = EngineSetting.LIQUID_SPREAD_LOSS;
        }

        int poolSize = targetCount + 1;
        int share = pool / poolSize;
        int remainder = pool % poolSize;
        int budget = level - share - loss;

        if (budget <= 0)
            return 0;

        int rate = permanent ? EngineSetting.LIQUID_LEVEL_MAX : EngineSetting.LIQUID_SPREAD_RATE;

        int delivered = 0;

        for (int i = 0; i < targetCount && budget > 0; i++) {

            int need = share + (i < remainder ? 1 : 0) - targetLevels[i];
            int amount = Math.min(Math.min(need, budget), rate);

            if (amount <= 0)
                continue;

            liquidManager.writeLiquid(targets[i], liquidBlockID, targetLevels[i] + amount);

            budget -= amount;
            delivered += amount;
        }

        return delivered > 0 ? delivered + loss : 0;
    }

    private int collectSpreadTargets(short liquidBlockID, int level) {

        int targetCount = 0;
        int dropCount = 0;

        for (int d = 0; d < LATERAL_DIRECTIONS.length; d++) {

            LiquidCellStruct target = targets[targetCount];

            if (!liquidManager.resolveNeighbor(cell, LATERAL_DIRECTIONS[d], target))
                continue;

            int targetLevel = liquidManager.getFillableLevel(target, liquidBlockID);

            if (!canSpreadInto(level, targetLevel))
                continue;

            if (!liquidManager.claimChunk(target.getChunkInstance())) {
                blocked = true;
                continue;
            }

            boolean drop = opensOntoDrop(target, liquidBlockID);

            targetLevels[targetCount] = targetLevel;
            targetDrops[targetCount] = drop;
            targetCount++;

            if (drop)
                dropCount++;
        }

        if (dropCount == 0 || dropCount == targetCount)
            return targetCount;

        int kept = 0;

        for (int i = 0; i < targetCount; i++)
            if (targetDrops[i])
                swapTargets(kept++, i);

        return kept;
    }

    private boolean canSpreadInto(int level, int targetLevel) {

        if (targetLevel == EngineSetting.LIQUID_LEVEL_BLOCKED)
            return false;

        if (targetLevel == EngineSetting.LIQUID_LEVEL_EMPTY)
            return level >= EngineSetting.LIQUID_WET_MIN_LEVEL;

        return level - targetLevel >= EngineSetting.LIQUID_SPREAD_MIN_DIFFERENCE;
    }

    private int keepOnlyTargetsBelowShare(int level, int targetCount) {

        boolean removed = true;

        while (removed && targetCount > 0) {

            int pool = level;
            for (int i = 0; i < targetCount; i++)
                pool += targetLevels[i];

            int share = pool / (targetCount + 1);
            removed = false;

            for (int i = targetCount - 1; i >= 0; i--) {

                if (targetLevels[i] < share)
                    continue;

                swapTargets(i, --targetCount);
                removed = true;
            }
        }

        return targetCount;
    }

    private boolean opensOntoDrop(LiquidCellStruct target, short liquidBlockID) {

        if (!liquidManager.resolveNeighbor(target, Direction3Vector.DOWN, probe))
            return false;

        int dropLevel = liquidManager.getFillableLevel(probe, liquidBlockID);

        return dropLevel != EngineSetting.LIQUID_LEVEL_BLOCKED && dropLevel < EngineSetting.LIQUID_LEVEL_MAX;
    }

    private void swapTargets(int a, int b) {

        if (a == b)
            return;

        LiquidCellStruct target = targets[a];
        targets[a] = targets[b];
        targets[b] = target;

        int targetLevel = targetLevels[a];
        targetLevels[a] = targetLevels[b];
        targetLevels[b] = targetLevel;

        boolean targetDrop = targetDrops[a];
        targetDrops[a] = targetDrops[b];
        targetDrops[b] = targetDrop;
    }

    // Settle \\

    private void settle(short liquidBlockID, int level, boolean permanent) {

        if (blocked)
            return;

        if (permanent) {
            liquidManager.deactivate(cell);
            return;
        }

        if (liquidBasinBranch.tryFill(cell, liquidBlockID))
            return;

        if (liquidBasinBranch.isBlocked())
            return;

        if (level <= EngineSetting.LIQUID_EVAPORATION_LEVEL
                && !liquidBasinBranch.isFed(cell)
                && bordersDryGround(liquidBlockID)) {
            evaporate(liquidBlockID, level);
            return;
        }

        liquidManager.deactivate(cell);
    }

    private void evaporate(short liquidBlockID, int level) {

        long cellHash = BiomeFieldUtility.hashCell(
                passCount,
                ChunkCoordinate3Int.getIndex(cell.getPackedXYZ()),
                (int) cell.getSubChunkInstance().getCoordinate());

        if (BiomeFieldUtility.hash01(cellHash) >= EngineSetting.LIQUID_EVAPORATION_CHANCE)
            return;

        liquidManager.writeLiquid(cell, liquidBlockID, level - EngineSetting.LIQUID_EVAPORATION_RATE);
    }

    private boolean bordersDryGround(short liquidBlockID) {

        for (int d = 0; d < LATERAL_DIRECTIONS.length; d++)
            if (liquidManager.resolveNeighbor(cell, LATERAL_DIRECTIONS[d], probe)
                    && liquidManager.getFillableLevel(probe, liquidBlockID) == EngineSetting.LIQUID_LEVEL_EMPTY)
                return true;

        return false;
    }

    // Source \\

    private int drain(short liquidBlockID, int level, int amount, boolean permanent) {

        if (amount <= 0 || permanent)
            return level;

        int remaining = level - amount;
        liquidManager.writeLiquid(cell, liquidBlockID, remaining);

        return remaining;
    }
}
