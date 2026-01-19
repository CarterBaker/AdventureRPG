package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry;

import java.util.BitSet;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.BlockDirection3Vector;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.Coordinate3Short;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.UVRect;
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
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class FullGeometryBranch extends BranchPackage {

    // Internal
    private TextureManager textureManager;
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
        this.textureManager = get(TextureManager.class);
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Face Verification \\

    public boolean assembleQuads(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            short xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            BitSet accumulatedBatch,
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
                dynamicPacketInstance,
                xyz,
                direction3Vector,
                biomeHandle,
                blockHandle,
                verts,
                accumulatedBatch,
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
        return (biomeHandleA == biomeHandleB &&
                blockHandleA == blockHandleB);
    }

    // Greedy Expansion \\

    private boolean assembleQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            short xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            BitSet accumulatedBatch,
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
                        accumulatedBatch,
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
                        accumulatedBatch,
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
                dynamicPacketInstance,
                xyz,
                sizeA, sizeB,
                direction3Vector,
                comparativeDirectionA, comparativeDirectionB,
                biomeHandle,
                blockHandle,
                verts,
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
            BitSet accumulatedBatch,
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
                    accumulatedBatch.get(checkXYZ) ||
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
            DynamicPacketInstance dynamicPacketInstance,
            short xyz,
            byte sizeA, byte sizeB,
            Direction3Vector direction3Vector,
            Direction3Vector tangentDirectionA, Direction3Vector tangentDirectionB,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            Color[] vertColors) {

        // Convert block to vert space (vert0)
        short vert0XYZ = Coordinate3Short.convertToVertSpace(xyz, direction3Vector);

        // base + width (vert1)
        short vert1XYZ = Coordinate3Short.getNeighborWithOffsetFromVert(vert0XYZ, tangentDirectionA, sizeA);

        // base + width + height (vert2)
        short vert2XYZ = Coordinate3Short.getNeighborWithOffsetFromVert(vert1XYZ, tangentDirectionB, sizeB);

        // base + height (vert3)
        short vert3XYZ = Coordinate3Short.getNeighborWithOffsetFromVert(vert0XYZ, tangentDirectionB, sizeB);

        float vert0Color = getVertColor(
                chunkInstance,
                subChunkInstance,
                vert0XYZ,
                vertColors);

        float vert1Color = getVertColor(
                chunkInstance,
                subChunkInstance,
                vert1XYZ,
                vertColors);

        float vert2Color = getVertColor(
                chunkInstance,
                subChunkInstance,
                vert2XYZ,
                vertColors);

        float vert3Color = getVertColor(
                chunkInstance,
                subChunkInstance,
                vert3XYZ,
                vertColors);

        int materialID = blockHandle.getMaterialID();
        int textureID = blockHandle.getTextureForFace(direction3Vector);

        return finalizeFace(
                verts,
                dynamicPacketInstance,
                direction3Vector,
                materialID,
                textureID,
                vert0XYZ,
                vert1XYZ,
                vert2XYZ,
                vert3XYZ,
                vert0Color,
                vert1Color,
                vert2Color,
                vert3Color);
    }

    private float getVertColor(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            short vertXYZ,
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

        return blendColors(vertColors);
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

    private float blendColors(Color[] vertColors) {

        float r = 0, g = 0, b = 0, a = 0;
        int count = 0;

        for (Color c : vertColors) {
            if (c != null) {
                r += c.r;
                g += c.g;
                b += c.b;
                a += c.a;
                count++;
            }
        }

        if (count == 0)
            return Color.rgba8888(Color.WHITE);

        r /= count;
        g /= count;
        b /= count;
        a /= count;

        return Color.rgba8888(r, g, b, a);
    }

    private boolean finalizeFace(
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            DynamicPacketInstance dynamicPacketInstance,
            Direction3Vector direction3Vector,
            int materialId,
            int textureID,
            short vert0XYZ,
            short vert1XYZ,
            short vert2XYZ,
            short vert3XYZ,
            float vert0Color,
            float vert1Color,
            float vert2Color,
            float vert3Color) {

        // Get or create buffer for this material
        FloatArrayList buffer = verts.computeIfAbsent(materialId, k -> new FloatArrayList());

        // Normal is just the direction
        float nx = (float) direction3Vector.x;
        float ny = (float) direction3Vector.y;
        float nz = (float) direction3Vector.z;

        // Get UV coordinates directly from texture atlas
        UVRect uvRect = textureManager.getTextureArrayUVfromTileID(textureID);

        // Vertex 0 (bottom-left corner)
        buffer.add((float) Coordinate3Short.unpackX(vert0XYZ));
        buffer.add((float) Coordinate3Short.unpackY(vert0XYZ));
        buffer.add((float) Coordinate3Short.unpackZ(vert0XYZ));
        buffer.add(nx);
        buffer.add(ny);
        buffer.add(nz);
        buffer.add(vert0Color);
        buffer.add(uvRect.u0);
        buffer.add(uvRect.v0);

        // Vertex 1 (bottom-right corner)
        buffer.add((float) Coordinate3Short.unpackX(vert1XYZ));
        buffer.add((float) Coordinate3Short.unpackY(vert1XYZ));
        buffer.add((float) Coordinate3Short.unpackZ(vert1XYZ));
        buffer.add(nx);
        buffer.add(ny);
        buffer.add(nz);
        buffer.add(vert1Color);
        buffer.add(uvRect.u1);
        buffer.add(uvRect.v0);

        // Vertex 2 (top-right corner)
        buffer.add((float) Coordinate3Short.unpackX(vert2XYZ));
        buffer.add((float) Coordinate3Short.unpackY(vert2XYZ));
        buffer.add((float) Coordinate3Short.unpackZ(vert2XYZ));
        buffer.add(nx);
        buffer.add(ny);
        buffer.add(nz);
        buffer.add(vert2Color);
        buffer.add(uvRect.u1);
        buffer.add(uvRect.v1);

        // Vertex 3 (top-left corner)
        buffer.add((float) Coordinate3Short.unpackX(vert3XYZ));
        buffer.add((float) Coordinate3Short.unpackY(vert3XYZ));
        buffer.add((float) Coordinate3Short.unpackZ(vert3XYZ));
        buffer.add(nx);
        buffer.add(ny);
        buffer.add(nz);
        buffer.add(vert3Color);
        buffer.add(uvRect.u0);
        buffer.add(uvRect.v1);

        return true;
    }
}
