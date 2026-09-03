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

class FullGeometryBranch extends BranchPackage {

    /*
     * Geometry branch for full-cube blocks. Greedily expands each face along
     * both tangent axes, samples ambient-occlusion vertex colors from
     * neighboring biomes, resolves rotation-aware texture/face encoding, and
     * packs each edge's exposure into the vertex bevel-mask attributes for
     * StandardSurface.tes to bevel. A natural block's mask encodes an inward
     * BEVEL and is written positive; an artificial block's mask instead
     * encodes an outward STRETCH — closing the gap a beveling natural
     * neighbor opens up at a shared corner — and is written negative. Both
     * are computed only where an edge is genuinely convex: a tangential
     * neighbor of matching geometry means the two faces continue the same
     * flat wall, never a corner, regardless of either side's natural flag.
     */

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

    // Face Preparation \\

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

        int maskA0 = 0;
        int maskA1 = 0;
        int maskB0 = 0;
        int maskB1 = 0;

        // maskA0 — walk B=0..sizeB-1 cells at A=0, check exposure in -A direction
        for (int j = 0; j < iSizeB; j++) {

            int cellXYZ = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionB, j);

            if (cellXYZ != -1 && isEdgeCellExposed(
                    chunkInstance, subChunkInstance,
                    cellXYZ, direction3Vector,
                    oppA, blockHandle))

                maskA0 |= (1 << j);
        }

        // maskA1 — walk B=0..sizeB-1 cells at A=sizeA-1, check exposure in +A direction
        int baseA1 = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionA, iSizeA - 1);
        for (int j = 0; j < iSizeB; j++) {

            int cellXYZ = (baseA1 != -1)
                    ? ChunkCoordinate3Int.getNeighborWithOffset(baseA1, tangentDirectionB, j)
                    : -1;

            if (cellXYZ != -1 && isEdgeCellExposed(
                    chunkInstance, subChunkInstance,
                    cellXYZ, direction3Vector,
                    tangentDirectionA, blockHandle))

                maskA1 |= (1 << j);
        }

        // maskB0 — walk A=0..sizeA-1 cells at B=0, check exposure in -B direction
        for (int i = 0; i < iSizeA; i++) {

            int cellXYZ = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionA, i);

            if (cellXYZ != -1 && isEdgeCellExposed(
                    chunkInstance, subChunkInstance,
                    cellXYZ, direction3Vector,
                    oppB, blockHandle))

                maskB0 |= (1 << i);
        }

        // maskB1 — walk A=0..sizeA-1 cells at B=sizeB-1, check exposure in +B direction
        int baseB1 = ChunkCoordinate3Int.getNeighborWithOffset(xyz, tangentDirectionB, iSizeB - 1);
        for (int i = 0; i < iSizeA; i++) {

            int cellXYZ = (baseB1 != -1)
                    ? ChunkCoordinate3Int.getNeighborWithOffset(baseB1, tangentDirectionA, i)
                    : -1;

            if (cellXYZ != -1 && isEdgeCellExposed(
                    chunkInstance, subChunkInstance,
                    cellXYZ, direction3Vector,
                    tangentDirectionB, blockHandle))

                maskB1 |= (1 << i);
        }

        // An artificial block never bevels its own edges — its masks instead
        // encode STRETCH, written negative so StandardSurface.tes can tell
        // the two apart while still reading the same bit-packed attributes.
        if (!blockHandle.isNatural()) {
            maskA0 = -maskA0;
            maskA1 = -maskA1;
            maskB0 = -maskB0;
            maskB1 = -maskB1;
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
                maskA0, maskA1, maskB0, maskB1);
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

    // Edge Exposure \\

    /*
     * Dispatches one edge cell to the natural bevel test or the artificial
     * stretch test based on the current block's own natural flag.
     */
    private boolean isEdgeCellExposed(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int cellXYZ,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle) {

        return blockHandle.isNatural()
                ? isConvexExposedOnSide(chunkInstance, subChunkInstance, cellXYZ, faceDirection, sideDirection,
                        blockHandle)
                : isStretchExposedOnSide(chunkInstance, subChunkInstance, cellXYZ, faceDirection, sideDirection,
                        blockHandle);
    }

    /*
     * True when the edge at sideDirection from cellXYZ is convex (the
     * tangent neighbor is open or different geometry) and exposed (that
     * neighbor doesn't itself carry a flush, coplanar face in faceDirection)
     * for a natural block — never for a concave edge, where the tangent
     * neighbor is the same solid geometry continuing the same flat wall, and
     * cross-referencing that unrelated edge would zero out real corners and
     * bleed bevel onto seams it shouldn't touch. The symmetric partner check
     * mirrors this same flush test from the perpendicular face's side, so
     * two adjoining faces of one genuine 3D corner always agree on it.
     */
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

    /*
     * Mirror of isConvexExposedOnSide() for an artificial block's own edge:
     * true where this block's own corner sits open to different geometry
     * (the same convex test above) AND a natural block of matching geometry
     * sits diagonally across that opening — exactly the neighbor
     * isConvexExposedOnSide() would bevel inward and downward, away from
     * this shared corner. StandardSurface.tes reads a positive mask as an
     * inward bevel and a negative one as this outward stretch, so an
     * artificial block only ever reaches toward a natural neighbor that is
     * actually pulling away from it, never anywhere else.
     */
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

    // Face Finalization \\

    private boolean finalizeFace(
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            DynamicPacketInstance dynamicPacketInstance,
            Direction3Vector direction3Vector,
            int materialId, TextureHandle textureHandle,
            int vert0XYZ, int vert1XYZ, int vert2XYZ, int vert3XYZ,
            float vert0Color, float vert1Color, float vert2Color, float vert3Color,
            int encodedFace,
            int sizeA, int sizeB,
            int maskA0, int maskA1, int maskB0, int maskB1) {

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

        // vert 0
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

        // vert 1
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

        // vert 2
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

        // vert 3
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

        return true;
    }
}