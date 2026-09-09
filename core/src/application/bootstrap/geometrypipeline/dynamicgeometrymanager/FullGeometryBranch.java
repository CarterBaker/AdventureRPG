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

// Geometry branch for full-cube blocks. Greedily merges faces into quads, samples AO vertex colors, resolves rotation-aware texture and face encoding, and classifies every edge cell as convex, concave, or stretch so the tessellation shader can bevel convex corners inward and fillet concave corners outward. negMask is set for every concave or stretch cell so both faces at any interior corner receive the same outward normal treatment and converge at the same displaced point.
class FullGeometryBranch extends BranchPackage {

    private TextureManager textureManager;
    private BiomeManager biomeManager;
    private BlockManager blockManager;

    private SubChunkInstance ERROR;

    private static final int CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
    private static final int WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;

    private static final int EXPOSURE_NONE = 0;
    private static final int EXPOSURE_CONVEX = 1;
    private static final int EXPOSURE_CONCAVE = 2;
    private static final int EXPOSURE_STRETCH = 3;

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
                biomePaletteHandle,
                blockPaletteHandle,
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
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            BlockPaletteHandle rotationPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            int xyz,
            byte sizeA,
            byte sizeB,
            Direction3Vector direction3Vector,
            Direction3Vector tangentDirectionA,
            Direction3Vector tangentDirectionB,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            Color[] vertColors) {

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

        int iSizeA = sizeA & 0xFF;
        int iSizeB = sizeB & 0xFF;

        Direction3Vector oppA = Direction3Vector.getOpposite(tangentDirectionA);
        Direction3Vector oppB = Direction3Vector.getOpposite(tangentDirectionB);

        int maskA0 = 0, maskA1 = 0, maskB0 = 0, maskB1 = 0;
        int negMaskA0 = 0, negMaskA1 = 0, negMaskB0 = 0, negMaskB1 = 0;

        for (int j = 0; j < iSizeB; j++) {
            int cellXYZ = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionB, j);
            if (cellXYZ == -1)
                continue;
            int exposure = classifyEdgeCell(
                    chunkInstance, subChunkInstance, cellXYZ, direction3Vector, oppA, blockHandle);
            if (exposure == EXPOSURE_NONE)
                continue;
            maskA0 |= (1 << j);
            if (exposure == EXPOSURE_CONCAVE || exposure == EXPOSURE_STRETCH)
                negMaskA0 |= (1 << j);
        }

        int baseA1 = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionA, iSizeA - 1);
        for (int j = 0; j < iSizeB; j++) {
            int cellXYZ = (baseA1 != -1)
                    ? ChunkCoordinate3Int.getNeighborWithOffset(baseA1, tangentDirectionB, j)
                    : -1;
            if (cellXYZ == -1)
                continue;
            int exposure = classifyEdgeCell(
                    chunkInstance, subChunkInstance, cellXYZ, direction3Vector, tangentDirectionA, blockHandle);
            if (exposure == EXPOSURE_NONE)
                continue;
            maskA1 |= (1 << j);
            if (exposure == EXPOSURE_CONCAVE || exposure == EXPOSURE_STRETCH)
                negMaskA1 |= (1 << j);
        }

        for (int i = 0; i < iSizeA; i++) {
            int cellXYZ = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionA, i);
            if (cellXYZ == -1)
                continue;
            int exposure = classifyEdgeCell(
                    chunkInstance, subChunkInstance, cellXYZ, direction3Vector, oppB, blockHandle);
            if (exposure == EXPOSURE_NONE)
                continue;
            maskB0 |= (1 << i);
            if (exposure == EXPOSURE_CONCAVE || exposure == EXPOSURE_STRETCH)
                negMaskB0 |= (1 << i);
        }

        int baseB1 = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionB, iSizeB - 1);
        for (int i = 0; i < iSizeA; i++) {
            int cellXYZ = (baseB1 != -1)
                    ? ChunkCoordinate3Int.getNeighborWithOffset(baseB1, tangentDirectionA, i)
                    : -1;
            if (cellXYZ == -1)
                continue;
            int exposure = classifyEdgeCell(
                    chunkInstance, subChunkInstance, cellXYZ, direction3Vector, tangentDirectionB, blockHandle);
            if (exposure == EXPOSURE_NONE)
                continue;
            maskB1 |= (1 << i);
            if (exposure == EXPOSURE_CONCAVE || exposure == EXPOSURE_STRETCH)
                negMaskB1 |= (1 << i);
        }

        return finalizeFace(
                verts,
                dynamicPacketInstance,
                direction3Vector,
                materialID, textureHandle,
                vert0XYZ, vert1XYZ, vert2XYZ, vert3XYZ,
                vert0Color, vert1Color, vert2Color, vert3Color,
                encodedFace,
                sizeA, sizeB,
                maskA0, maskA1, maskB0, maskB1,
                negMaskA0, negMaskA1, negMaskB0, negMaskB1);
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

    private int classifyEdgeCell(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellXYZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle) {

        if (!blockHandle.isNatural())
            return isStretchExposedOnSide(
                    chunkInstance, subChunkInstance, cellXYZ, faceDirection, sideDirection, blockHandle)
                            ? EXPOSURE_STRETCH
                            : EXPOSURE_NONE;

        if (isConvexExposedOnSide(chunkInstance, subChunkInstance, cellXYZ, faceDirection, sideDirection, blockHandle))
            return EXPOSURE_CONVEX;

        if (isConcaveExposedOnSide(chunkInstance, subChunkInstance, cellXYZ, faceDirection, sideDirection, blockHandle))
            return EXPOSURE_CONCAVE;

        return EXPOSURE_NONE;
    }

    private boolean isConvexExposedOnSide(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellXYZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle) {

        SubChunkInstance sideSubChunk = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, cellXYZ, sideDirection);

        if (sideSubChunk == null || sideSubChunk == ERROR)
            return true;

        int sideXYZ = ChunkCoordinate3Int.getNeighborAndWrap(cellXYZ, sideDirection);
        BlockPaletteHandle sidePalette = sideSubChunk.getBlockPaletteHandle();
        short sideBlockID = sidePalette.getBlock(sideXYZ);
        BlockHandle sideBlock = blockManager.getBlockHandleFromBlockID(sideBlockID);

        if (sideBlock.getGeometry() == blockHandle.getGeometry())
            return false;

        if (blockHasFace(chunkInstance, sideSubChunk, sideXYZ, faceDirection, null, sideBlock))
            return false;

        SubChunkInstance faceSubChunk = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, cellXYZ, faceDirection);

        if (faceSubChunk != null && faceSubChunk != ERROR) {
            int faceXYZ = ChunkCoordinate3Int.getNeighborAndWrap(cellXYZ, faceDirection);
            BlockPaletteHandle facePalette = faceSubChunk.getBlockPaletteHandle();
            short faceBlockID = facePalette.getBlock(faceXYZ);
            BlockHandle faceBlock = blockManager.getBlockHandleFromBlockID(faceBlockID);

            if (blockHasFace(chunkInstance, faceSubChunk, faceXYZ, sideDirection, null, faceBlock))
                return false;
        }

        return true;
    }

    private boolean isConcaveExposedOnSide(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellXYZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle) {

        SubChunkInstance sideSubChunk = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, cellXYZ, sideDirection);

        if (sideSubChunk == null || sideSubChunk == ERROR)
            return false;

        int sideXYZ = ChunkCoordinate3Int.getNeighborAndWrap(cellXYZ, sideDirection);
        BlockPaletteHandle sidePalette = sideSubChunk.getBlockPaletteHandle();
        short sideBlockID = sidePalette.getBlock(sideXYZ);
        BlockHandle sideBlock = blockManager.getBlockHandleFromBlockID(sideBlockID);

        if (sideBlock.getGeometry() != blockHandle.getGeometry())
            return false;

        SubChunkInstance diagonalSubChunk = getComparativeSubChunkInstance(
                chunkInstance, sideSubChunk, sideXYZ, faceDirection);

        if (diagonalSubChunk == null || diagonalSubChunk == ERROR)
            return false;

        int diagonalXYZ = ChunkCoordinate3Int.getNeighborAndWrap(sideXYZ, faceDirection);
        BlockPaletteHandle diagonalPalette = diagonalSubChunk.getBlockPaletteHandle();
        short diagonalBlockID = diagonalPalette.getBlock(diagonalXYZ);
        BlockHandle diagonalBlock = blockManager.getBlockHandleFromBlockID(diagonalBlockID);

        if (!diagonalBlock.isNatural() || diagonalBlock.getGeometry() != blockHandle.getGeometry())
            return false;

        Direction3Vector oppositeSide = Direction3Vector.getOpposite(sideDirection);

        return blockHasFace(chunkInstance, diagonalSubChunk, diagonalXYZ, oppositeSide, null, diagonalBlock);
    }

    private boolean isStretchExposedOnSide(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellXYZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle) {

        SubChunkInstance sideSubChunk = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, cellXYZ, sideDirection);

        if (sideSubChunk == null || sideSubChunk == ERROR)
            return false;

        int sideXYZ = ChunkCoordinate3Int.getNeighborAndWrap(cellXYZ, sideDirection);
        BlockPaletteHandle sidePalette = sideSubChunk.getBlockPaletteHandle();
        short sideBlockID = sidePalette.getBlock(sideXYZ);
        BlockHandle sideBlock = blockManager.getBlockHandleFromBlockID(sideBlockID);

        if (sideBlock.getGeometry() == blockHandle.getGeometry())
            return false;

        SubChunkInstance diagonalSubChunk = getComparativeSubChunkInstance(
                chunkInstance, sideSubChunk, sideXYZ, faceDirection);

        if (diagonalSubChunk == null || diagonalSubChunk == ERROR)
            return false;

        int diagonalXYZ = ChunkCoordinate3Int.getNeighborAndWrap(sideXYZ, faceDirection);
        BlockPaletteHandle diagonalPalette = diagonalSubChunk.getBlockPaletteHandle();
        short diagonalBlockID = diagonalPalette.getBlock(diagonalXYZ);
        BlockHandle diagonalBlock = blockManager.getBlockHandleFromBlockID(diagonalBlockID);

        return diagonalBlock.isNatural() && diagonalBlock.getGeometry() == blockHandle.getGeometry();
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
            int materialId, TextureHandle textureHandle,
            int vert0XYZ, int vert1XYZ, int vert2XYZ, int vert3XYZ,
            float vert0Color, float vert1Color, float vert2Color, float vert3Color,
            int encodedFace,
            int sizeA, int sizeB,
            int maskA0, int maskA1, int maskB0, int maskB1,
            int negMaskA0, int negMaskA1, int negMaskB0, int negMaskB1) {

        FloatArrayList buffer = verts.computeIfAbsent(materialId, k -> new FloatArrayList());

        float nor = (float) direction3Vector.index;
        float fEncFace = (float) encodedFace;
        float u0 = textureHandle.getU0();
        float v0 = textureHandle.getV0();
        float fQuadSize = (float) ((sizeA & 0xFF) | ((sizeB & 0xFF) << 8));
        float fMaskA0 = (float) maskA0;
        float fMaskA1 = (float) maskA1;
        float fMaskB0 = (float) maskB0;
        float fMaskB1 = (float) maskB1;
        float fNegMaskA0 = (float) negMaskA0;
        float fNegMaskA1 = (float) negMaskA1;
        float fNegMaskB0 = (float) negMaskB0;
        float fNegMaskB1 = (float) negMaskB1;

        buffer.add((float) Coordinate3Int.unpackX(vert0XYZ));
        buffer.add((float) Coordinate3Int.unpackY(vert0XYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vert0XYZ));
        buffer.add(nor);
        buffer.add(vert0Color);
        buffer.add(u0);
        buffer.add(v0);
        buffer.add(fEncFace);
        buffer.add(fQuadSize);
        buffer.add(fMaskA0);
        buffer.add(fMaskA1);
        buffer.add(fMaskB0);
        buffer.add(fMaskB1);
        buffer.add(fNegMaskA0);
        buffer.add(fNegMaskA1);
        buffer.add(fNegMaskB0);
        buffer.add(fNegMaskB1);

        buffer.add((float) Coordinate3Int.unpackX(vert1XYZ));
        buffer.add((float) Coordinate3Int.unpackY(vert1XYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vert1XYZ));
        buffer.add(nor);
        buffer.add(vert1Color);
        buffer.add(u0);
        buffer.add(v0);
        buffer.add(fEncFace);
        buffer.add(fQuadSize);
        buffer.add(fMaskA0);
        buffer.add(fMaskA1);
        buffer.add(fMaskB0);
        buffer.add(fMaskB1);
        buffer.add(fNegMaskA0);
        buffer.add(fNegMaskA1);
        buffer.add(fNegMaskB0);
        buffer.add(fNegMaskB1);

        buffer.add((float) Coordinate3Int.unpackX(vert2XYZ));
        buffer.add((float) Coordinate3Int.unpackY(vert2XYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vert2XYZ));
        buffer.add(nor);
        buffer.add(vert2Color);
        buffer.add(u0);
        buffer.add(v0);
        buffer.add(fEncFace);
        buffer.add(fQuadSize);
        buffer.add(fMaskA0);
        buffer.add(fMaskA1);
        buffer.add(fMaskB0);
        buffer.add(fMaskB1);
        buffer.add(fNegMaskA0);
        buffer.add(fNegMaskA1);
        buffer.add(fNegMaskB0);
        buffer.add(fNegMaskB1);

        buffer.add((float) Coordinate3Int.unpackX(vert3XYZ));
        buffer.add((float) Coordinate3Int.unpackY(vert3XYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vert3XYZ));
        buffer.add(nor);
        buffer.add(vert3Color);
        buffer.add(u0);
        buffer.add(v0);
        buffer.add(fEncFace);
        buffer.add(fQuadSize);
        buffer.add(fMaskA0);
        buffer.add(fMaskA1);
        buffer.add(fMaskB0);
        buffer.add(fMaskB1);
        buffer.add(fNegMaskA0);
        buffer.add(fNegMaskA1);
        buffer.add(fNegMaskB0);
        buffer.add(fNegMaskB1);

        return true;
    }
}