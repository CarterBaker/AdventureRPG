package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.block.BlockRotationType;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Coordinate3Int;
import engine.util.mathematics.extras.Direction2Vector;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

// Geometry branch for full-cube blocks. Greedily merges faces into quads (capped at MAX_MERGE_EXTENT so
// every per-edge state word stays inside a float32 mantissa and every tessellation level stays under the
// hardware clamp), samples AO vertex colors, resolves rotation-aware texture and face encoding, and
// classifies every edge cell of the resulting quad into one of four states the tessellation evaluator
// consumes directly: INTERIOR (the surface continues past this edge, no fade, no bevel), BOUNDARY (the
// surface ends but nothing bevels), CONVEX, or CONCAVE. Classification is deliberately symmetric — both
// faces meeting at any physical edge evaluate the same two block lookups in the same order and therefore
// always agree on that edge's state, which is what lets the shader converge both faces onto one point.
class FullGeometryBranch extends BranchPackage {

    private TextureManager textureManager;
    private BiomeManager biomeManager;
    private BlockManager blockManager;

    private SubChunkInstance ERROR;

    private static final int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    private static final int WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;

    private static final int MAX_MERGE_EXTENT = 12;

    private static final int EDGE_STATE_INTERIOR = 0;
    private static final int EDGE_STATE_BOUNDARY = 1;
    private static final int EDGE_STATE_CONVEX = 2;
    private static final int EDGE_STATE_CONCAVE = 3;

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

