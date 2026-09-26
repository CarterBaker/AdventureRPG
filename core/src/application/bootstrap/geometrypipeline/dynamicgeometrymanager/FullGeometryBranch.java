package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class FullGeometryBranch extends BranchPackage {

    /*
     * Block-resolution pass for whole full-cube blocks. Greedily merges the
     * faces GeometryBuildManager routes here — only faces SubCellSampleBranch
     * proves block-simple, so no block this face or its edges consult is
     * subdivided — into quads of up to GEOMETRY_MAX_MERGE_EXTENT blocks, and
     * hands each to SurfaceEmissionBranch with one edge word entry per block.
     * A face that is not block-simple, and every face of a subdivided block,
     * belongs to PartialGeometryBranch instead, and expansion here never
     * crosses into one, so the two passes never emit the same surface.
     */

    // Internal
    private BiomeManager biomeManager;
    private BlockManager blockManager;
    private SubCellSampleBranch subCellSampleBranch;
    private SurfaceEmissionBranch surfaceEmissionBranch;

    // Settings
    private int divisions;
    private int maxMergeExtent;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.divisions = SubBlockUtility.DIVISIONS;
        this.maxMergeExtent = EngineSetting.GEOMETRY_MAX_MERGE_EXTENT;
    }

    @Override
    protected void get() {

        // Internal
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
        this.subCellSampleBranch = get(SubCellSampleBranch.class);
        this.surfaceEmissionBranch = get(SurfaceEmissionBranch.class);
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
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            BitSet accumulatedBatch,
            BitSet batchReturn,
            Color vertColorAccumulator) {

        if (!blockHasFace(chunkInstance, subChunkInstance, xyz, direction3Vector, blockHandle))
            return false;

        assembleQuad(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                blockPaletteHandle,
                rotationPaletteHandle,
                xyz,
                direction3Vector,
                biomeHandle,
                blockHandle,
                verts,
                accumulatedBatch,
                batchReturn,
                vertColorAccumulator);

        return true;
    }

    // Face Presence \\

    private boolean blockHasFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle) {

        return subCellSampleBranch.hasSubFace(
                chunkInstance,
                subChunkInstance,
                toFrontSub(Coordinate3Int.unpackX(xyz), direction3Vector.x),
                toFrontSub(Coordinate3Int.unpackY(xyz), direction3Vector.y),
                toFrontSub(Coordinate3Int.unpackZ(xyz), direction3Vector.z),
                blockHandle,
                direction3Vector);
    }

    // The sub-cell of a block on the low side of each tangent and touching the face along its normal
    private int toFrontSub(int cell, int directionComponent) {
        return cell * divisions + (directionComponent > 0 ? divisions - 1 : 0);
    }

    // Merging \\

    private void assembleQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            int xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            BitSet accumulatedBatch,
            BitSet batchReturn,
            Color vertColorAccumulator) {

        boolean checkA = true;
        boolean checkB = true;
        int sizeA = 1;
        int sizeB = 1;

        Direction3Vector tangentA = Direction3Vector.getTangentA(direction3Vector);
        Direction3Vector tangentB = Direction3Vector.getTangentB(direction3Vector);

        short baseOrientation = rotationPaletteHandle.getBlock(xyz);

        accumulatedBatch.set(ChunkCoordinate3Int.getIndex(xyz));

        do {

            if (checkA) {
                if (tryExpand(
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
                        rotationPaletteHandle,
                        xyz,
                        direction3Vector,
                        tangentA,
                        tangentB,
                        sizeA,
                        sizeB,
                        biomeHandle,
                        blockHandle,
                        baseOrientation,
                        accumulatedBatch,
                        batchReturn)) {
                    accumulatedBatch.or(batchReturn);
                    sizeA++;
                } else
                    checkA = false;
            }

            if (checkB) {
                if (tryExpand(
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
                        rotationPaletteHandle,
                        xyz,
                        direction3Vector,
                        tangentB,
                        tangentA,
                        sizeB,
                        sizeA,
                        biomeHandle,
                        blockHandle,
                        baseOrientation,
                        accumulatedBatch,
                        batchReturn)) {
                    accumulatedBatch.or(batchReturn);
                    sizeB++;
                } else
                    checkB = false;
            }
        } while (checkA || checkB);

        surfaceEmissionBranch.emitQuad(
                chunkInstance,
                subChunkInstance,
                rotationPaletteHandle,
                verts,
                vertColorAccumulator,
                toFrontSub(Coordinate3Int.unpackX(xyz), direction3Vector.x),
                toFrontSub(Coordinate3Int.unpackY(xyz), direction3Vector.y),
                toFrontSub(Coordinate3Int.unpackZ(xyz), direction3Vector.z),
                sizeA * divisions,
                sizeB * divisions,
                divisions,
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
            int xyz,
            Direction3Vector direction3Vector,
            Direction3Vector expandDirection,
            Direction3Vector tangentDirection,
            int currentSize,
            int tangentSize,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short baseOrientation,
            BitSet accumulatedBatch,
            BitSet batchReturn) {

        batchReturn.clear();

        if (currentSize >= maxMergeExtent)
            return false;

        int nextXYZ = ChunkCoordinate3Int.getNeighborWithOffset(xyz, expandDirection, currentSize);

        if (nextXYZ == -1)
            return false;

        boolean orientationSensitive = subCellSampleBranch.requiresOrientationMatch(blockHandle);

        for (int i = 0; i < tangentSize; i++) {

            int checkXYZ = ChunkCoordinate3Int.getNeighborWithOffset(nextXYZ, tangentDirection, i);

            if (checkXYZ == -1 || !canMerge(
                    chunkInstance,
                    subChunkInstance,
                    biomePaletteHandle,
                    blockPaletteHandle,
                    rotationPaletteHandle,
                    checkXYZ,
                    direction3Vector,
                    biomeHandle,
                    blockHandle,
                    orientationSensitive,
                    baseOrientation,
                    accumulatedBatch)) {
                batchReturn.clear();
                return false;
            }

            batchReturn.set(ChunkCoordinate3Int.getIndex(checkXYZ));
        }

        return true;
    }

    private boolean canMerge(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            int checkXYZ,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            boolean orientationSensitive,
            short baseOrientation,
            BitSet accumulatedBatch) {

        if (accumulatedBatch.get(ChunkCoordinate3Int.getIndex(checkXYZ)))
            return false;

        if (biomeManager.getBiomeHandleFromBiomeID(biomePaletteHandle.getBlock(checkXYZ)) != biomeHandle)
            return false;

        if (blockManager.getBlockHandleFromBlockID(blockPaletteHandle.getBlock(checkXYZ)) != blockHandle)
            return false;

        if (blockPaletteHandle.getSubBlockMask(checkXYZ) != SubBlockUtility.MASK_FULL)
            return false;

        if (orientationSensitive && rotationPaletteHandle.getBlock(checkXYZ) != baseOrientation)
            return false;

        if (!blockHasFace(chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle))
            return false;

        return subCellSampleBranch.isBlockSimple(
                chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle);
    }
}
