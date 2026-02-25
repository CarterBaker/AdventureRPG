package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry;

import java.util.BitSet;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.UVRect;
import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.block.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.block.BlockRotationType;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import com.internal.core.engine.BranchPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Color;
import com.internal.core.util.mathematics.Extras.Coordinate3Int;
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
        this.ERROR = create(SubChunkInstance.class);
    }

    @Override
    protected void get() {
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
            BlockPaletteHandle rotationPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            int xyz,
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
                rotationPaletteHandle,
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
            int xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle) {

        SubChunkInstance comparativeSubChunkInstance = getComparativeSubChunkInstance(
                chunkInstance,
                subChunkInstance,
                xyz,
                direction3Vector);

        if (comparativeSubChunkInstance == ERROR)
            return false;

        if (comparativeSubChunkInstance == null) {
            byte subY = (byte) subChunkInstance.getCoordinate();
            if ((direction3Vector == Direction3Vector.DOWN && subY == 0) ||
                    (direction3Vector == Direction3Vector.UP && subY == WORLD_HEIGHT - 1))
                return true;
            return false;
        }

        int comparativeXYZ = ChunkCoordinate3Int.getNeighborAndWrap(xyz, direction3Vector);

        BlockPaletteHandle comparativeBlockPaletteHandle = comparativeSubChunkInstance.getBlockPaletteHandle();
        short comparativeBlockID = comparativeBlockPaletteHandle.getBlock(comparativeXYZ);
        BlockHandle comparativeBlockHandle = blockManager.getBlockFromBlockID(comparativeBlockID);

        return compareNeighbor(blockHandle, comparativeBlockHandle);
    }

    private SubChunkInstance getComparativeSubChunkInstance(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector) {

        if (!ChunkCoordinate3Int.isAtEdge(xyz, direction3Vector))
            return subChunkInstance;

        byte subChunkCoordinate = (byte) subChunkInstance.getCoordinate();

        if (direction3Vector == Direction3Vector.UP ||
                direction3Vector == Direction3Vector.DOWN) {

            byte comparativeSubChunkCoordinate = (byte) (subChunkCoordinate + direction3Vector.y);

            if (comparativeSubChunkCoordinate >= 0 && comparativeSubChunkCoordinate < EngineSetting.WORLD_HEIGHT)
                return chunkInstance.getSubChunk(comparativeSubChunkCoordinate);
            else
                return null;
        }

        Direction2Vector direction2Vector = direction3Vector.to2D();
        ChunkNeighborStruct chunkNeighborStruct = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborStruct.getNeighborChunk(direction2Vector.index);

        SubChunkInstance comparativeSubChunkInstance = neighborChunkInstance.getSubChunk(subChunkCoordinate);
        if (comparativeSubChunkInstance == null)
            return ERROR;

        return comparativeSubChunkInstance;
    }

    private boolean compareNeighbor(
            BlockHandle blockHandleA,
            BlockHandle blockHandleB) {
        return (blockHandleA.getGeometry() != blockHandleB.getGeometry());
    }

    // Greedy Expansion \\

    private boolean assembleQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            BitSet accumulatedBatch,
            BitSet batchReturn,
            Color[] vertColors) {

        boolean checkA = true;
        boolean checkB = true;
        byte sizeA = 1;
        byte sizeB = 1;

        Direction3Vector[] tangents = Direction3Vector.getTangents(direction3Vector);
        Direction3Vector comparativeDirectionA = tangents[0];
        Direction3Vector comparativeDirectionB = tangents[1];

        // Read this block's orientation once — used in expansion comparison
        short baseOrientation = rotationPaletteHandle.getBlock(xyz);

        do {

            // Expand along A
            if (checkA) {
                if (tryExpand(
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
                        rotationPaletteHandle,
                        xyz,
                        direction3Vector,
                        comparativeDirectionA, comparativeDirectionB,
                        sizeA, sizeB,
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

            // Expand along B
            if (checkB) {
                if (tryExpand(
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
                        rotationPaletteHandle,
                        xyz,
                        direction3Vector,
                        comparativeDirectionB, comparativeDirectionA,
                        sizeB, sizeA,
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

        return prepareFace(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                blockPaletteHandle,
                rotationPaletteHandle,
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
            int currentSize, int tangentSize,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short baseOrientation,
            BitSet accumulatedBatch,
            BitSet batchReturn) {

        if (currentSize >= CHUNK_SIZE)
            return false;

        int nextXYZ = ChunkCoordinate3Int.getNeighborWithOffset(xyz, expandDirection, currentSize);
        if (nextXYZ == -1)
            return false;

        for (int i = 0; i < tangentSize; i++) {

            int checkXYZ = ChunkCoordinate3Int.getNeighborWithOffset(nextXYZ, tangentDirection, i);
            if (checkXYZ == -1)
                return false;

            short comparativeBiomeID = biomePaletteHandle.getBlock(checkXYZ);
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeFromBiomeID(comparativeBiomeID);

            short comparativeBlockID = blockPaletteHandle.getBlock(checkXYZ);
            BlockHandle comparativeBlockHandle = blockManager.getBlockFromBlockID(comparativeBlockID);

            // Read the candidate block's orientation
            short comparativeOrientation = rotationPaletteHandle.getBlock(checkXYZ);

            if (!compareNext(
                    biomeHandle,
                    comparativeBiomeHandle,
                    blockHandle,
                    comparativeBlockHandle,
                    baseOrientation,
                    comparativeOrientation) ||
                    accumulatedBatch.get(ChunkCoordinate3Int.getIndex(checkXYZ)) ||
                    !blockHasFace(
                            chunkInstance,
                            subChunkInstance,
                            checkXYZ,
                            direction3Vector,
                            comparativeBiomeHandle,
                            comparativeBlockHandle)) {
                batchReturn.clear();
                return false;
            }

            batchReturn.set(ChunkCoordinate3Int.getIndex(checkXYZ));
        }

        return true;
    }

    private boolean compareNext(
            BiomeHandle biomeHandleA,
            BiomeHandle biomeHandleB,
            BlockHandle blockHandleA,
            BlockHandle blockHandleB,
            short orientationA,
            short orientationB) {

        // Biome and block type must match as before
        if (biomeHandleA != biomeHandleB || blockHandleA != blockHandleB)
            return false;

        // Non-rotatable blocks always batch regardless of palette value
        if (blockHandleA.getRotationType() == BlockRotationType.NONE)
            return true;

        // Rotatable blocks must have identical orientations to batch
        return orientationA == orientationB;
    }

    // Face Preparation \\

    private boolean prepareFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            int xyz,
            byte sizeA, byte sizeB,
            Direction3Vector direction3Vector,
            Direction3Vector tangentDirectionA,
            Direction3Vector tangentDirectionB,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            Color[] vertColors) {

        // Vert positions
        int vert0XYZ = ChunkCoordinate3Int.convertToVertSpace(xyz, direction3Vector);
        int vert1XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentDirectionA, sizeA);
        int vert2XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert1XYZ, tangentDirectionB, sizeB);
        int vert3XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentDirectionB, sizeB);

        // Vert colors
        float vert0Color = getVertColor(chunkInstance, subChunkInstance, vert0XYZ, vertColors);
        float vert1Color = getVertColor(chunkInstance, subChunkInstance, vert1XYZ, vertColors);
        float vert2Color = getVertColor(chunkInstance, subChunkInstance, vert2XYZ, vertColors);
        float vert3Color = getVertColor(chunkInstance, subChunkInstance, vert3XYZ, vertColors);

        int materialID = blockHandle.getMaterialID();
        int orientation = resolveOrientation(rotationPaletteHandle, xyz);
        int textureID = resolveTextureID(blockHandle, direction3Vector, orientation);
        int encodedFace = resolveEncodedFace(blockHandle, direction3Vector, orientation);

        return finalizeFace(
                verts,
                dynamicPacketInstance,
                direction3Vector,
                materialID,
                textureID,
                vert0XYZ, vert1XYZ, vert2XYZ, vert3XYZ,
                vert0Color, vert1Color, vert2Color, vert3Color,
                encodedFace);
    }

    // Reads raw orientation from palette and masks to int
    private int resolveOrientation(BlockPaletteHandle rotationPaletteHandle, int xyz) {
        return rotationPaletteHandle.getBlock(xyz) & 0xFFFF;
    }

    // Returns the texture ID for this world face given block orientation
    private int resolveTextureID(BlockHandle blockHandle, Direction3Vector worldFace, int orientation) {
        BlockRotationType rot = blockHandle.getRotationType();
        if (rot == BlockRotationType.NONE || rot == BlockRotationType.NATURAL_FULL)
            return blockHandle.getTextureForFace(worldFace);
        Direction3Vector textureFace = Direction3Vector.VALUES[Direction3Vector.getEncodedFace(orientation, worldFace)
                / 4];
        return blockHandle.getTextureForFace(textureFace);
    }

    // Returns the encoded face value (0-23) for the UBO lookup in the shader
    private int resolveEncodedFace(BlockHandle blockHandle, Direction3Vector worldFace, int orientation) {
        BlockRotationType rot = blockHandle.getRotationType();
        if (rot == BlockRotationType.NONE)
            return worldFace.ordinal() * 4;
        if (rot == BlockRotationType.NATURAL_FULL) {
            if (worldFace == Direction3Vector.UP || worldFace == Direction3Vector.DOWN)
                return 24 + worldFace.ordinal(); // sentinels 28 (UP=4) and 29 (DOWN=5)
            else
                return worldFace.ordinal() * 4; // sides always spin 0, upright as authored
        }
        return Direction3Vector.getEncodedFace(orientation, worldFace);
    }

    // Vert Color \\

    private float getVertColor(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int vertXYZ,
            Color[] vertColors) {

        for (int i = 0; i < 8; i++) {

            VertBlockNeighbor3Vector blockDirection3Vector = VertBlockNeighbor3Vector.VALUES[i];
            int offsetXYZ = ChunkCoordinate3Int.getNeighborFromVert(vertXYZ, blockDirection3Vector);
            int blockXYZ = ChunkCoordinate3Int.convertToBlockSpace(offsetXYZ, blockDirection3Vector);

            SubChunkInstance comparativeSubChunkInstance = getComparativeSubChunkInstance(
                    chunkInstance,
                    subChunkInstance,
                    blockXYZ,
                    blockDirection3Vector);

            if (comparativeSubChunkInstance == null ||
                    comparativeSubChunkInstance == ERROR) {
                vertColors[i] = Color.WHITE;
                continue;
            }

            BlockPaletteHandle comparativeBiomePaletteHandle = comparativeSubChunkInstance.getBiomePaletteHandle();
            short comparativeBiomeID = comparativeBiomePaletteHandle.getBlock(blockXYZ);
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeFromBiomeID(comparativeBiomeID);

            vertColors[i] = comparativeBiomeHandle.getBiomeColor();
        }

        return blendColors(vertColors);
    }

    private SubChunkInstance getComparativeSubChunkInstance(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            VertBlockNeighbor3Vector vertBlockNeighbor3Vector) {

        if (!ChunkCoordinate3Int.isAtEdge(xyz, vertBlockNeighbor3Vector))
            return subChunkInstance;

        int x = Coordinate3Int.unpackX(xyz);
        int y = Coordinate3Int.unpackY(xyz);
        int z = Coordinate3Int.unpackZ(xyz);

        short subChunkCoordinate = (byte) subChunkInstance.getCoordinate();

        if (y < 0) {
            if (subChunkCoordinate == 0)
                return null;
            subChunkCoordinate -= 1;
        } else if (y >= CHUNK_SIZE) {
            if (subChunkCoordinate == (WORLD_HEIGHT - 1))
                return null;
            subChunkCoordinate += 1;
        }

        boolean needsHorizontalNeighbor = (x < 0 || x >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE);

        if (!needsHorizontalNeighbor)
            return chunkInstance.getSubChunk(subChunkCoordinate);

        Direction2Vector direction2Vector = vertBlockNeighbor3Vector.to2D();
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

    // Face Finalization \\

    private boolean finalizeFace(
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            DynamicPacketInstance dynamicPacketInstance,
            Direction3Vector direction3Vector,
            int materialId,
            int textureID,
            int vert0XYZ, int vert1XYZ, int vert2XYZ, int vert3XYZ,
            float vert0Color, float vert1Color, float vert2Color, float vert3Color,
            int encodedFace) {

        FloatArrayList buffer = verts.computeIfAbsent(materialId, k -> new FloatArrayList());

        UVRect uvRect = textureManager.getTextureArrayUVfromTileID(textureID);
        float nor = (float) direction3Vector.index;
        float fEncodedFace = (float) encodedFace;

        // Layout: POS(3) NOR(1) COL(1) UV(2) ORIENT(1) SPARE(1) = 9 floats
        // ORIENT slot is now unused (was spin, now encoded in spare)
        // SPARE slot carries encodedFace (0-23) for UBO lookup

        // Vert 0
        buffer.add((float) Coordinate3Int.unpackX(vert0XYZ));
        buffer.add((float) Coordinate3Int.unpackY(vert0XYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vert0XYZ));
        buffer.add(nor);
        buffer.add(vert0Color);
        buffer.add(uvRect.u0);
        buffer.add(uvRect.v0);
        buffer.add(fEncodedFace);
        buffer.add(0f);

        // Vert 1
        buffer.add((float) Coordinate3Int.unpackX(vert1XYZ));
        buffer.add((float) Coordinate3Int.unpackY(vert1XYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vert1XYZ));
        buffer.add(nor);
        buffer.add(vert1Color);
        buffer.add(uvRect.u0);
        buffer.add(uvRect.v0);
        buffer.add(fEncodedFace);
        buffer.add(0f);

        // Vert 2
        buffer.add((float) Coordinate3Int.unpackX(vert2XYZ));
        buffer.add((float) Coordinate3Int.unpackY(vert2XYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vert2XYZ));
        buffer.add(nor);
        buffer.add(vert2Color);
        buffer.add(uvRect.u0);
        buffer.add(uvRect.v0);
        buffer.add(fEncodedFace);
        buffer.add(0f);

        // Vert 3
        buffer.add((float) Coordinate3Int.unpackX(vert3XYZ));
        buffer.add((float) Coordinate3Int.unpackY(vert3XYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vert3XYZ));
        buffer.add(nor);
        buffer.add(vert3Color);
        buffer.add(uvRect.u0);
        buffer.add(uvRect.v0);
        buffer.add(fEncodedFace);
        buffer.add(0f);

        return true;
    }
}