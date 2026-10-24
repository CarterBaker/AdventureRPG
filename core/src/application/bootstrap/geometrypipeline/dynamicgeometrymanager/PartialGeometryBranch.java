package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import application.bootstrap.geometrypipeline.quadpacket.QuadPacketInstance;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;

class PartialGeometryBranch extends BranchPackage {

    /*
     * Sub-block resolution pass. Owns every face of a subdivided block and
     * every whole-block face that is not block-simple, greedily merging
     * sub-faces of matching block, biome, orientation, coverage word and
     * content within the subchunk and the merge extent limit.
     */

    // Internal
    private DynamicGeometryManager dynamicGeometryManager;
    private BiomeManager biomeManager;
    private BlockManager blockManager;
    private SubCellSampleBranch subCellSampleBranch;
    private SurfaceEmissionBranch surfaceEmissionBranch;

    // Settings
    private int divisions;
    private int subChunkSize;
    private int maxMergeExtent;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.divisions = SubBlockUtility.DIVISIONS;
        this.subChunkSize = EngineSetting.CHUNK_SIZE * divisions;
        this.maxMergeExtent = EngineSetting.GEOMETRY_MAX_MERGE_EXTENT;
    }

    @Override
    protected void get() {

        // Internal
        this.dynamicGeometryManager = get(DynamicGeometryManager.class);
        this.subCellSampleBranch = get(SubCellSampleBranch.class);
        this.surfaceEmissionBranch = get(SurfaceEmissionBranch.class);
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
            BitSet subAccumulatedBatch,
            BitSet subBatchReturn,
            Color vertColorAccumulator) {

        int mask = blockPaletteHandle.getSubBlockMask(xyz);
        int cellX = Coordinate3Int.unpackX(xyz);
        int cellY = Coordinate3Int.unpackY(xyz);
        int cellZ = Coordinate3Int.unpackZ(xyz);

        boolean assembled = false;

        for (int octant = 0; octant < SubBlockUtility.OCTANT_COUNT; octant++) {

            if (!SubBlockUtility.hasOctant(mask, octant))
                continue;

            int subX = cellX * divisions + SubBlockUtility.getOctantX(octant);
            int subY = cellY * divisions + SubBlockUtility.getOctantY(octant);
            int subZ = cellZ * divisions + SubBlockUtility.getOctantZ(octant);

            if (subAccumulatedBatch.get(getSubIndex(subX, subY, subZ)))
                continue;

            if (!subCellSampleBranch.hasSubFace(
                    chunkInstance, subChunkInstance, subX, subY, subZ, blockHandle, direction3Vector))
                continue;

            assembleQuad(
                    chunkInstance,
                    subChunkInstance,
                    biomePaletteHandle,
                    blockPaletteHandle,
                    rotationPaletteHandle,
                    subX, subY, subZ,
                    direction3Vector,
                    biomeHandle,
                    blockHandle,
                    quadPacket,
                    subAccumulatedBatch,
                    subBatchReturn,
                    vertColorAccumulator);

            assembled = true;
        }

        return assembled;
    }

    // Merging \\

    private void assembleQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            int subX, int subY, int subZ,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            QuadPacketInstance quadPacket,
            BitSet subAccumulatedBatch,
            BitSet subBatchReturn,
            Color vertColorAccumulator) {

        int sizeA = 1;
        int sizeB = 1;

        Direction3Vector tangentA = Direction3Vector.getTangentA(direction3Vector);
        Direction3Vector tangentB = Direction3Vector.getTangentB(direction3Vector);

        int baseCellXYZ = subCellSampleBranch.toCellXYZ(subX, subY, subZ);
        short baseOrientation = rotationPaletteHandle.getBlock(baseCellXYZ);
        int baseCoverageWord = surfaceEmissionBranch.resolveCoverageWord(
                subChunkInstance, baseCellXYZ, direction3Vector);
        short baseContentID = surfaceEmissionBranch.resolveContentID(subChunkInstance, baseCellXYZ);

        subAccumulatedBatch.set(getSubIndex(subX, subY, subZ));

        // Each run grows as far as it can along A, then the rows it spans grow along B
        while (tryExpand(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                blockPaletteHandle,
                rotationPaletteHandle,
                subX, subY, subZ,
                direction3Vector,
                tangentA,
                tangentB,
                sizeA,
                sizeB,
                biomeHandle,
                blockHandle,
                baseOrientation,
                baseCoverageWord,
                baseContentID,
                subAccumulatedBatch,
                subBatchReturn)) {
            subAccumulatedBatch.or(subBatchReturn);
            sizeA++;
        }

        while (tryExpand(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                blockPaletteHandle,
                rotationPaletteHandle,
                subX, subY, subZ,
                direction3Vector,
                tangentB,
                tangentA,
                sizeB,
                sizeA,
                biomeHandle,
                blockHandle,
                baseOrientation,
                baseCoverageWord,
                baseContentID,
                subAccumulatedBatch,
                subBatchReturn)) {
            subAccumulatedBatch.or(subBatchReturn);
            sizeB++;
        }

        surfaceEmissionBranch.emitQuad(
                chunkInstance,
                subChunkInstance,
                rotationPaletteHandle,
                quadPacket,
                vertColorAccumulator,
                subX, subY, subZ,
                sizeA,
                sizeB,
                EngineSetting.PARTIAL_CELLS_PER_EDGE_ENTRY,
                direction3Vector,
                biomeHandle,
                blockHandle);
    }

    private boolean tryExpand(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            int subX, int subY, int subZ,
            Direction3Vector direction3Vector,
            Direction3Vector expandDirection,
            Direction3Vector tangentDirection,
            int currentSize,
            int tangentSize,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short baseOrientation,
            int baseCoverageWord,
            short baseContentID,
            BitSet subAccumulatedBatch,
            BitSet subBatchReturn) {

        subBatchReturn.clear();

        if (currentSize >= maxMergeExtent)
            return false;

        boolean orientationSensitive = subCellSampleBranch.requiresOrientationMatch(blockHandle);

        for (int i = 0; i < tangentSize; i++) {

            int checkX = subX + expandDirection.x * currentSize + tangentDirection.x * i;
            int checkY = subY + expandDirection.y * currentSize + tangentDirection.y * i;
            int checkZ = subZ + expandDirection.z * currentSize + tangentDirection.z * i;

            if (!subCellSampleBranch.isInsideSubChunk(checkX, checkY, checkZ) || !canMerge(
                    chunkInstance,
                    subChunkInstance,
                    biomePaletteHandle,
                    blockPaletteHandle,
                    rotationPaletteHandle,
                    checkX, checkY, checkZ,
                    direction3Vector,
                    biomeHandle,
                    blockHandle,
                    orientationSensitive,
                    baseOrientation,
                    baseCoverageWord,
                    baseContentID,
                    subAccumulatedBatch)) {
                subBatchReturn.clear();
                return false;
            }

            subBatchReturn.set(getSubIndex(checkX, checkY, checkZ));
        }

        return true;
    }

    private boolean canMerge(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            int checkX, int checkY, int checkZ,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            boolean orientationSensitive,
            short baseOrientation,
            int baseCoverageWord,
            short baseContentID,
            BitSet subAccumulatedBatch) {

        if (subAccumulatedBatch.get(getSubIndex(checkX, checkY, checkZ)))
            return false;

        int cellXYZ = subCellSampleBranch.toCellXYZ(checkX, checkY, checkZ);

        if (biomeManager.getBiomeHandleFromBiomeID(biomePaletteHandle.getBlock(cellXYZ)) != biomeHandle)
            return false;

        if (blockManager.getBlockHandleFromBlockID(blockPaletteHandle.getBlock(cellXYZ)) != blockHandle)
            return false;

        int mask = blockPaletteHandle.getSubBlockMask(cellXYZ);
        int octant = SubBlockUtility.getOctant(
                Math.floorMod(checkX, divisions),
                Math.floorMod(checkY, divisions),
                Math.floorMod(checkZ, divisions));

        if (!SubBlockUtility.hasOctant(mask, octant))
            return false;

        if (orientationSensitive && rotationPaletteHandle.getBlock(cellXYZ) != baseOrientation)
            return false;

        if (surfaceEmissionBranch.resolveCoverageWord(subChunkInstance, cellXYZ, direction3Vector) != baseCoverageWord)
            return false;

        if (surfaceEmissionBranch.resolveContentID(subChunkInstance, cellXYZ) != baseContentID)
            return false;

        if (!subCellSampleBranch.hasSubFace(
                chunkInstance, subChunkInstance, checkX, checkY, checkZ, blockHandle, direction3Vector))
            return false;

        return SubBlockUtility.isSubdivided(mask) || !subCellSampleBranch.isBlockSimple(
                chunkInstance, subChunkInstance, cellXYZ, direction3Vector, blockHandle);
    }

    // Utility \\

    private int getSubIndex(int subX, int subY, int subZ) {
        return (subY * subChunkSize + subZ) * subChunkSize + subX;
    }
}
