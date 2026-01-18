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
import com.internal.core.util.mathematics.Extras.Color;
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
    private static final int WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;

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

    // Face Verification \\

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
            BitSet batchReturn,
            Color[] vertColors) {

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
                batchReturn,
                vertColors);
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

        // If the coordinate is within the same `SubChunkInstance` continue
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

    // Greedy Expansion \\

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
            BitSet batchReturn,
            Color[] vertColors) {

        // First step start with default values for the check
        boolean checkA = true;
        boolean checkB = true;

        byte sizeA = 1;
        byte sizeB = 1;

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

        return prepareFace(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                blockPaletteHandle,
                xyz,
                sizeA, sizeB,
                direction3Vector,
                comparativeDirectionA, comparativeDirectionB,
                biomeHandle,
                blockHandle,
                quads,
                vertColors);
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

    // Face preperation \\

    private boolean prepareFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            short xyz,
            byte sizeA, byte sizeB,
            Direction3Vector direction3Vector,
            Direction3Vector tangentDirectionA, Direction3Vector tangentDirectionB,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            FloatArrayList quads,
            Color[] vertColors) {

        // Convert block to vert space
        short vertXYZ = Coordinate3Short.convertToVertSpace(xyz, direction3Vector);

        // base (vert0)
        short vert0XYZ = vertXYZ;

        // base + width (vert1)
        short vert1XYZ = Coordinate3Short.getNeighborWithOffsetFromVert(vertXYZ, tangentDirectionA, sizeA);

        // base + width + height (vert2)
        short vert2XYZ = Coordinate3Short.getNeighborWithOffsetFromVert(vert1XYZ, tangentDirectionB, sizeB);

        // base + height (vert3)
        short vert3XYZ = Coordinate3Short.getNeighborWithOffsetFromVert(vertXYZ, tangentDirectionB, sizeB);

        float color0 = getVertColor(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                xyz,
                vertXYZ,
                direction3Vector,
                biomeHandle,
                vertColors);

        float color1 = getVertColor(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                xyz,
                vertXYZ,
                direction3Vector,
                biomeHandle,
                vertColors);

        float color2 = getVertColor(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                xyz,
                vertXYZ,
                direction3Vector,
                biomeHandle,
                vertColors);

        float color3 = getVertColor(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                xyz,
                vertXYZ,
                direction3Vector,
                biomeHandle,
                vertColors);

        return false;
    }

    private float getVertColor(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            short xyz,
            short vertXYZ,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            Color[] vertColors) {

        for (int i = 0; i < 8; i++) {

            BlockDirection3Vector blockDirection3Vector = BlockDirection3Vector.VALUES[i];
            short offsetXYZ = Coordinate3Short.getNeighborFromVert(vertXYZ, blockDirection3Vector);
            short blockXYZ = Coordinate3Short.convertToBlockSpace(offsetXYZ, blockDirection3Vector);

            SubChunkInstance comparativeSubChunkCoordinate = getComparativeSubChunkInstance(
                    chunkInstance,
                    subChunkInstance,
                    blockXYZ,
                    blockDirection3Vector);
            if (comparativeSubChunkCoordinate == ERROR) {
                vertColors[i] = Color.WHITE;
                continue;
            }

            BlockPaletteHandle comparativeBiomePaletteHandle = comparativeSubChunkCoordinate.getBiomePaletteHandle();
            short comparativeBiomeID = comparativeBiomePaletteHandle.getBlock(blockXYZ);
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeFromBiomeID(comparativeBiomeID);

            vertColors[i] = comparativeBiomeHandle.getBiomeColor();
        }

        return blendColors();
    }

    private SubChunkInstance getComparativeSubChunkInstance(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            short xyz,
            BlockDirection3Vector blockDirection3Vector) {

        // If the coordinate is within the same `SubChunkInstance` continue
        if (!Coordinate3Short.isAtEdge(xyz))
            return subChunkInstance;

        short x = Coordinate3Short.unpackX(xyz);
        short y = Coordinate3Short.unpackY(xyz);
        short z = Coordinate3Short.unpackZ(xyz);

        short subChunkCoordinate = subChunkInstance.getSubChunkCoordinate();

        // Handle vertical movement
        if (y < 0) {

            if (subChunkCoordinate == 0)
                return null;

            subChunkCoordinate -= 1;
        }

        if (y >= CHUNK_SIZE) {

            if (subChunkCoordinate == (WORLD_HEIGHT - 1))
                return null;

            subChunkCoordinate += 1;
        }

        boolean needsHorizontalNeighbor = (x < 0 ||
                x >= CHUNK_SIZE ||
                z < 0 ||
                z >= CHUNK_SIZE);

        if (!needsHorizontalNeighbor)
            return chunkInstance.getSubChunk(subChunkCoordinate);

        Direction2Vector direction2Vector = blockDirection3Vector.to2D();
        ChunkNeighborStruct chunkNeighborStruct = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborStruct.getNeighborChunk(direction2Vector.index);
        if (neighborChunkInstance == null)
            return ERROR;

        SubChunkInstance comparativeSubChunkInstance = neighborChunkInstance.getSubChunk(subChunkCoordinate);
        if (comparativeSubChunkInstance == null)
            return ERROR;

        return comparativeSubChunkInstance;
    }
}
