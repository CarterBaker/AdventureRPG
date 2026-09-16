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

// Geometry branch for full-cube blocks. Greedily merges faces into quads, samples biome vertex colors,
// resolves rotation-aware texture and face encoding, and classifies every edge cell of the resulting quad
// into INTERIOR, BOUNDARY, CONVEX or CONCAVE for the tessellation evaluator. Each of the four edge words
// carries two bits per cell for the quad's own run plus one padding cell at each end, so the evaluator can
// interpolate bevel strength between cell centers without ever clamping to a cell the neighbouring face
// would not have read; that padding is what lets a non-natural surface inherit and taper a natural
// neighbour's bevel, and what keeps two faces meeting at a quad's end from resolving different cells.
// Classification reads the naturalness and geometry of the cell it is given rather than the quad's owning
// block, and resolves every chunk and subchunk hop explicitly, so both faces meeting at any physical edge
// evaluate the same lookups in the same order and always agree on that edge's state.
class FullGeometryBranch extends BranchPackage {

    private TextureManager textureManager;
    private BiomeManager biomeManager;
    private BlockManager blockManager;

    private SubChunkInstance ERROR;

    private static final int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    private static final int WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;

    private static final int MAX_MERGE_EXTENT = 10;
    private static final int VERTEX_FLOAT_COUNT = 11;

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

    // Traversal \\

    private ChunkInstance stepChunk(
            ChunkInstance chunkInstance,
            int xyz,
            Direction3Vector direction3Vector) {

        if (chunkInstance == null ||
                direction3Vector.y != 0 ||
                !ChunkCoordinate3Int.isAtEdge(xyz, direction3Vector))
            return chunkInstance;

        Direction2Vector direction2Vector = direction3Vector.to2D();
        ChunkNeighborHandle chunkNeighborHandle = chunkInstance.getChunkNeighbors();

        return chunkNeighborHandle.getNeighborChunk(direction2Vector.index);
    }

    private SubChunkInstance stepSubChunk(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector) {

        if (chunkInstance == null || subChunkInstance == null)
            return null;

        if (!ChunkCoordinate3Int.isAtEdge(xyz, direction3Vector))
            return subChunkInstance;

        int subChunkCoordinate = (byte) subChunkInstance.getCoordinate();

        if (direction3Vector.y != 0) {
            int comparativeSubChunkCoordinate = subChunkCoordinate + direction3Vector.y;
            if (comparativeSubChunkCoordinate < 0 || comparativeSubChunkCoordinate >= WORLD_HEIGHT)
                return null;
            return chunkInstance.getSubChunk((byte) comparativeSubChunkCoordinate);
        }

        Direction2Vector direction2Vector = direction3Vector.to2D();
        ChunkNeighborHandle chunkNeighborHandle = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborHandle.getNeighborChunk(direction2Vector.index);

        if (neighborChunkInstance == null)
            return null;

        return neighborChunkInstance.getSubChunk((byte) subChunkCoordinate);
    }

    private BlockHandle blockAt(SubChunkInstance subChunkInstance, int xyz) {
        short blockID = subChunkInstance.getBlockPaletteHandle().getBlock(xyz);
        return blockManager.getBlockHandleFromBlockID(blockID);
    }

    // Face presence \\

