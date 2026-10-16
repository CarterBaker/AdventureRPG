package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.contentpipeline.util.ContentUtility;
import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector;
import application.bootstrap.geometrypipeline.quadpacket.QuadPacketInstance;
import application.bootstrap.geometrypipeline.quadpacket.QuadRecordUtility;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.block.BlockRotationType;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.coveringmanager.CoveringManager;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.CoverageUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.ints.IntArrayList;

class SurfaceEmissionBranch extends BranchPackage {

    /*
     * Turns one merged solid quad into its packed record at either
     * resolution: its origin, the face, extents, flags and the content its
     * block holds in one meta word, the coverage word beside the tile's
     * atlas cell, then its four corner tints and the column codes along each
     * edge as 24-bit words, so the surface shader rebuilds its corners and
     * the sub-cells around any vertex exactly as neighboring quads do. The
     * coverage word carries the cell's covering and level on an upper face,
     * on an upright face only when the covering has side tiles, and never on
     * a lower face; a content shows on every face of the block holding it.
     * resolveCoverageWord() and resolveContentID() are the definitions both
     * merge passes compare, so a merged quad never spans two coverages or two
     * contents. Every value is a local, so builder threads emit through it at
     * once.
     */

    // Internal
    private TextureManager textureManager;
    private CoveringManager coveringManager;
    private SubCellSampleBranch subCellSampleBranch;

    // Settings
    private int divisions;
    private float subBlockSize;
    private int edgeBitsPerCell;
    private int edgeCellMask;
    private int edgeWordBits;
    private int edgeWordMask;
    private int faceBits;
    private int encodedFaceShift;
    private int encodedFaceMask;
    private int sizeMask;
    private int sizeAShift;
    private int sizeBShift;
    private int naturalShift;
    private int edgeUnitShift;
    private int contentShift;
    private int tileShift;

    // Internal \\

    @Override
    protected void create() {

        // Settings
        this.divisions = SubBlockUtility.DIVISIONS;
        this.subBlockSize = SubBlockUtility.SIZE;
        this.edgeBitsPerCell = EngineSetting.GEOMETRY_EDGE_BITS_PER_CELL;
        this.edgeCellMask = (1 << edgeBitsPerCell) - 1;
        this.edgeWordBits = edgeBitsPerCell * EngineSetting.GEOMETRY_EDGE_CELLS_PER_WORD;
        this.edgeWordMask = (1 << edgeWordBits) - 1;
        this.faceBits = EngineSetting.GEOMETRY_META_FACE_BITS;
        this.encodedFaceShift = faceBits;
        this.encodedFaceMask = (1 << EngineSetting.GEOMETRY_META_ENCODED_FACE_BITS) - 1;
        this.sizeMask = (1 << EngineSetting.GEOMETRY_META_SIZE_BITS) - 1;
        this.sizeAShift = encodedFaceShift + EngineSetting.GEOMETRY_META_ENCODED_FACE_BITS;
        this.sizeBShift = sizeAShift + EngineSetting.GEOMETRY_META_SIZE_BITS;
        this.naturalShift = sizeBShift + EngineSetting.GEOMETRY_META_SIZE_BITS;
        this.edgeUnitShift = naturalShift + 1;
        this.contentShift = edgeUnitShift + 1;
        this.tileShift = EngineSetting.QUAD_RECORD_TILE_SHIFT;
    }

    @Override
    protected void get() {

        // Internal
        this.textureManager = get(TextureManager.class);
        this.coveringManager = get(CoveringManager.class);
        this.subCellSampleBranch = get(SubCellSampleBranch.class);
    }

    // Emission \\

