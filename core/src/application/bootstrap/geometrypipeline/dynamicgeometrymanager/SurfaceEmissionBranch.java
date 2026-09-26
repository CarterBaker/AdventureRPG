package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.VertBlockNeighbor3Vector;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texturemanager.TextureManager;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.block.BlockRotationType;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
import engine.graphics.color.Color;
import engine.root.BranchPackage;
import engine.root.EngineSetting;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class SurfaceEmissionBranch extends BranchPackage {

    /*
     * Turns one merged solid quad into its four patch vertices, whichever
     * resolution it was merged at. A quad arrives in sub-cell units: the
     * sub-cell behind its first corner, its extent along each tangent, and
     * how many sub-cells each entry of its edge data covers — two for a
     * block-resolution quad, one for a sub-block one. Each edge carries one
     * four-bit column code (see SubCellSampleBranch.classifyColumn()) per
     * entry across the run plus one padding entry at either end, where the
     * padding describes the column diagonally beyond the quad's corner.
     * Together the four edges describe every column touching the quad from
     * outside, so the surface shader can rebuild the eight sub-cells around
     * any lattice vertex on the quad exactly as any other quad touching that
     * vertex does. Codes split across a low word of six entries and a high
     * word of the rest, and the meta word packs the face, encoded face, both
     * extents in sub-cells, the natural flag and the edge resolution into 21
     * bits, so every packed value is exact in a float32 mantissa. Vertex
     * tint on a half-block corner is the mean of the block corners
     * bracketing it, so tint stays linear across any mix of quad sizes.
     */

    // Internal
    private TextureManager textureManager;
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
    private int vertexFloatCount;
    private int quadVertexCount;

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
        this.vertexFloatCount = EngineSetting.CHUNK_VERTEX_FLOAT_COUNT;
        this.quadVertexCount = EngineSetting.QUAD_VERTEX_COUNT;
    }

    @Override
    protected void get() {

        // Internal
        this.textureManager = get(TextureManager.class);
        this.subCellSampleBranch = get(SubCellSampleBranch.class);
    }

    // Emission \\

    void emitQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle rotationPaletteHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
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

        float vert0Color = resolveVertColor(chunkInstance, subChunkInstance, vert0X, vert0Y, vert0Z,
                biomeHandle, vertColorAccumulator);
        float vert1Color = resolveVertColor(chunkInstance, subChunkInstance, vert1X, vert1Y, vert1Z,
                biomeHandle, vertColorAccumulator);
        float vert2Color = resolveVertColor(chunkInstance, subChunkInstance, vert2X, vert2Y, vert2Z,
                biomeHandle, vertColorAccumulator);
        float vert3Color = resolveVertColor(chunkInstance, subChunkInstance, vert3X, vert3Y, vert3Z,
                biomeHandle, vertColorAccumulator);

        // Texture and orientation
        short baseOrientation = rotationPaletteHandle.getBlock(
                subCellSampleBranch.toCellXYZ(originSubX, originSubY, originSubZ));
        int orientation = baseOrientation & 0xFFFF;
        int textureID = resolveTextureID(blockHandle, faceDirection, orientation);
        TextureHandle textureHandle = textureManager.getTextureHandleFromTileID(textureID);
        int encodedFace = resolveEncodedFace(blockHandle, faceDirection, orientation);

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
                | ((cellsPerEntry == divisions ? 1 : 0) << edgeUnitShift);

        FloatArrayList buffer = verts.computeIfAbsent(blockHandle.getMaterialID(), k -> new FloatArrayList());
        buffer.ensureCapacity(buffer.size() + vertexFloatCount * quadVertexCount);

        float u0 = textureHandle.getU0();
        float v0 = textureHandle.getV0();
        float fMeta = (float) meta;

        pushVert(buffer, vert0X, vert0Y, vert0Z, u0, v0, fMeta, vert0Color, edgeA0, edgeA1, edgeB0, edgeB1);
        pushVert(buffer, vert1X, vert1Y, vert1Z, u0, v0, fMeta, vert1Color, edgeA0, edgeA1, edgeB0, edgeB1);
        pushVert(buffer, vert2X, vert2Y, vert2Z, u0, v0, fMeta, vert2Color, edgeA0, edgeA1, edgeB0, edgeB1);
        pushVert(buffer, vert3X, vert3Y, vert3Z, u0, v0, fMeta, vert3Color, edgeA0, edgeA1, edgeB0, edgeB1);
    }

    // Edge Classification \\

    /*
     * Column codes for one edge, entry by entry from the low padding to the
     * high padding, packed four bits apiece into one long. An entry
     * classifies the column beside the first sub-cell it covers, and a
     * padding entry the column beside the sub-cell just past the quad's end.
     */
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

    private float toLowWord(long codes) {
        return (float) (int) (codes & edgeWordMask);
    }

    private float toHighWord(long codes) {
        return (float) (int) ((codes >>> edgeWordBits) & edgeWordMask);
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

    // Vertex Color \\

    /*
     * Mean biome tint of the blocks around a corner given in sub-cell units.
     * A corner on the block lattice averages the eight blocks touching it; a
     * corner halfway along a block averages every block-lattice corner
     * bracketing it, which is exactly the linear interpolation a larger quad
     * spanning that corner would produce there.
     */
    private float resolveVertColor(
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

    private float packColor(float r, float g, float b) {

        int ir = Math.round(Math.min(Math.max(r, EngineSetting.COLOR_CHANNEL_MIN), EngineSetting.COLOR_CHANNEL_MAX)
                * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int ig = Math.round(Math.min(Math.max(g, EngineSetting.COLOR_CHANNEL_MIN), EngineSetting.COLOR_CHANNEL_MAX)
                * EngineSetting.COLOR_CHANNEL_BYTE_MAX);
        int ib = Math.round(Math.min(Math.max(b, EngineSetting.COLOR_CHANNEL_MIN), EngineSetting.COLOR_CHANNEL_MAX)
                * EngineSetting.COLOR_CHANNEL_BYTE_MAX);

        return (float) ((ir << 16) | (ig << 8) | ib);
    }

    // Vertex \\

    private void pushVert(
            FloatArrayList buffer,
            int vertSubX, int vertSubY, int vertSubZ,
            float u0, float v0,
            float meta, float color,
            long edgeA0, long edgeA1, long edgeB0, long edgeB1) {

        buffer.add(vertSubX * subBlockSize);
        buffer.add(vertSubY * subBlockSize);
        buffer.add(vertSubZ * subBlockSize);
        buffer.add(u0);
        buffer.add(v0);
        buffer.add(meta);
        buffer.add(color);
        buffer.add(toLowWord(edgeA0));
        buffer.add(toLowWord(edgeA1));
        buffer.add(toLowWord(edgeB0));
        buffer.add(toLowWord(edgeB1));
        buffer.add(toHighWord(edgeA0));
        buffer.add(toHighWord(edgeA1));
        buffer.add(toHighWord(edgeB0));
        buffer.add(toHighWord(edgeB1));
    }
}