    private boolean blockHasFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle) {

        ChunkInstance comparativeChunkInstance = stepChunk(chunkInstance, xyz, direction3Vector);

        if (comparativeChunkInstance == null)
            return false;

        SubChunkInstance comparativeSubChunkInstance = stepSubChunk(
                chunkInstance, subChunkInstance, xyz, direction3Vector);

        if (comparativeSubChunkInstance == null)
            return direction3Vector.y != 0;

        int comparativeXYZ = ChunkCoordinate3Int.getNeighborAndWrap(xyz, direction3Vector);
        BlockHandle comparativeBlockHandle = blockAt(comparativeSubChunkInstance, comparativeXYZ);

        return comparativeBlockHandle.getGeometry() != blockHandle.getGeometry();
    }

    // Merging \\

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
        int sizeA = 1;
        int sizeB = 1;

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
            int currentSize,
            int tangentSize,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short baseOrientation,
            BitSet accumulatedBatch,
            BitSet batchReturn) {

        batchReturn.clear();

        if (currentSize >= MAX_MERGE_EXTENT)
            return false;

        int nextXYZ = ChunkCoordinate3Int.getNeighborWithOffset(xyz, expandDirection, currentSize);

        if (nextXYZ == -1)
            return false;

        boolean orientationSensitive = requiresOrientationMatch(blockHandle);

        for (int i = 0; i < tangentSize; i++) {

            int checkXYZ = ChunkCoordinate3Int.getNeighborWithOffset(nextXYZ, tangentDirection, i);

            if (checkXYZ == -1) {
                batchReturn.clear();
                return false;
            }

            if (accumulatedBatch.get(ChunkCoordinate3Int.getIndex(checkXYZ))) {
                batchReturn.clear();
                return false;
            }

            short comparativeBiomeID = biomePaletteHandle.getBlock(checkXYZ);
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeHandleFromBiomeID(comparativeBiomeID);

            if (comparativeBiomeHandle != biomeHandle) {
                batchReturn.clear();
                return false;
            }

            short comparativeBlockID = blockPaletteHandle.getBlock(checkXYZ);
            BlockHandle comparativeBlockHandle = blockManager.getBlockHandleFromBlockID(comparativeBlockID);

            if (comparativeBlockHandle != blockHandle) {
                batchReturn.clear();
                return false;
            }

            if (orientationSensitive && rotationPaletteHandle.getBlock(checkXYZ) != baseOrientation) {
                batchReturn.clear();
                return false;
            }

            if (!blockHasFace(chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle)) {
                batchReturn.clear();
                return false;
            }

            batchReturn.set(ChunkCoordinate3Int.getIndex(checkXYZ));
        }

        return true;
    }

    private boolean requiresOrientationMatch(BlockHandle blockHandle) {
        BlockRotationType rotationType = blockHandle.getRotationType();
        return rotationType != BlockRotationType.NONE && rotationType != BlockRotationType.NATURAL_FULL;
    }

    // Edge classification \\

    private int buildEdgeWord(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            int originXYZ,
            Direction3Vector runDirection,
            int runLength,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection) {

        Direction3Vector backDirection = Direction3Vector.getOpposite(runDirection);
        int lastXYZ = ChunkCoordinate3Int.getNeighborWithOffset(originXYZ, runDirection, runLength - 1);

        int word = 0;

        for (int i = -1; i <= runLength; i++) {

            ChunkInstance cellChunkInstance;
            SubChunkInstance cellSubChunkInstance;
            int cellXYZ;

            if (i < 0) {
                cellChunkInstance = stepChunk(chunkInstance, originXYZ, backDirection);
                cellSubChunkInstance = stepSubChunk(chunkInstance, subChunkInstance, originXYZ, backDirection);
                cellXYZ = ChunkCoordinate3Int.getNeighborAndWrap(originXYZ, backDirection);
            } else if (i < runLength) {
                cellChunkInstance = chunkInstance;
                cellSubChunkInstance = subChunkInstance;
                cellXYZ = ChunkCoordinate3Int.getNeighborWithOffset(originXYZ, runDirection, i);
            } else {
                cellChunkInstance = stepChunk(chunkInstance, lastXYZ, runDirection);
                cellSubChunkInstance = stepSubChunk(chunkInstance, subChunkInstance, lastXYZ, runDirection);
                cellXYZ = ChunkCoordinate3Int.getNeighborAndWrap(lastXYZ, runDirection);
            }

            int state = classifyEdgeCell(
                    cellChunkInstance,
                    cellSubChunkInstance,
                    cellXYZ,
                    faceDirection,
                    sideDirection,
                    subChunkInstance,
                    rotationPaletteHandle);

            word |= state << ((i + 1) * 2);
        }

        return word;
    }

    private int classifyEdgeCell(
            ChunkInstance cellChunkInstance,
            SubChunkInstance cellSubChunkInstance,
            int cellXYZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            SubChunkInstance originSubChunkInstance,
            BlockPaletteHandle rotationPaletteHandle) {

        if (cellChunkInstance == null || cellSubChunkInstance == null)
            return EDGE_STATE_BOUNDARY;

        BlockHandle cellBlockHandle = blockAt(cellSubChunkInstance, cellXYZ);

        if (!blockHasFace(cellChunkInstance, cellSubChunkInstance, cellXYZ, faceDirection, cellBlockHandle))
            return EDGE_STATE_BOUNDARY;

        ChunkInstance sideChunkInstance = stepChunk(cellChunkInstance, cellXYZ, sideDirection);

        if (sideChunkInstance == null)
            return EDGE_STATE_BOUNDARY;

        SubChunkInstance sideSubChunkInstance = stepSubChunk(
                cellChunkInstance, cellSubChunkInstance, cellXYZ, sideDirection);

        if (sideSubChunkInstance == null)
            return cellBlockHandle.isNatural() ? EDGE_STATE_CONVEX : EDGE_STATE_BOUNDARY;

        int sideXYZ = ChunkCoordinate3Int.getNeighborAndWrap(cellXYZ, sideDirection);
        BlockHandle sideBlockHandle = blockAt(sideSubChunkInstance, sideXYZ);

        if (sideBlockHandle.getGeometry() != cellBlockHandle.getGeometry())
            return cellBlockHandle.isNatural() ? EDGE_STATE_CONVEX : EDGE_STATE_BOUNDARY;

        ChunkInstance diagonalChunkInstance = stepChunk(sideChunkInstance, sideXYZ, faceDirection);

        if (diagonalChunkInstance == null)
            return EDGE_STATE_BOUNDARY;

        SubChunkInstance diagonalSubChunkInstance = stepSubChunk(
                sideChunkInstance, sideSubChunkInstance, sideXYZ, faceDirection);

        if (diagonalSubChunkInstance != null) {

            int diagonalXYZ = ChunkCoordinate3Int.getNeighborAndWrap(sideXYZ, faceDirection);
            BlockHandle diagonalBlockHandle = blockAt(diagonalSubChunkInstance, diagonalXYZ);

            if (diagonalBlockHandle.getGeometry() == cellBlockHandle.getGeometry())
                return (cellBlockHandle.isNatural() || diagonalBlockHandle.isNatural())
                        ? EDGE_STATE_CONCAVE
                        : EDGE_STATE_BOUNDARY;
        }

        if (sideBlockHandle != cellBlockHandle)
            return EDGE_STATE_BOUNDARY;

        if (requiresOrientationMatch(cellBlockHandle)) {

            if (cellSubChunkInstance != originSubChunkInstance ||
                    sideSubChunkInstance != originSubChunkInstance)
                return EDGE_STATE_BOUNDARY;

            if (rotationPaletteHandle.getBlock(sideXYZ) != rotationPaletteHandle.getBlock(cellXYZ))
                return EDGE_STATE_BOUNDARY;
        }

        return EDGE_STATE_INTERIOR;
    }

    // Emission \\

    private boolean prepareFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            int xyz,
            int sizeA,
            int sizeB,
            Direction3Vector direction3Vector,
            Direction3Vector tangentDirectionA,
            Direction3Vector tangentDirectionB,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            Color[] vertColors) {

        int vert0XYZ = ChunkCoordinate3Int.convertToVertSpace(xyz, direction3Vector);
        int vert1XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentDirectionA, (byte) sizeA);
        int vert2XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert1XYZ, tangentDirectionB, (byte) sizeB);
        int vert3XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentDirectionB, (byte) sizeB);

        float vert0Color = getVertColor(chunkInstance, subChunkInstance, vert0XYZ, biomeHandle, vertColors);
        float vert1Color = getVertColor(chunkInstance, subChunkInstance, vert1XYZ, biomeHandle, vertColors);
        float vert2Color = getVertColor(chunkInstance, subChunkInstance, vert2XYZ, biomeHandle, vertColors);
        float vert3Color = getVertColor(chunkInstance, subChunkInstance, vert3XYZ, biomeHandle, vertColors);

        int materialID = blockHandle.getMaterialID();
        int orientation = rotationPaletteHandle.getBlock(xyz) & 0xFFFF;
        int textureID = resolveTextureID(blockHandle, direction3Vector, orientation);
        TextureHandle textureHandle = textureManager.getTextureHandleFromTileID(textureID);
        int encodedFace = resolveEncodedFace(blockHandle, direction3Vector, orientation);

        Direction3Vector oppA = Direction3Vector.getOpposite(tangentDirectionA);
        Direction3Vector oppB = Direction3Vector.getOpposite(tangentDirectionB);

        int baseA1 = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionA, sizeA - 1);
        int baseB1 = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionB, sizeB - 1);

        int edgeA0 = buildEdgeWord(chunkInstance, subChunkInstance, rotationPaletteHandle,
                xyz, tangentDirectionB, sizeB, direction3Vector, oppA);

        int edgeA1 = buildEdgeWord(chunkInstance, subChunkInstance, rotationPaletteHandle,
                baseA1, tangentDirectionB, sizeB, direction3Vector, tangentDirectionA);

        int edgeB0 = buildEdgeWord(chunkInstance, subChunkInstance, rotationPaletteHandle,
                xyz, tangentDirectionA, sizeA, direction3Vector, oppB);

        int edgeB1 = buildEdgeWord(chunkInstance, subChunkInstance, rotationPaletteHandle,
                baseB1, tangentDirectionA, sizeA, direction3Vector, tangentDirectionB);

        int meta = (direction3Vector.index & 0x7)
                | ((encodedFace & 0x3F) << 3)
                | (((sizeA - 1) & 0xF) << 9)
                | (((sizeB - 1) & 0xF) << 13)
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
            BiomeHandle biomeHandle,
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
                vertColors[i] = biomeHandle.getBiomeColor();
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

        for (int i = 0; i < 8; i++) {
            Color color = vertColors[i];
            r += color.r;
            g += color.g;
            b += color.b;
        }

        int ir = Math.round(Math.min(Math.max(r * 0.125f, 0f), 1f) * 255f);
        int ig = Math.round(Math.min(Math.max(g * 0.125f, 0f), 1f) * 255f);
        int ib = Math.round(Math.min(Math.max(b * 0.125f, 0f), 1f) * 255f);

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
        buffer.ensureCapacity(buffer.size() + VERTEX_FLOAT_COUNT * 4);

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