    boolean assembleQuads(
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
        BlockHandle comparativeBlockHandle = blockManager.getBlockHandleFromBlockID(comparativeBlockID);

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

    private boolean compareNeighbor(BlockHandle blockHandleA, BlockHandle blockHandleB) {
        return blockHandleA.getGeometry() != blockHandleB.getGeometry();
    }

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

        Direction3Vector comparativeDirectionA = Direction3Vector.getTangentA(direction3Vector);
        Direction3Vector comparativeDirectionB = Direction3Vector.getTangentB(direction3Vector);

        short baseOrientation = rotationPaletteHandle.getBlock(xyz);

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
                        comparativeDirectionA,
                        comparativeDirectionB,
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
                        comparativeDirectionB,
                        comparativeDirectionA,
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

        return prepareFace(
                chunkInstance,
                subChunkInstance,
                rotationPaletteHandle,
                dynamicPacketInstance,
                xyz,
                sizeA,
                sizeB,
                direction3Vector,
                comparativeDirectionA,
                comparativeDirectionB,
                blockHandle,
                baseOrientation,
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
            int currentSize,
            int tangentSize,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short baseOrientation,
            BitSet accumulatedBatch,
            BitSet batchReturn) {

        if (currentSize >= MAX_MERGE_EXTENT || currentSize >= CHUNK_SIZE)
            return false;

        int nextXYZ = ChunkCoordinate3Int.getNeighborWithOffset(xyz, expandDirection, currentSize);

        if (nextXYZ == -1)
            return false;

        for (int i = 0; i < tangentSize; i++) {

            int checkXYZ = ChunkCoordinate3Int.getNeighborWithOffset(nextXYZ, tangentDirection, i);

            if (checkXYZ == -1)
                return false;

            short comparativeBiomeID = biomePaletteHandle.getBlock(checkXYZ);
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeHandleFromBiomeID(comparativeBiomeID);
            short comparativeBlockID = blockPaletteHandle.getBlock(checkXYZ);
            BlockHandle comparativeBlockHandle = blockManager.getBlockHandleFromBlockID(comparativeBlockID);
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

        if (biomeHandleA != biomeHandleB || blockHandleA != blockHandleB)
            return false;

        if (blockHandleA.getRotationType() == BlockRotationType.NONE)
            return true;

        return orientationA == orientationB;
    }

    private boolean prepareFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            int xyz,
            byte sizeA,
            byte sizeB,
            Direction3Vector direction3Vector,
            Direction3Vector tangentDirectionA,
            Direction3Vector tangentDirectionB,
            BlockHandle blockHandle,
            short baseOrientation,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            Color[] vertColors) {

        int iSizeA = sizeA & 0xFF;
        int iSizeB = sizeB & 0xFF;

        int vert0XYZ = ChunkCoordinate3Int.convertToVertSpace(xyz, direction3Vector);
        int vert1XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentDirectionA, sizeA);
        int vert2XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert1XYZ, tangentDirectionB, sizeB);
        int vert3XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentDirectionB, sizeB);

        float vert0Color = getVertColor(chunkInstance, subChunkInstance, vert0XYZ, vertColors);
        float vert1Color = getVertColor(chunkInstance, subChunkInstance, vert1XYZ, vertColors);
        float vert2Color = getVertColor(chunkInstance, subChunkInstance, vert2XYZ, vertColors);
        float vert3Color = getVertColor(chunkInstance, subChunkInstance, vert3XYZ, vertColors);

        int materialID = blockHandle.getMaterialID();
        int orientation = resolveOrientation(rotationPaletteHandle, xyz);
        int textureID = resolveTextureID(blockHandle, direction3Vector, orientation);
        TextureHandle textureHandle = textureManager.getTextureHandleFromTileID(textureID);
        int encodedFace = resolveEncodedFace(blockHandle, direction3Vector, orientation);

        Direction3Vector oppA = Direction3Vector.getOpposite(tangentDirectionA);
        Direction3Vector oppB = Direction3Vector.getOpposite(tangentDirectionB);

        int baseA1 = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionA, iSizeA - 1);
        int baseB1 = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionB, iSizeB - 1);

        int edgeA0 = buildEdgeWord(chunkInstance, subChunkInstance, rotationPaletteHandle,
                xyz, tangentDirectionB, iSizeB, direction3Vector, oppA, blockHandle, baseOrientation);

        int edgeA1 = buildEdgeWord(chunkInstance, subChunkInstance, rotationPaletteHandle,
                baseA1, tangentDirectionB, iSizeB, direction3Vector, tangentDirectionA, blockHandle,
                baseOrientation);

        int edgeB0 = buildEdgeWord(chunkInstance, subChunkInstance, rotationPaletteHandle,
                xyz, tangentDirectionA, iSizeA, direction3Vector, oppB, blockHandle, baseOrientation);

        int edgeB1 = buildEdgeWord(chunkInstance, subChunkInstance, rotationPaletteHandle,
                baseB1, tangentDirectionA, iSizeA, direction3Vector, tangentDirectionB, blockHandle,
                baseOrientation);

        int meta = (direction3Vector.index & 0x7)
                | ((encodedFace & 0x3F) << 3)
                | (((iSizeA - 1) & 0xF) << 9)
                | (((iSizeB - 1) & 0xF) << 13)
                | ((blockHandle.isNatural() ? 1 : 0) << 17);

        return finalizeFace(
                verts,
                dynamicPacketInstance,
                materialID, textureHandle,
                vert0XYZ, vert1XYZ, vert2XYZ, vert3XYZ,
                vert0Color, vert1Color, vert2Color, vert3Color,
                meta,
                edgeA0, edgeA1, edgeB0, edgeB1);
    }

    private int buildEdgeWord(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            int originXYZ,
            Direction3Vector runDirection,
            int runLength,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle,
            short baseOrientation) {

        int word = 0;

        for (int i = 0; i < runLength; i++) {

            int cellXYZ = (originXYZ == -1)
                    ? -1
                    : ChunkCoordinate3Int.getNeighborWithOffset(originXYZ, runDirection, i);

            int state = (cellXYZ == -1)
                    ? EDGE_STATE_BOUNDARY
                    : classifyEdgeCell(chunkInstance, subChunkInstance, rotationPaletteHandle,
                            cellXYZ, faceDirection, sideDirection, blockHandle, baseOrientation);

            word |= state << (i * 2);
        }

        return word;
    }

    private int classifyEdgeCell(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            int cellXYZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle,
            short baseOrientation) {

        SubChunkInstance sideSubChunk = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, cellXYZ, sideDirection);

        if (sideSubChunk == ERROR)
            return EDGE_STATE_BOUNDARY;

        if (sideSubChunk == null)
            return blockHandle.isNatural() ? EDGE_STATE_CONVEX : EDGE_STATE_BOUNDARY;

        int sideXYZ = ChunkCoordinate3Int.getNeighborAndWrap(cellXYZ, sideDirection);
        short sideBlockID = sideSubChunk.getBlockPaletteHandle().getBlock(sideXYZ);
        BlockHandle sideBlock = blockManager.getBlockHandleFromBlockID(sideBlockID);

        if (sideBlock.getGeometry() != blockHandle.getGeometry())
            return blockHandle.isNatural() ? EDGE_STATE_CONVEX : EDGE_STATE_BOUNDARY;

        SubChunkInstance diagonalSubChunk = getComparativeSubChunkInstance(
                chunkInstance, sideSubChunk, sideXYZ, faceDirection);

        if (diagonalSubChunk == null || diagonalSubChunk == ERROR)
            return EDGE_STATE_BOUNDARY;

        int diagonalXYZ = ChunkCoordinate3Int.getNeighborAndWrap(sideXYZ, faceDirection);
        short diagonalBlockID = diagonalSubChunk.getBlockPaletteHandle().getBlock(diagonalXYZ);
        BlockHandle diagonalBlock = blockManager.getBlockHandleFromBlockID(diagonalBlockID);

        if (diagonalBlock.getGeometry() == blockHandle.getGeometry())
            return (blockHandle.isNatural() || diagonalBlock.isNatural())
                    ? EDGE_STATE_CONCAVE
                    : EDGE_STATE_BOUNDARY;

        if (sideSubChunk != subChunkInstance || sideBlock != blockHandle)
            return EDGE_STATE_BOUNDARY;

        if (blockHandle.getRotationType() != BlockRotationType.NONE &&
                rotationPaletteHandle.getBlock(sideXYZ) != baseOrientation)
            return EDGE_STATE_BOUNDARY;

        return EDGE_STATE_INTERIOR;
    }

    private int resolveOrientation(BlockPaletteHandle rotationPaletteHandle, int xyz) {
        return rotationPaletteHandle.getBlock(xyz) & 0xFFFF;
    }

    private int resolveTextureID(BlockHandle blockHandle, Direction3Vector worldFace, int orientation) {

        BlockRotationType rot = blockHandle.getRotationType();

        if (rot == BlockRotationType.NONE || rot == BlockRotationType.NATURAL_FULL)
            return blockHandle.getTextureForFace(worldFace);

        Direction3Vector textureFace = Direction3Vector.VALUES[Direction3Vector.getEncodedFace(orientation, worldFace)
                / 4];

        return blockHandle.getTextureForFace(textureFace);
    }

    private int resolveEncodedFace(BlockHandle blockHandle, Direction3Vector worldFace, int orientation) {

        BlockRotationType rot = blockHandle.getRotationType();

        if (rot == BlockRotationType.NONE)
            return worldFace.ordinal() * 4;

        if (rot == BlockRotationType.NATURAL_FULL) {
            if (worldFace == Direction3Vector.UP || worldFace == Direction3Vector.DOWN)
                return EngineSetting.ENCODED_FACE_NATURAL_FULL_OFFSET + worldFace.ordinal();
            else
                return worldFace.ordinal() * 4;
        }

        return Direction3Vector.getEncodedFace(orientation, worldFace);
    }

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
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeHandleFromBiomeID(comparativeBiomeID);

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

        boolean needsHorizontalNeighbor = x < 0 || x >= CHUNK_SIZE || z < 0 || z >= CHUNK_SIZE;

        if (!needsHorizontalNeighbor)
            return chunkInstance.getSubChunk(subChunkCoordinate);

        Direction2Vector direction2Vector = vertBlockNeighbor3Vector.to2D();
        ChunkNeighborHandle chunkNeighborHandle = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborHandle.getNeighborChunk(direction2Vector.index);

        if (neighborChunkInstance == null)
            return ERROR;

        SubChunkInstance comparativeSubChunkInstance = neighborChunkInstance.getSubChunk(subChunkCoordinate);

        if (comparativeSubChunkInstance == null)
            return ERROR;

        return comparativeSubChunkInstance;
    }

    private float blendColors(Color[] vertColors) {

        float r = 0, g = 0, b = 0;
        int count = 0;

        for (Color c : vertColors) {
            if (c != null) {
                r += c.r;
                g += c.g;
                b += c.b;
                count++;
            }
        }

        if (count == 0)
            return (float) 0xFFFFFF;

        int ir = Math.round(Math.min(Math.max(r / count, 0f), 1f) * 255f);
        int ig = Math.round(Math.min(Math.max(g / count, 0f), 1f) * 255f);
        int ib = Math.round(Math.min(Math.max(b / count, 0f), 1f) * 255f);

        return (float) ((ir << 16) | (ig << 8) | ib);
    }

    private boolean finalizeFace(
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            DynamicPacketInstance dynamicPacketInstance,
            int materialId, TextureHandle textureHandle,
            int vert0XYZ, int vert1XYZ, int vert2XYZ, int vert3XYZ,
            float vert0Color, float vert1Color, float vert2Color, float vert3Color,
            int meta,
            int edgeA0, int edgeA1, int edgeB0, int edgeB1) {

        FloatArrayList buffer = verts.computeIfAbsent(materialId, k -> new FloatArrayList());

        float u0 = textureHandle.getU0();
        float v0 = textureHandle.getV0();
        float fMeta = (float) meta;
        float fEdgeA0 = (float) edgeA0;
        float fEdgeA1 = (float) edgeA1;
        float fEdgeB0 = (float) edgeB0;
        float fEdgeB1 = (float) edgeB1;

        pushVert(buffer, vert0XYZ, u0, v0, fMeta, vert0Color, fEdgeA0, fEdgeA1, fEdgeB0, fEdgeB1);
        pushVert(buffer, vert1XYZ, u0, v0, fMeta, vert1Color, fEdgeA0, fEdgeA1, fEdgeB0, fEdgeB1);
        pushVert(buffer, vert2XYZ, u0, v0, fMeta, vert2Color, fEdgeA0, fEdgeA1, fEdgeB0, fEdgeB1);
        pushVert(buffer, vert3XYZ, u0, v0, fMeta, vert3Color, fEdgeA0, fEdgeA1, fEdgeB0, fEdgeB1);

        return true;
    }

    private void pushVert(
            FloatArrayList buffer,
            int vertXYZ,
            float u0, float v0,
            float meta, float color,
            float edgeA0, float edgeA1, float edgeB0, float edgeB1) {

        buffer.add((float) Coordinate3Int.unpackX(vertXYZ));
        buffer.add((float) Coordinate3Int.unpackY(vertXYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vertXYZ));
        buffer.add(u0);
        buffer.add(v0);
        buffer.add(meta);
        buffer.add(color);
        buffer.add(edgeA0);
        buffer.add(edgeA1);
        buffer.add(edgeB0);
        buffer.add(edgeB1);
    }
}