package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry;

import java.util.BitSet;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import com.internal.bootstrap.worldpipeline.subchunk.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Coordinate3Short;
import com.internal.core.util.mathematics.Extras.Direction2Vector;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class FullGeometryBranch extends BranchPackage {

    // Internal
    private BiomeManager biomeManager;
    private BlockManager blockManager;

    // Data
    private SubChunkInstance ERROR;
    private static final int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;

    // Internal \\

    @Override
    protected void create() {

        // Data
        this.ERROR = create(SubChunkInstance.class);
    }

    @Override
    protected void get() {

        // Internal
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Full Geometry Builder \\

    public Boolean assembleQuads(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            short xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            FloatArrayList quads,
            BitSet batchReturn) {

        if (!blockHasFace(
                chunkInstance,
                subChunkInstance,
                xyz,
                direction3Vector,
                biomeHandle,
                blockHandle))
            return false;

        return assembleQuad(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                blockPaletteHandle,
                xyz,
                direction3Vector,
                biomeHandle,
                blockHandle,
                quads,
                batchReturn);
    }

    private boolean blockHasFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            short xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle) {

        SubChunkInstance comparativeSubChunkCoordinate = getComparativeSubChunkInstance(
                chunkInstance,
                subChunkInstance,
                xyz,
                direction3Vector);
        if (comparativeSubChunkCoordinate == ERROR)
            return false;

        short comparativeXYZ = Coordinate3Short.getNeighborAndWrap(xyz, direction3Vector);

        BlockPaletteHandle comparativeBiomePaletteHandle = comparativeSubChunkCoordinate.getBiomePaletteHandle();
        short comparativeBiomeID = comparativeBiomePaletteHandle.getBlock(comparativeXYZ);
        BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeFromBiomeID(comparativeBiomeID);

        BlockPaletteHandle comparativeBlockPaletteHandle = comparativeSubChunkCoordinate.getBlockPaletteHandle();
        short comparativeBlockID = comparativeBlockPaletteHandle.getBlock(comparativeXYZ);
        BlockHandle comparativeBlockHandle = blockManager.getBlockFromBlockID(comparativeBlockID);

        return compareNeighbor(
                biomeHandle,
                comparativeBiomeHandle,
                blockHandle,
                comparativeBlockHandle);
    }

    private SubChunkInstance getComparativeSubChunkInstance(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            short xyz,
            Direction3Vector direction3Vector) {

        // If the coordinate is within the sa,e
        if (!Coordinate3Short.isAtEdge(xyz))
            return subChunkInstance;

        byte subChunkCoordinate = subChunkInstance.getSubChunkCoordinate();

        // Handle vertical chunk comparison
        if (direction3Vector == Direction3Vector.UP ||
                direction3Vector == Direction3Vector.DOWN) {

            // Add the `subChunkCoordinate` and the `direction3Vector.y`
            byte comparativeSubChunkCoordinate = (byte) (subChunkCoordinate + direction3Vector.y);

            // If `comparativeSubChunkCoordinate` if in range `getSubChunk` and return
            if (comparativeSubChunkCoordinate >= 0 && comparativeSubChunkCoordinate < EngineSetting.WORLD_HEIGHT)
                return chunkInstance.getSubChunk(comparativeSubChunkCoordinate);

            else // Else return null
                return null;
        }

        // Flatten the `direction3Vector`
        Direction2Vector direction2Vector = direction3Vector.to2D();

        // Get the neighbor `ChunkInstance` reference
        ChunkNeighborStruct chunkNeighborStruct = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborStruct.getNeighborChunk(direction2Vector.index);

        // get the `comparativeSubChunkInstance` reference
        SubChunkInstance comparativeSubChunkInstance = neighborChunkInstance.getSubChunk(subChunkCoordinate);

        // If `comparativeSubChunkInstance` is null return `ERROR`
        if (comparativeSubChunkInstance == null)
            return ERROR;

        return comparativeSubChunkInstance;
    }

    private boolean compareNeighbor(
            BiomeHandle biomeHandleA,
            BiomeHandle biomeHandleB,
            BlockHandle blockHandleA,
            BlockHandle blockHandleB) {

        if (biomeHandleA == biomeHandleB &&
                blockHandleA == blockHandleB)
            return true;

        return false;
    }

    // Face Assembly \\

    private boolean assembleQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            short xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            FloatArrayList quads,
            BitSet batchReturn) {

        // First step start with default values for the check
        boolean checkA = true;
        boolean checkB = true;

        int sizeA = 1;
        int sizeB = 1;

        // Get the tangent directions for the current `Direction3vector`
        Direction3Vector[] tangents = Direction3Vector.getTangents(direction3Vector);
        Direction3Vector comparativeDirectionA = tangents[0];
        Direction3Vector comparativeDirectionB = tangents[1];

        do {

            // expand along A
            if (checkA) {

                if (tryExpand(
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
                        xyz,
                        direction3Vector,
                        comparativeDirectionA, comparativeDirectionB,
                        sizeA, sizeB,
                        biomeHandle,
                        blockHandle,
                        quads,
                        batchReturn))
                    sizeA++;

                else
                    checkA = false;
            }

            // expand along B
            if (checkB) {

                if (tryExpand(
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
                        xyz,
                        direction3Vector,
                        comparativeDirectionB, comparativeDirectionA,
                        sizeB, sizeA,
                        biomeHandle,
                        blockHandle,
                        quads,
                        batchReturn))
                    sizeB++;

                else
                    checkB = false;
            }

        }

        while (checkA || checkB);

        return true;
    }

    // The main method to stretch the face
    private boolean tryExpand(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            short xyz,
            Direction3Vector direction3Vector,
            Direction3Vector expandDirection, Direction3Vector tangentDirection,
            int currentSize, int tangentSize,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            FloatArrayList quads,
            BitSet batchReturn) {

        // First step is to make sure we are within the same chunk
        if (currentSize >= CHUNK_SIZE)
            return false;

        // Calculate next base coordinate along `expandDirection`
        short nextXYZ = Coordinate3Short.getNeighbor(xyz, expandDirection);

        // Used to keep the coordinates within a single chunk
        if (nextXYZ == -1)
            return false;

        // Loop across the perpendicular dimension
        for (int i = 0; i < tangentSize; i++) {

            short checkXYZ = Coordinate3Short.getNeighborWithOffset(nextXYZ, tangentDirection, i);
            if (checkXYZ == -1)
                return false;

            short comparativeBiomeID = biomePaletteHandle.getBlock(checkXYZ);
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeFromBiomeID(comparativeBiomeID);

            short comparativeBlockID = blockPaletteHandle.getBlock(checkXYZ);
            BlockHandle comparativeBlockHandle = blockManager.getBlockFromBlockID(comparativeBlockID);

            if (!compareNeighbor(
                    biomeHandle,
                    comparativeBiomeHandle,
                    blockHandle,
                    comparativeBlockHandle) ||
                    batchReturn.get(checkXYZ) ||
                    !blockHasFace(
                            chunkInstance,
                            subChunkInstance,
                            checkXYZ,
                            direction3Vector,
                            biomeHandle,
                            blockHandle))
                return false;

            batchReturn.set(checkXYZ);
        }

        return true;
    }
}
