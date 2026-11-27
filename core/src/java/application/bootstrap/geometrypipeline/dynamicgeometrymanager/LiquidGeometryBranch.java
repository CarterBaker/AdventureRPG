package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import application.bootstrap.geometrypipeline.quadpacket.QuadPacketInstance;
import application.bootstrap.geometrypipeline.quadpacket.QuadRecordUtility;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinateUtility;
import application.bootstrap.worldpipeline.util.LiquidFlowUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import application.bootstrap.worldpipeline.worldgenerationmanager.GenerationCacheStruct;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction2Vector;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class LiquidGeometryBranch extends BranchPackage {

    /*
     * Geometry branch for liquid blocks. Settled water emits its UP faces and
     * the faces it pours over open air from, a river stepping down its
     * channel or a fall off a cliff; flowing water emits every exposed face;
     * all greedily merged. Faces against opaque blocks are dropped, and each
     * face becomes one packed record of its first corner, extents, fill
     * level, tidal flag, flow and fall, from which the water shader rebuilds
     * its corners and surface flags. A top face carries the way its column's
     * water runs, as world generation left it in the chunk's terrain cache,
     * or churns where water pours onto it from beside, the foot of a fall,
     * carrying the drop poured onto it; an upright face carries how far above
     * its top edge its fall's lip stands, and stands full height wherever the
     * same liquid lies over it, so a falling column pours as one sheet; only
     * faces carrying the same flow and fall merge. Each merged churning face
     * is recorded as the foot of a fall in its subchunk's LiquidFallStruct,
     * where the spray at the foot is thrown up from.
     */

    // Internal
    private DynamicGeometryManager dynamicGeometryManager;
    private BiomeManager biomeManager;
    private BlockManager blockManager;
    private SubChunkInstance ERROR;

    private static final int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    private static final int WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;
    private static final int SUB_BLOCK_DIVISIONS = EngineSetting.SUB_BLOCK_DIVISIONS;

    private static final Direction3Vector[] LATERAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    // Internal \\

    @Override
    protected void create() {
        this.ERROR = create(SubChunkInstance.class);
    }

    @Override
    protected void get() {
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
    }

    @Override
    protected void start() {

        this.biomeManager = dynamicGeometryManager.getBiomeManager();
        this.blockManager = dynamicGeometryManager.getBlockManager();
    }

    // Build \\

    boolean assembleQuads(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            int xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            QuadPacketInstance quadPacket,
            BitSet accumulatedBatch,
            BitSet batchReturn,
            Color vertColorAccumulator) {

        // Settled water only shows its top face and the faces it pours over open air from
        if (subChunkInstance.isLiquidStable() && direction3Vector != Direction3Vector.UP
                && !isOpenFall(chunkInstance, subChunkInstance, xyz, direction3Vector))
            return false;

        if (!hasExposedFace(chunkInstance, subChunkInstance, xyz, direction3Vector, blockHandle))
            return false;

        assembleQuad(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                blockPaletteHandle,
                xyz,
                direction3Vector,
                biomeHandle,
                blockHandle,
                quadPacket,
                accumulatedBatch,
                batchReturn);

        return true;
    }

    // Greedy Expansion \\

    private void assembleQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            int xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            QuadPacketInstance quadPacket,
            BitSet accumulatedBatch,
            BitSet batchReturn) {

        short level = resolveLevel(chunkInstance, subChunkInstance, xyz, direction3Vector, blockHandle);
        boolean tidal = subChunkInstance.isLiquidTidal(xyz);
        byte flow = resolveFlow(chunkInstance, subChunkInstance, xyz, direction3Vector, blockHandle);
        int fall = resolveFall(chunkInstance, subChunkInstance, xyz, direction3Vector, blockHandle, flow);
        boolean tidalSurface = tidal
                && isBeneathOpenSurface(chunkInstance, subChunkInstance, xyz, blockHandle);

        byte sizeA = 1;
        byte sizeB = 1;

        Direction3Vector tangentA = Direction3Vector.getTangentA(direction3Vector);
        Direction3Vector tangentB = Direction3Vector.getTangentB(direction3Vector);

        // Each run grows as far as it can along A, then the rows it spans grow along B
        while (tryExpand(
                chunkInstance, subChunkInstance, biomePaletteHandle, blockPaletteHandle,
                xyz, direction3Vector, tangentA, tangentB, sizeA, sizeB,
                level, tidal, flow, fall, biomeHandle, blockHandle, accumulatedBatch, batchReturn)) {
            accumulatedBatch.or(batchReturn);
            sizeA++;
        }

        while (tryExpand(
                chunkInstance, subChunkInstance, biomePaletteHandle, blockPaletteHandle,
                xyz, direction3Vector, tangentB, tangentA, sizeB, sizeA,
                level, tidal, flow, fall, biomeHandle, blockHandle, accumulatedBatch, batchReturn)) {
            accumulatedBatch.or(batchReturn);
            sizeB++;
        }

        if (flow == LiquidFlowUtility.CHURN)
            recordFoot(chunkInstance, subChunkInstance, xyz, sizeA, sizeB, fall, blockHandle);

        finalizeFace(quadPacket, subChunkInstance, xyz, sizeA, sizeB, direction3Vector, blockHandle,
                level, tidalSurface, flow, fall);
    }

    private boolean tryExpand(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            int xyz,
            Direction3Vector direction3Vector,
            Direction3Vector expandDirection,
            Direction3Vector tangentDirection,
            int currentSize,
            int tangentSize,
            short level,
            boolean tidal,
            byte flow,
            int fall,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            BitSet accumulatedBatch,
            BitSet batchReturn) {

        if (currentSize >= CHUNK_SIZE)
            return false;

        int nextXYZ = ChunkCoordinateUtility.getNeighborWithOffset(xyz, expandDirection, currentSize);

        if (nextXYZ == -1)
            return false;

        for (int i = 0; i < tangentSize; i++) {

            int checkXYZ = ChunkCoordinateUtility.getNeighborWithOffset(nextXYZ, tangentDirection, i);

            if (checkXYZ == -1)
                return false;

            short comparativeBiomeID = biomePaletteHandle.getBlock(checkXYZ);
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeHandleFromBiomeID(comparativeBiomeID);
            short comparativeBlockID = blockPaletteHandle.getBlock(checkXYZ);

            if (comparativeBlockID != blockHandle.getBlockID() ||
                    comparativeBiomeHandle != biomeHandle ||
                    subChunkInstance.isLiquidTidal(checkXYZ) != tidal ||
                    accumulatedBatch.get(ChunkCoordinateUtility.getIndex(checkXYZ)) ||
                    resolveLevel(chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle) != level ||
                    !hasExposedFace(chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle) ||
                    (subChunkInstance.isLiquidStable() && direction3Vector != Direction3Vector.UP
                            && !isOpenFall(chunkInstance, subChunkInstance, checkXYZ, direction3Vector))) {
                batchReturn.clear();
                return false;
            }

            byte comparativeFlow = resolveFlow(
                    chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle);

            if (comparativeFlow != flow
                    || resolveFall(chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle, flow)
                            != fall) {
                batchReturn.clear();
                return false;
            }

            batchReturn.set(ChunkCoordinateUtility.getIndex(checkXYZ));
        }

        return true;
    }

    // Flow \\

    // The way a top face's water runs: churning under water pouring onto it from beside, else its column's flow as
    // world generation cached it, still when the cache is gone; every other face is still
    private byte resolveFlow(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle) {

        if (direction3Vector != Direction3Vector.UP)
            return LiquidFlowUtility.STILL;

        if (resolvePourSide(chunkInstance, subChunkInstance, xyz, blockHandle) != null)
            return LiquidFlowUtility.CHURN;

        GenerationCacheStruct cache = chunkInstance.getTerrainCache();

        if (!cache.isValidFor(chunkInstance.getCoordinate()))
            return LiquidFlowUtility.STILL;

        return cache.getFlow(Coordinate3Int.unpackZ(xyz) * CHUNK_SIZE + Coordinate3Int.unpackX(xyz));
    }

    // The side the same liquid stands on beside the cell above, pouring down onto this one; null when none does
    private Direction3Vector resolvePourSide(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            BlockHandle blockHandle) {

        SubChunkInstance aboveSubChunkInstance = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, xyz, Direction3Vector.UP);

        if (aboveSubChunkInstance == null || aboveSubChunkInstance == ERROR)
            return null;

        int aboveXYZ = ChunkCoordinateUtility.getNeighborAndWrap(xyz, Direction3Vector.UP);

        for (Direction3Vector lateral : LATERAL_DIRECTIONS)
            if (isLiquidBeside(chunkInstance, aboveSubChunkInstance, aboveXYZ, lateral, blockHandle))
                return lateral;

        return null;
    }

    private boolean isLiquidBeside(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector lateral,
            BlockHandle blockHandle) {

        SubChunkInstance besideSubChunkInstance = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, xyz, lateral);

        return besideSubChunkInstance != null && besideSubChunkInstance != ERROR
                && besideSubChunkInstance.getBlock(ChunkCoordinateUtility.getNeighborAndWrap(xyz, lateral))
                        == blockHandle.getBlockID();
    }

    // Fall \\

    // The level a face is drawn at: an upright face stands full height wherever the same liquid lies over its cell,
    // so a falling column pours as one sheet; every other face takes its cell's own level
    private short resolveLevel(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle) {

        if (isLateral(direction3Vector) && !isBeneathOpenSurface(chunkInstance, subChunkInstance, xyz, blockHandle))
            return EngineSetting.LIQUID_LEVEL_MAX;

        return subChunkInstance.getLiquidLevel(xyz);
    }

    // What a face carries of the fall it belongs to: an upright face the height of its fall's lip, the topmost cell
    // of the run of the same liquid above it that shows the same face, and a churning top face the drop poured onto
    // it; nothing on any other face
    private int resolveFall(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle,
            byte flow) {

        if (isLateral(direction3Vector))
            return resolveLipY(chunkInstance, subChunkInstance, xyz, direction3Vector, blockHandle);

        if (flow != LiquidFlowUtility.CHURN)
            return 0;

        Direction3Vector side = resolvePourSide(chunkInstance, subChunkInstance, xyz, blockHandle);

        return side == null ? 0 : resolveDrop(chunkInstance, subChunkInstance, xyz, side, blockHandle);
    }

    // The block height just over the lip of the fall an upright face belongs to, counted across the column's
    // subchunks no further than the most a record's fall bits hold
    private int resolveLipY(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle) {

        SubChunkInstance cellSubChunkInstance = subChunkInstance;
        int cellXYZ = xyz;
        int lipY = resolveTotalY(subChunkInstance, xyz) + 1;

        for (int climbed = 0; climbed < EngineSetting.LIQUID_FALL_MAX_BLOCKS; climbed++) {

            SubChunkInstance aboveSubChunkInstance = getComparativeSubChunkInstance(
                    chunkInstance, cellSubChunkInstance, cellXYZ, Direction3Vector.UP);

            if (aboveSubChunkInstance == null || aboveSubChunkInstance == ERROR)
                break;

            int aboveXYZ = ChunkCoordinateUtility.getNeighborAndWrap(cellXYZ, Direction3Vector.UP);

            if (aboveSubChunkInstance.getBlock(aboveXYZ) != blockHandle.getBlockID()
                    || !hasExposedFace(chunkInstance, aboveSubChunkInstance, aboveXYZ, direction3Vector, blockHandle))
                break;

            cellSubChunkInstance = aboveSubChunkInstance;
            cellXYZ = aboveXYZ;
            lipY++;
        }

        return lipY;
    }

    // How far water falls onto a cell from the side it pours from: the height of the open column over the cell that
    // the same liquid stands beside, at least one block
    private int resolveDrop(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector side,
            BlockHandle blockHandle) {

        SubChunkInstance cellSubChunkInstance = subChunkInstance;
        int cellXYZ = xyz;
        int drop = 0;

        while (drop < EngineSetting.LIQUID_FALL_MAX_BLOCKS) {

            SubChunkInstance aboveSubChunkInstance = getComparativeSubChunkInstance(
                    chunkInstance, cellSubChunkInstance, cellXYZ, Direction3Vector.UP);

            if (aboveSubChunkInstance == null || aboveSubChunkInstance == ERROR)
                break;

            int aboveXYZ = ChunkCoordinateUtility.getNeighborAndWrap(cellXYZ, Direction3Vector.UP);

            if (aboveSubChunkInstance.getBlock(aboveXYZ) == blockHandle.getBlockID()
                    || !isLiquidBeside(chunkInstance, aboveSubChunkInstance, aboveXYZ, side, blockHandle))
                break;

            cellSubChunkInstance = aboveSubChunkInstance;
            cellXYZ = aboveXYZ;
            drop++;
        }

        return LiquidFlowUtility.clampDrop(drop);
    }

    // A merged churning face kept as the foot of its fall, with the side its water pours from
    private void recordFoot(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            byte sizeA,
            byte sizeB,
            int drop,
            BlockHandle blockHandle) {

        Direction3Vector side = resolvePourSide(chunkInstance, subChunkInstance, xyz, blockHandle);

        if (side == null)
            return;

        subChunkInstance.getLiquidFalls().add(LiquidFlowUtility.packFoot(
                Coordinate3Int.unpackX(xyz),
                Coordinate3Int.unpackY(xyz),
                Coordinate3Int.unpackZ(xyz),
                sizeA,
                sizeB,
                drop,
                side.index));
    }

    private int resolveTotalY(SubChunkInstance subChunkInstance, int xyz) {
        return (int) subChunkInstance.getCoordinate() * CHUNK_SIZE + Coordinate3Int.unpackY(xyz);
    }

    // Face Exposure \\

    private boolean hasExposedFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle) {

        SubChunkInstance comparativeSubChunkInstance = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, xyz, direction3Vector);

        if (comparativeSubChunkInstance == ERROR)
            return false;

        if (comparativeSubChunkInstance == null) {
            byte subY = (byte) subChunkInstance.getCoordinate();
            return (direction3Vector == Direction3Vector.DOWN && subY == 0)
                    || (direction3Vector == Direction3Vector.UP && subY == WORLD_HEIGHT - 1);
        }

        int comparativeXYZ = ChunkCoordinateUtility.getNeighborAndWrap(xyz, direction3Vector);
        short comparativeBlockID = comparativeSubChunkInstance.getBlockPaletteHandle().getBlock(comparativeXYZ);

        // Same liquid on the other side is an internal boundary, except
        // sideways against a lower neighbor — that step down in the surface
        // is exactly what a flowing stream needs to show.
        if (comparativeBlockID == blockHandle.getBlockID())
            return isLateral(direction3Vector)
                    && comparativeSubChunkInstance.getLiquidLevel(comparativeXYZ)
                            < subChunkInstance.getLiquidLevel(xyz);

        // Any neighbor but a whole opaque cube exposes the face
        BlockHandle comparativeBlockHandle = blockManager.getBlockHandleFromBlockID(comparativeBlockID);

        if (SubBlockUtility.isSubdivided(comparativeSubChunkInstance.getSubBlockMask(comparativeXYZ)))
            return true;

        return comparativeBlockHandle.getGeometry() != DynamicGeometryType.FULL;
    }

    private boolean isBeneathOpenSurface(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            BlockHandle blockHandle) {

        SubChunkInstance aboveSubChunkInstance = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, xyz, Direction3Vector.UP);

        if (aboveSubChunkInstance == null)
            return true;

        int aboveXYZ = ChunkCoordinateUtility.getNeighborAndWrap(xyz, Direction3Vector.UP);

        return aboveSubChunkInstance.getBlock(aboveXYZ) != blockHandle.getBlockID();
    }

    // Whether settled water pours over open air through an upright face: a cell off the tide whose neighbour that
    // way is an empty, whole cell
    private boolean isOpenFall(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector) {

        if (!isLateral(direction3Vector) || subChunkInstance.isLiquidTidal(xyz))
            return false;

        SubChunkInstance comparativeSubChunkInstance = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, xyz, direction3Vector);

        if (comparativeSubChunkInstance == null || comparativeSubChunkInstance == ERROR)
            return false;

        int comparativeXYZ = ChunkCoordinateUtility.getNeighborAndWrap(xyz, direction3Vector);

        return !SubBlockUtility.isSubdivided(comparativeSubChunkInstance.getSubBlockMask(comparativeXYZ))
                && blockManager.getBlockHandleFromBlockID(comparativeSubChunkInstance.getBlock(comparativeXYZ))
                        .getGeometry() == DynamicGeometryType.NONE;
    }

    private boolean isLateral(Direction3Vector direction3Vector) {
        return direction3Vector != Direction3Vector.UP && direction3Vector != Direction3Vector.DOWN;
    }

    private SubChunkInstance getComparativeSubChunkInstance(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector) {

        if (!ChunkCoordinateUtility.isAtEdge(xyz, direction3Vector))
            return subChunkInstance;

        byte subChunkCoordinate = (byte) subChunkInstance.getCoordinate();

        if (direction3Vector == Direction3Vector.UP || direction3Vector == Direction3Vector.DOWN) {
            byte comparativeSubChunkCoordinate = (byte) (subChunkCoordinate + direction3Vector.y);
            if (comparativeSubChunkCoordinate >= 0 && comparativeSubChunkCoordinate < WORLD_HEIGHT)
                return chunkInstance.getSubChunk(comparativeSubChunkCoordinate);
            else
                return null;
        }

        Direction2Vector direction2Vector = direction3Vector.to2D();
        ChunkNeighborHandle chunkNeighborHandle = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborHandle.getBuildNeighborChunk(direction2Vector.index);

        if (neighborChunkInstance == null)
            return ERROR;

        SubChunkInstance comparativeSubChunkInstance = neighborChunkInstance.getSubChunk(subChunkCoordinate);

        if (comparativeSubChunkInstance == null)
            return ERROR;

        return comparativeSubChunkInstance;
    }

    // Face Finalization \\

    // One record per merged face: its first corner and extents, the level it holds, whether it rides the tide, the
    // way it runs and its fall, an upright face's lip counted from its top edge
    private void finalizeFace(
            QuadPacketInstance quadPacket,
            SubChunkInstance subChunkInstance,
            int xyz,
            byte sizeA,
            byte sizeB,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle,
            short level,
            boolean tidalSurface,
            byte flow,
            int fall) {

        int vertXYZ = ChunkCoordinateUtility.convertToVertSpace(xyz, direction3Vector);
        IntArrayList records = quadPacket.getRecords(blockHandle.getMaterialID(), EngineSetting.QUAD_RECORD_LIQUID_INTS);

        records.add(QuadRecordUtility.packPosition(
                Coordinate3Int.unpackX(vertXYZ) * SUB_BLOCK_DIVISIONS,
                Coordinate3Int.unpackY(vertXYZ) * SUB_BLOCK_DIVISIONS,
                Coordinate3Int.unpackZ(vertXYZ) * SUB_BLOCK_DIVISIONS));
        int recordFall = isLateral(direction3Vector)
                ? Math.min(Math.max(fall - resolveTotalY(subChunkInstance, xyz) - sizeB, 0),
                        EngineSetting.LIQUID_FALL_MAX_BLOCKS)
                : fall;

        records.add(QuadRecordUtility.packLiquid(
                direction3Vector.index, sizeA, sizeB, level, tidalSurface, flow, recordFall));
    }
}
