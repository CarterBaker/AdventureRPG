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
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction2Vector;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class LiquidGeometryBranch extends BranchPackage {

    /*
     * Geometry branch for liquid blocks. Settled water emits only its UP
     * faces; flowing water emits every exposed face; both greedily merged. Faces
     * against opaque blocks are dropped, and each face becomes one packed
     * record of its first corner, extents, fill level and tidal flag, from
     * which the water shader rebuilds its corners and surface flags.
     */

    // Internal
    private BiomeManager biomeManager;
    private BlockManager blockManager;
    private SubChunkInstance ERROR;

    private static final int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    private static final int WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;
    private static final int SUB_BLOCK_DIVISIONS = EngineSetting.SUB_BLOCK_DIVISIONS;

    // Internal \\

    @Override
    protected void create() {
        this.ERROR = create(SubChunkInstance.class);
    }

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
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

        // Settled water only ever shows its top face
        if (subChunkInstance.isLiquidStable() && direction3Vector != Direction3Vector.UP)
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

        short level = blockPaletteHandle.getLiquidLevel(xyz);
        boolean tidal = subChunkInstance.isLiquidTidal(xyz);
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
                level, tidal, biomeHandle, blockHandle, accumulatedBatch, batchReturn)) {
            accumulatedBatch.or(batchReturn);
            sizeA++;
        }

        while (tryExpand(
                chunkInstance, subChunkInstance, biomePaletteHandle, blockPaletteHandle,
                xyz, direction3Vector, tangentB, tangentA, sizeB, sizeA,
                level, tidal, biomeHandle, blockHandle, accumulatedBatch, batchReturn)) {
            accumulatedBatch.or(batchReturn);
            sizeB++;
        }

        finalizeFace(quadPacket, xyz, sizeA, sizeB, direction3Vector, blockHandle, level, tidalSurface);
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
            short comparativeLevel = blockPaletteHandle.getLiquidLevel(checkXYZ);

            if (comparativeBlockID != blockHandle.getBlockID() ||
                    comparativeBiomeHandle != biomeHandle ||
                    comparativeLevel != level ||
                    subChunkInstance.isLiquidTidal(checkXYZ) != tidal ||
                    accumulatedBatch.get(ChunkCoordinateUtility.getIndex(checkXYZ)) ||
                    !hasExposedFace(chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle)) {
                batchReturn.clear();
                return false;
            }

            batchReturn.set(ChunkCoordinateUtility.getIndex(checkXYZ));
        }

        return true;
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
        ChunkInstance neighborChunkInstance = chunkNeighborHandle.getNeighborChunk(direction2Vector.index);

        if (neighborChunkInstance == null)
            return ERROR;

        SubChunkInstance comparativeSubChunkInstance = neighborChunkInstance.getSubChunk(subChunkCoordinate);

        if (comparativeSubChunkInstance == null)
            return ERROR;

        return comparativeSubChunkInstance;
    }

    // Face Finalization \\

    // One record per merged face: its first corner and extents, the level it holds and whether it rides the tide
    private void finalizeFace(
            QuadPacketInstance quadPacket,
            int xyz,
            byte sizeA,
            byte sizeB,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle,
            short level,
            boolean tidalSurface) {

        int vertXYZ = ChunkCoordinateUtility.convertToVertSpace(xyz, direction3Vector);
        IntArrayList records = quadPacket.getRecords(blockHandle.getMaterialID(), EngineSetting.QUAD_RECORD_LIQUID_INTS);

        records.add(QuadRecordUtility.packPosition(
                Coordinate3Int.unpackX(vertXYZ) * SUB_BLOCK_DIVISIONS,
                Coordinate3Int.unpackY(vertXYZ) * SUB_BLOCK_DIVISIONS,
                Coordinate3Int.unpackZ(vertXYZ) * SUB_BLOCK_DIVISIONS));
        records.add(QuadRecordUtility.packLiquid(direction3Vector.index, sizeA, sizeB, level, tidalSurface));
    }
}