    void emitQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            QuadPacketInstance quadPacket,
            Color vertColorAccumulator,
            int originSubX, int originSubY, int originSubZ,
            int sizeA, int sizeB,
            int cellsPerEntry,
            Direction3Vector faceDirection,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle) {

        Direction3Vector tangentA = Direction3Vector.getTangentA(faceDirection);
        Direction3Vector tangentB = Direction3Vector.getTangentB(faceDirection);
        Direction3Vector oppositeA = Direction3Vector.getOpposite(tangentA);
        Direction3Vector oppositeB = Direction3Vector.getOpposite(tangentB);

        // Vertex corners in sub-cell units
        int vert0X = originSubX + (faceDirection.positive ? faceDirection.x : 0);
        int vert0Y = originSubY + (faceDirection.positive ? faceDirection.y : 0);
        int vert0Z = originSubZ + (faceDirection.positive ? faceDirection.z : 0);

        int vert1X = vert0X + tangentA.x * sizeA;
        int vert1Y = vert0Y + tangentA.y * sizeA;
        int vert1Z = vert0Z + tangentA.z * sizeA;

        int vert2X = vert1X + tangentB.x * sizeB;
        int vert2Y = vert1Y + tangentB.y * sizeB;
        int vert2Z = vert1Z + tangentB.z * sizeB;

        int vert3X = vert0X + tangentB.x * sizeB;
        int vert3Y = vert0Y + tangentB.y * sizeB;
        int vert3Z = vert0Z + tangentB.z * sizeB;

        int vert0Color = resolveVertColor(chunkInstance, subChunkInstance, vert0X, vert0Y, vert0Z,
                biomeHandle, vertColorAccumulator);
        int vert1Color = resolveVertColor(chunkInstance, subChunkInstance, vert1X, vert1Y, vert1Z,
                biomeHandle, vertColorAccumulator);
        int vert2Color = resolveVertColor(chunkInstance, subChunkInstance, vert2X, vert2Y, vert2Z,
                biomeHandle, vertColorAccumulator);
        int vert3Color = resolveVertColor(chunkInstance, subChunkInstance, vert3X, vert3Y, vert3Z,
                biomeHandle, vertColorAccumulator);

        // Texture, orientation, coverage and content
        int cellXYZ = subCellSampleBranch.toCellXYZ(originSubX, originSubY, originSubZ);
        short baseOrientation = rotationPaletteHandle.getBlock(cellXYZ);
        int orientation = baseOrientation & 0xFFFF;
        int textureID = resolveTextureID(blockHandle, faceDirection, orientation);
        TextureHandle textureHandle = textureManager.getTextureHandleFromTileID(textureID);
        int encodedFace = resolveEncodedFace(blockHandle, faceDirection, orientation);
        int coverageWord = resolveCoverageWord(subChunkInstance, cellXYZ, faceDirection);
        short contentID = resolveContentID(subChunkInstance, cellXYZ);

        // Edge columns
        int lastA = sizeA - 1;
        int lastB = sizeB - 1;

        long edgeA0 = buildEdgeCodes(chunkInstance, subChunkInstance, rotationPaletteHandle,
                originSubX, originSubY, originSubZ,
                tangentB, sizeB, cellsPerEntry, faceDirection, oppositeA, blockHandle, baseOrientation);

        long edgeA1 = buildEdgeCodes(chunkInstance, subChunkInstance, rotationPaletteHandle,
                originSubX + tangentA.x * lastA, originSubY + tangentA.y * lastA, originSubZ + tangentA.z * lastA,
                tangentB, sizeB, cellsPerEntry, faceDirection, tangentA, blockHandle, baseOrientation);

        long edgeB0 = buildEdgeCodes(chunkInstance, subChunkInstance, rotationPaletteHandle,
                originSubX, originSubY, originSubZ,
                tangentA, sizeA, cellsPerEntry, faceDirection, oppositeB, blockHandle, baseOrientation);

        long edgeB1 = buildEdgeCodes(chunkInstance, subChunkInstance, rotationPaletteHandle,
                originSubX + tangentB.x * lastB, originSubY + tangentB.y * lastB, originSubZ + tangentB.z * lastB,
                tangentA, sizeA, cellsPerEntry, faceDirection, tangentB, blockHandle, baseOrientation);

        int meta = (faceDirection.index & ((1 << faceBits) - 1))
                | ((encodedFace & encodedFaceMask) << encodedFaceShift)
                | ((lastA & sizeMask) << sizeAShift)
                | ((lastB & sizeMask) << sizeBShift)
                | ((blockHandle.isNatural() ? 1 : 0) << naturalShift)
                | ((cellsPerEntry == divisions ? 1 : 0) << edgeUnitShift)
                | ContentUtility.toMetaBits(contentID, contentShift);

        IntArrayList records = quadPacket.getRecords(
                blockHandle.getMaterialID(), EngineSetting.QUAD_RECORD_SOLID_INTS);

        records.add(QuadRecordUtility.packPosition(originSubX, originSubY, originSubZ));
        records.add(meta);
        records.add(coverageWord | (textureHandle.getAtlasCell() << tileShift));
        QuadRecordUtility.addWords(records, vert0Color, vert1Color, vert2Color, vert3Color);
        QuadRecordUtility.addWords(records, toLowWord(edgeA0), toLowWord(edgeA1), toLowWord(edgeB0),
                toLowWord(edgeB1));
        QuadRecordUtility.addWords(records, toHighWord(edgeA0), toHighWord(edgeA1), toHighWord(edgeB0),
                toHighWord(edgeB1));
    }

    // Edge Classification \\

    private long buildEdgeCodes(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            int originSubX, int originSubY, int originSubZ,
            Direction3Vector runDirection,
            int runLength,
            int cellsPerEntry,
            Direction3Vector faceDirection,
            Direction3Vector sideDirection,
            BlockHandle blockHandle,
            short baseOrientation) {

        int entryCount = runLength / cellsPerEntry;
        long codes = 0L;

        for (int entry = -1; entry <= entryCount; entry++) {

            int offset;

            if (entry < 0)
                offset = -1;
            else if (entry < entryCount)
                offset = entry * cellsPerEntry;
            else
                offset = runLength;

            int code = subCellSampleBranch.classifyColumn(
                    chunkInstance,
                    subChunkInstance,
                    rotationPaletteHandle,
                    originSubX + runDirection.x * offset,
                    originSubY + runDirection.y * offset,
                    originSubZ + runDirection.z * offset,
                    faceDirection,
                    sideDirection,
                    blockHandle,
                    baseOrientation);

            codes |= (long) (code & edgeCellMask) << ((entry + 1) * edgeBitsPerCell);
        }

        return codes;
    }

    private int toLowWord(long codes) {
        return (int) (codes & edgeWordMask);
    }

    private int toHighWord(long codes) {
        return (int) ((codes >>> edgeWordBits) & edgeWordMask);
    }

    // Texture \\

    private int resolveTextureID(BlockHandle blockHandle, Direction3Vector worldFace, int orientation) {

        BlockRotationType rotationType = blockHandle.getRotationType();

        if (rotationType == BlockRotationType.NONE || rotationType == BlockRotationType.NATURAL_FULL)
            return blockHandle.getTextureForFace(worldFace);

        Direction3Vector textureFace = Direction3Vector.VALUES[Direction3Vector.getEncodedFace(orientation, worldFace)
                / EngineSetting.ENCODED_FACE_SPIN_COUNT];

        return blockHandle.getTextureForFace(textureFace);
    }

    private int resolveEncodedFace(BlockHandle blockHandle, Direction3Vector worldFace, int orientation) {

        BlockRotationType rotationType = blockHandle.getRotationType();

        if (rotationType == BlockRotationType.NONE)
            return worldFace.ordinal() * EngineSetting.ENCODED_FACE_SPIN_COUNT;

        if (rotationType == BlockRotationType.NATURAL_FULL) {
            if (worldFace == Direction3Vector.UP || worldFace == Direction3Vector.DOWN)
                return EngineSetting.ENCODED_FACE_NATURAL_FULL_OFFSET + worldFace.ordinal();
            else
                return worldFace.ordinal() * EngineSetting.ENCODED_FACE_SPIN_COUNT;
        }

        return Direction3Vector.getEncodedFace(orientation, worldFace);
    }

    // Coverage \\

    // The coverage word a face of a cell carries — bare on a lower face and on an upright face of a sideless covering
    int resolveCoverageWord(SubChunkInstance subChunkInstance, int cellXYZ, Direction3Vector faceDirection) {

        short coverage = subChunkInstance.getCoverage(cellXYZ);

        if (!CoverageUtility.isCovered(coverage) || faceDirection == Direction3Vector.DOWN)
            return CoverageUtility.NONE;

        if (faceDirection == Direction3Vector.UP)
            return CoverageUtility.toVertexWord(coverage, false);

        if (!coveringManager.getCoveringHandleFromCoverage(coverage).hasSide())
            return CoverageUtility.NONE;

        return CoverageUtility.toVertexWord(coverage, true);
    }

    // Content \\

    // The content every face of a cell shows — its block's, held only by a whole block
    short resolveContentID(SubChunkInstance subChunkInstance, int cellXYZ) {
        return subChunkInstance.getContent(cellXYZ);
    }

    // Vertex Color \\

    private int resolveVertColor(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int vertSubX, int vertSubY, int vertSubZ,
            BiomeHandle biomeHandle,
            Color vertColorAccumulator) {

        vertColorAccumulator.set(0f, 0f, 0f, 1f);
        int sampleCount = 0;

        int lowX = Math.floorDiv(vertSubX, divisions);
        int lowY = Math.floorDiv(vertSubY, divisions);
        int lowZ = Math.floorDiv(vertSubZ, divisions);
        int highX = Math.floorDiv(vertSubX + divisions - 1, divisions);
        int highY = Math.floorDiv(vertSubY + divisions - 1, divisions);
        int highZ = Math.floorDiv(vertSubZ + divisions - 1, divisions);

        for (int vertY = lowY; vertY <= highY; vertY++)
            for (int vertZ = lowZ; vertZ <= highZ; vertZ++)
                for (int vertX = lowX; vertX <= highX; vertX++)
                    for (VertBlockNeighbor3Vector neighbor : VertBlockNeighbor3Vector.VALUES) {

                        BiomeHandle neighborBiomeHandle = subCellSampleBranch.sampleBiome(
                                chunkInstance,
                                subChunkInstance,
                                vertX + neighbor.x,
                                vertY + neighbor.y,
                                vertZ + neighbor.z,
                                biomeHandle);

                        Color color = neighborBiomeHandle.getBiomeColor();
                        vertColorAccumulator.r += color.r;
                        vertColorAccumulator.g += color.g;
                        vertColorAccumulator.b += color.b;
                        sampleCount++;
                    }

        float scale = 1f / sampleCount;

        return packColor(
                vertColorAccumulator.r * scale,
                vertColorAccumulator.g * scale,
                vertColorAccumulator.b * scale);
    }

    private int packColor(float r, float g, float b) {

        int ir = Math.round(Math.min(Math.max(r, EngineSetting.COLOR_CHANNEL_MIN), EngineSetting.COLOR_CHANNEL_MAX)
                * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int ig = Math.round(Math.min(Math.max(g, EngineSetting.COLOR_CHANNEL_MIN), EngineSetting.COLOR_CHANNEL_MAX)
                * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int ib = Math.round(Math.min(Math.max(b, EngineSetting.COLOR_CHANNEL_MIN), EngineSetting.COLOR_CHANNEL_MAX)
                * EngineSetting.COLOR_CHANNEL_BYTE_MAX);

        return (ir << 16) | (ig << 8) | ib;
    }
}
