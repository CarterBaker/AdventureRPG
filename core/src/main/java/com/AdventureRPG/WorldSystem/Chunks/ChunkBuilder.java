package com.AdventureRPG.WorldSystem.Chunks;

import java.util.BitSet;

import com.AdventureRPG.MaterialManager.MaterialData;
import com.AdventureRPG.TextureManager.TextureManager.UVRect;
import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.Util.Direction3Int;
import com.AdventureRPG.WorldSystem.PackedCoordinate3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.Type;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.FloatArray;
import com.badlogic.gdx.utils.IntArray;
import com.badlogic.gdx.utils.ShortArray;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

public class ChunkBuilder {

    // Game Manager
    private final WorldSystem worldSystem;
    private final PackedCoordinate3Int packedCoordinate3Int;
    private final BiomeSystem biomeSystem;

    // Settings
    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

    // Data
    private final Chunk chunk;
    private IntArray quads;
    Int2IntOpenHashMap quadCounts;
    private final int QUAD_SIZE = 9;
    private Color[] blendColors;

    private BitSet tempBatchedBlocks;
    private BitSet batchedBlocksUp;
    private BitSet batchedBlocksNorth;
    private BitSet batchedBlocksSouth;
    private BitSet batchedBlocksEast;
    private BitSet batchedBlocksWest;
    private BitSet batchedBlocksDown;

    private Color tmpColor;

    // Base \\

    public ChunkBuilder(WorldSystem worldSystem, Chunk chunk) {

        // Game Manager
        this.worldSystem = worldSystem;
        this.packedCoordinate3Int = worldSystem.packedCoordinate3Int;
        this.biomeSystem = worldSystem.biomeSystem;

        // Settings
        this.CHUNK_SIZE = worldSystem.settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = worldSystem.settings.WORLD_HEIGHT;

        // Data
        this.chunk = chunk;
        this.quads = new IntArray(worldSystem.settings.CHUNK_VERT_BUFFER);
        this.quadCounts = new Int2IntOpenHashMap();
        this.blendColors = new Color[8];

        this.tempBatchedBlocks = new BitSet(packedCoordinate3Int.chunkSize);
        this.batchedBlocksUp = new BitSet(packedCoordinate3Int.chunkSize);
        this.batchedBlocksNorth = new BitSet(packedCoordinate3Int.chunkSize);
        this.batchedBlocksSouth = new BitSet(packedCoordinate3Int.chunkSize);
        this.batchedBlocksEast = new BitSet(packedCoordinate3Int.chunkSize);
        this.batchedBlocksWest = new BitSet(packedCoordinate3Int.chunkSize);
        this.batchedBlocksDown = new BitSet(packedCoordinate3Int.chunkSize);

        this.tmpColor = new Color();
    }

    // Data \\

    public void build(int subChunkIndex) {

        try {

            SubChunk subChunk = chunk.getSubChunk(subChunkIndex);

            for (int index = 0; index < packedCoordinate3Int.chunkSize; index++) {

                int xyz = packedCoordinate3Int.getPackedBlockCoordinate(index);

                int aX = packedCoordinate3Int.unpackX(xyz);
                int aY = packedCoordinate3Int.unpackY(xyz);
                int aZ = packedCoordinate3Int.unpackZ(xyz);

                int blockID = subChunk.getBlock(aX, aY, aZ);
                Type type = worldSystem.getBlockType(blockID);

                if (type == Type.NULL)
                    continue;

                int biomeID = subChunk.getBiome(aX, aY, aZ);

                for (int directionIndex = 0; directionIndex < 6; directionIndex++) {

                    Direction3Int direction = Direction3Int.DIRECTIONS[directionIndex];

                    BitSet batchedSet = getBatchedSet(direction);

                    if (batchedSet.get(xyz))
                        continue;

                    assembleFace(
                            quads, batchedSet,
                            subChunk,
                            subChunkIndex,
                            aX, aY, aZ,
                            direction,
                            biomeID, blockID,
                            type);
                }
            }

            if (quads.size > 0)
                buildFromQuads(subChunk.subChunkMesh, subChunkIndex);
        }

        catch (AbortBuildException endEarly) {
            clearData();
        }

        finally {
            clearData();
        }

    }

    private void clearData() {

        quads.clear();
        quadCounts.clear();

        batchedBlocksUp.clear();
        batchedBlocksNorth.clear();
        batchedBlocksSouth.clear();
        batchedBlocksEast.clear();
        batchedBlocksWest.clear();
        batchedBlocksDown.clear();
    }

    private BitSet getBatchedSet(Direction3Int direction3Int) {

        return switch (direction3Int) {
            case UP -> batchedBlocksUp;
            case DOWN -> batchedBlocksDown;
            case NORTH -> batchedBlocksNorth;
            case SOUTH -> batchedBlocksSouth;
            case EAST -> batchedBlocksEast;
            case WEST -> batchedBlocksWest;
        };
    }

    private void assembleFace(
            IntArray quads, BitSet batchedSet,
            SubChunk subChunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            Direction3Int direction,
            int biomeID, int blockID,
            Type type) {

        if (!blockFaceCheck(
                subChunkIndex,
                aX, aY, aZ,
                direction,
                type))
            return;

        boolean checkA = true;
        boolean checkB = true;

        int sizeA = 1;
        int sizeB = 1;

        Direction3Int[] tangents = Direction3Int.getTangents(direction);
        Direction3Int comparativeDirectionA = tangents[0];
        Direction3Int comparativeDirectionB = tangents[1];

        do {

            // expand along A
            if (checkA) {

                if (tryExpand(
                        batchedSet,
                        subChunk,
                        subChunkIndex,
                        aX, aY, aZ,
                        sizeA, sizeB,
                        comparativeDirectionA, comparativeDirectionB,
                        biomeID, blockID,
                        direction,
                        type))
                    sizeA++;

                else
                    checkA = false;
            }

            // expand along B
            if (checkB) {

                if (tryExpand(
                        batchedSet,
                        subChunk,
                        subChunkIndex,
                        aX, aY, aZ,
                        sizeB, sizeA,
                        comparativeDirectionB, comparativeDirectionA,
                        biomeID, blockID,
                        direction,
                        type))
                    sizeB++;

                else
                    checkB = false;
            }

        }

        while (checkA || checkB);

        prepareFace(
                quads,
                subChunk,
                subChunkIndex,
                aX, aY, aZ,
                sizeA, sizeB,
                direction,
                biomeID, blockID);
    }

    private boolean tryExpand(
            BitSet batchedSet,
            SubChunk subChunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            int currentSize, int otherSize,
            Direction3Int expandDir, Direction3Int otherDir,
            int biomeID, int blockID,
            Direction3Int direction,
            Type type) {

        if (currentSize >= CHUNK_SIZE)
            return false;

        tempBatchedBlocks.clear();

        // Calculate next base coordinate along expandDir
        int nextX = aX + expandDir.x * currentSize;
        int nextY = aY + expandDir.y * currentSize;
        int nextZ = aZ + expandDir.z * currentSize;

        if (coordinatesOutOfBounds(nextX, nextY, nextZ))
            return false;

        // Loop across the perpendicular dimension
        for (int i = 0; i < otherSize; i++) {

            int checkX = nextX + otherDir.x * i;
            int checkY = nextY + otherDir.y * i;
            int checkZ = nextZ + otherDir.z * i;

            int xyz = packedCoordinate3Int.pack(checkX, checkY, checkZ);

            int comparativeBiomeID = subChunk.getBiome(checkX, checkY, checkZ);
            int comparativeBlockID = subChunk.getBlock(checkX, checkY, checkZ);

            if (biomeID != comparativeBiomeID ||
                    blockID != comparativeBlockID ||
                    batchedSet.get(xyz) ||
                    !blockFaceCheck(
                            subChunkIndex,
                            checkX, checkY, checkZ,
                            direction,
                            type)) {
                return false;
            }

            tempBatchedBlocks.set(xyz);
        }

        batchedSet.or(tempBatchedBlocks);

        tempBatchedBlocks.clear();
        return true;
    }

    private boolean coordinatesOutOfBounds(int x, int y, int z) {

        return (x >= CHUNK_SIZE ||
                y >= CHUNK_SIZE ||
                z >= CHUNK_SIZE);
    }

    private boolean blockFaceCheck(
            int subChunkIndex,
            int aX, int aY, int aZ,
            Direction3Int direction,
            Type type) {

        int bX = packedCoordinate3Int.addAndWrapAxis(direction.x, aX);
        int bY = packedCoordinate3Int.addAndWrapAxis(direction.y, aY);
        int bZ = packedCoordinate3Int.addAndWrapAxis(direction.z, aZ);

        SubChunk comparativeSubChunk = getComparativeSubChunk(
                subChunkIndex,
                bX, bY, bZ,
                direction);

        if (comparativeSubChunk == null)
            return true;

        int blockID = comparativeSubChunk.getBlock(bX, bY, bZ);
        Type comparativeType = worldSystem.getBlockType(blockID);

        return comparativeType != type;
    }

    private SubChunk getComparativeSubChunk(
            int subChunkIndex,
            int bX, int bY, int bZ,
            Direction3Int direction) {

        if (!packedCoordinate3Int.isOverEdge(bX, bY, bZ, direction))
            return chunk.getSubChunk(subChunkIndex);

        if (direction == Direction3Int.UP || direction == Direction3Int.DOWN) {

            int outputSubChunk = subChunkIndex + direction.y;

            if (outputSubChunk >= 0 && outputSubChunk < WORLD_HEIGHT)
                return chunk.getSubChunk(outputSubChunk);

            else
                return null;
        }

        else {

            Direction2Int direction2Int = direction.direction2Int;
            Chunk neighborChunk = chunk.getNeighborChunk(direction2Int);

            if (neighborChunk == null)
                return null;

            if (neighborChunk.getState() == ChunkState.NEEDS_GENERATION_DATA)
                throw new AbortBuildException();

            return neighborChunk.getSubChunk(subChunkIndex);
        }
    }

    private void prepareFace(
            IntArray quads,
            SubChunk subChunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            int width, int height,
            Direction3Int direction,
            int biomeID, int blockID) {

        int xyz = packedCoordinate3Int.pack(aX, aY, aZ);

        int directionIndex = direction.index;

        Direction3Int[] tangents = Direction3Int.getTangents(direction);
        Direction3Int dirA = tangents[0];
        Direction3Int dirB = tangents[1];

        // base
        int vert0X = aX;
        int vert0Y = aY;
        int vert0Z = aZ;

        // base + width
        int vert1X = aX + dirA.x * width;
        int vert1Y = aY + dirA.y * width;
        int vert1Z = aZ + dirA.z * width;

        // base + width + height
        int vert2X = vert1X + dirB.x * height;
        int vert2Y = vert1Y + dirB.y * height;
        int vert2Z = vert1Z + dirB.z * height;

        // base + height
        int vert3X = aX + dirB.x * height;
        int vert3Y = aY + dirB.y * height;
        int vert3Z = aZ + dirB.z * height;

        int color0 = getVertColor(subChunk, subChunkIndex, vert0X, vert0Y, vert0Z);
        int color1 = getVertColor(subChunk, subChunkIndex, vert1X, vert1Y, vert1Z);
        int color2 = getVertColor(subChunk, subChunkIndex, vert2X, vert2Y, vert2Z);
        int color3 = getVertColor(subChunk, subChunkIndex, vert3X, vert3Y, vert3Z);

        int materialDataID = worldSystem.getBlockByID(blockID).getMaterialDataForSide(direction).id;

        packQuad(
                quads,
                xyz,
                width,
                height,
                directionIndex,
                blockID,
                color0,
                color1,
                color2,
                color3,
                materialDataID);
    }

    private int getVertColor(
            SubChunk subChunk,
            int subChunkIndex,
            int vertX, int vertY, int vertZ) {

        for (int index = 0; index < 8; index++) {

            NeighborBlockDirection direction = NeighborBlockDirection.VALUES[index];

            int offsetX = vertX + direction.x;
            int offsetY = vertY + direction.y;
            int offsetZ = vertZ + direction.z;

            int blockX = convertToBlockSpace(offsetX);
            int blockY = convertToBlockSpace(offsetY);
            int blockZ = convertToBlockSpace(offsetZ);

            SubChunk neighborSubChunk = getNeighborSubChunk(
                    subChunk,
                    subChunkIndex,
                    blockX, blockY, blockZ,
                    offsetX, offsetY, offsetZ,
                    direction);

            if (neighborSubChunk != null) {

                int biomeID = neighborSubChunk.getBiome(blockX, blockY, blockZ);
                blendColors[index] = biomeSystem.getBiomeByID(biomeID).biomeColor;
            }

            else {

                if (blendColors[0] != null)
                    blendColors[index] = blendColors[0];
                else
                    blendColors[index] = Color.WHITE;
            }
        }

        return blendColors();
    }

    public int convertToBlockSpace(int vertAxis) {

        int output = vertAxis;

        if (output < 0)
            return CHUNK_SIZE - 1;

        if (output >= CHUNK_SIZE)
            return 0;

        return output;
    }

    private SubChunk getNeighborSubChunk(
            SubChunk subChunk,
            int subChunkIndex,
            int blockX, int blockY, int blockZ,
            int offsetX, int offsetY, int offsetZ,
            NeighborBlockDirection direction) {

        if (!isOverEdge(offsetX, offsetY, offsetZ))
            return subChunk;

        // Handle vertical movement
        if (offsetY < 0) {

            if (subChunkIndex == 0)
                return null;

            subChunkIndex -= 1;
        }

        if (offsetY >= CHUNK_SIZE) {

            if (subChunkIndex == WORLD_HEIGHT - 1)
                return null;

            subChunkIndex += 1;
        }

        Direction2Int direction2Int = getDirection2Int(offsetX, offsetZ);

        if (direction2Int == null)
            return chunk.getSubChunk(subChunkIndex);

        Chunk neighborChunk = chunk.getNeighborChunk(direction2Int);

        if (neighborChunk != null)
            return neighborChunk.getSubChunk(subChunkIndex);

        return null;
    }

    private boolean isOverEdge(int offsetX, int offsetY, int offsetZ) {

        return (offsetX < 0 || offsetX > CHUNK_SIZE ||
                offsetY < 0 || offsetY > CHUNK_SIZE ||
                offsetZ < 0 || offsetZ > CHUNK_SIZE);
    }

    private Direction2Int getDirection2Int(int offsetX, int offsetZ) {

        boolean north = (offsetZ > CHUNK_SIZE);
        boolean south = (offsetZ < 0);
        boolean east = (offsetX > CHUNK_SIZE);
        boolean west = (offsetX < 0);

        if (north && east)
            return Direction2Int.NORTHEAST;
        if (north && west)
            return Direction2Int.NORTHWEST;
        if (south && east)
            return Direction2Int.SOUTHEAST;
        if (south && west)
            return Direction2Int.SOUTHWEST;

        if (north)
            return Direction2Int.NORTH;
        if (south)
            return Direction2Int.SOUTH;
        if (east)
            return Direction2Int.EAST;
        if (west)
            return Direction2Int.WEST;

        return null;
    }

    private int blendColors() {

        float r = 0, g = 0, b = 0, a = 0;
        int count = 0;

        for (Color c : blendColors) {
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

        // Pack back into int (LibGDX uses ABGR by default for rgba8888).
        return Color.rgba8888(r, g, b, a);
    }

    private void packQuad(
            IntArray quads,
            int xyz,
            int width,
            int height,
            int directionIndex,
            int blockID,
            int color0,
            int color1,
            int color2,
            int color3,
            int materialDataID) {

        quads.add(xyz);
        quads.add(width);
        quads.add(height);
        quads.add(directionIndex);
        quads.add(blockID);
        quads.add(color0);
        quads.add(color1);
        quads.add(color2);
        quads.add(color3);

        quadCounts.addTo(materialDataID, 1);
    }

    // Mesh \\

    private void buildFromQuads(SubChunkMesh subChunkMesh, int subChunkIndex) {

        // Prepare map of materialID -> BatchWriter
        Int2ObjectOpenHashMap<BatchWriter> writers = new Int2ObjectOpenHashMap<>();

        for (int i = 0; i < quads.size; i += QUAD_SIZE) {

            // Quad data
            int xyz = quads.get(i);
            int width = quads.get(i + 1);
            int height = quads.get(i + 2);
            int directionIndex = quads.get(i + 3);
            int blockID = quads.get(i + 4);

            int color0 = quads.get(i + 5);
            int color1 = quads.get(i + 6);
            int color2 = quads.get(i + 7);
            int color3 = quads.get(i + 8);

            Direction3Int direction = Direction3Int.DIRECTIONS[directionIndex];
            Direction3Int[] tangents = Direction3Int.getTangents(direction);

            Direction3Int dirA = tangents[0];
            Direction3Int dirB = tangents[1];

            int baseX = packedCoordinate3Int.unpackX(xyz);
            int baseY = packedCoordinate3Int.unpackY(xyz) + (subChunkIndex * CHUNK_SIZE);
            int baseZ = packedCoordinate3Int.unpackZ(xyz);

            // Quad verts
            int vert0X = baseX;
            int vert0Y = baseY;
            int vert0Z = baseZ;

            int vert1X = baseX + dirA.x * width;
            int vert1Y = baseY + dirA.y * width;
            int vert1Z = baseZ + dirA.z * width;

            int vert2X = vert1X + dirB.x * height;
            int vert2Y = vert1Y + dirB.y * height;
            int vert2Z = vert1Z + dirB.z * height;

            int vert3X = baseX + dirB.x * height;
            int vert3Y = baseY + dirB.y * height;
            int vert3Z = baseZ + dirB.z * height;

            // Block Data
            Block block = worldSystem.getBlockByID(blockID);
            UVRect uv = block.getUVForSide(direction);
            MaterialData materialData = block.getMaterialDataForSide(direction);
            int matId = materialData.id;

            // Ensure writer exists for this material
            BatchWriter writer = writers.get(matId);

            if (writer == null) {

                int quadCount = quadCounts.get(matId);
                writer = new BatchWriter(quadCount);
                writers.put(matId, writer);
            }

            // Normal
            float nx = direction.x;
            float ny = direction.y;
            float nz = direction.z;

            // Colors: convert packed int (rgba8888) -> Color -> packed float
            Color.rgba8888ToColor(tmpColor, color0);
            float c0 = tmpColor.toFloatBits();
            Color.rgba8888ToColor(tmpColor, color1);
            float c1 = tmpColor.toFloatBits();
            Color.rgba8888ToColor(tmpColor, color2);
            float c2 = tmpColor.toFloatBits();
            Color.rgba8888ToColor(tmpColor, color3);
            float c3 = tmpColor.toFloatBits();

            // UVs from UVRect
            float u0 = uv.u0, v0 = uv.v0;
            float u1 = uv.u1, v1 = uv.v0;
            float u2 = uv.u1, v2 = uv.v1;
            float u3 = uv.u0, v3 = uv.v1;

            // Push quad into writer
            writer.addQuad(
                    vert0X, vert0Y, vert0Z,
                    vert1X, vert1Y, vert1Z,
                    vert2X, vert2Y, vert2Z,
                    vert3X, vert3Y, vert3Z,
                    nx, ny, nz,
                    c0, c1, c2, c3,
                    u0, v0,
                    u1, v1,
                    u2, v2,
                    u3, v3);
        }

        // Convert writers into MaterialBatches
        Int2ObjectOpenHashMap<SubChunkPacket.MaterialBatch> batches = new Int2ObjectOpenHashMap<>();
        writers.forEach((matId, writer) -> {

            FloatArray vertsFA = new FloatArray(writer.vertPos);

            for (int vi = 0; vi < writer.vertPos; vi++)
                vertsFA.add(writer.verts[vi]);

            ShortArray indsFA = new ShortArray(writer.indPos);

            for (int ii = 0; ii < writer.indPos; ii++)
                indsFA.add(writer.inds[ii]);

            batches.putIfAbsent(matId, new SubChunkPacket.MaterialBatch(
                    matId,
                    writer.verts, writer.vertPos,
                    writer.inds, writer.indPos));
        });

        // Create and submit packet
        SubChunkPacket packet = new SubChunkPacket(subChunkIndex, batches);
        subChunkMesh.submit(packet);
    }

    // Utility \\

    private static final class BatchWriter {

        final float[] verts;
        final short[] inds;
        int vertPos = 0;
        int indPos = 0;
        short baseIndex = 0;

        BatchWriter(int quadCount) {

            verts = new float[quadCount * 4 * SubChunkMesh.VERT_STRIDE];
            inds = new short[quadCount * 6];
        }

        void addQuad(
                float x0, float y0, float z0,
                float x1, float y1, float z1,
                float x2, float y2, float z2,
                float x3, float y3, float z3,
                float nx, float ny, float nz,
                float c0, float c1, float c2, float c3,
                float u0, float v0,
                float u1, float v1,
                float u2, float v2,
                float u3, float v3) {

            pushVertex(x0, y0, z0, nx, ny, nz, c0, u0, v0);
            pushVertex(x1, y1, z1, nx, ny, nz, c1, u1, v1);
            pushVertex(x2, y2, z2, nx, ny, nz, c2, u2, v2);
            pushVertex(x3, y3, z3, nx, ny, nz, c3, u3, v3);

            // two triangles
            inds[indPos++] = (short) (baseIndex + 0);
            inds[indPos++] = (short) (baseIndex + 1);
            inds[indPos++] = (short) (baseIndex + 2);
            inds[indPos++] = (short) (baseIndex + 2);
            inds[indPos++] = (short) (baseIndex + 3);
            inds[indPos++] = (short) (baseIndex + 0);

            baseIndex += 4;
        }

        private void pushVertex(float x, float y, float z,
                float nx, float ny, float nz,
                float c,
                float u, float v) {

            verts[vertPos++] = x;
            verts[vertPos++] = y;
            verts[vertPos++] = z;
            verts[vertPos++] = nx;
            verts[vertPos++] = ny;
            verts[vertPos++] = nz;
            verts[vertPos++] = c;
            verts[vertPos++] = u;
            verts[vertPos++] = v;
        }
    }

    private enum NeighborBlockDirection {

        UPPER_NORTH_EAST(0, 0, 0),
        UPPER_NORTH_WEST(-1, 0, 0),
        UPPER_SOUTH_EAST(0, 0, -1),
        UPPER_SOUTH_WEST(-1, 0, -1),
        LOWER_NORTH_EAST(0, -1, 0),
        LOWER_NORTH_WEST(-1, -1, 0),
        LOWER_SOUTH_EAST(0, -1, -1),
        LOWER_SOUTH_WEST(-1, -1, -1);

        public final int x, y, z;

        NeighborBlockDirection(int x, int y, int z) {

            this.x = x;
            this.y = y;
            this.z = z;
        }

        public static final NeighborBlockDirection[] VALUES = {

                UPPER_NORTH_EAST,
                UPPER_NORTH_WEST,
                UPPER_SOUTH_EAST,
                UPPER_SOUTH_WEST,
                LOWER_NORTH_EAST,
                LOWER_NORTH_WEST,
                LOWER_SOUTH_EAST,
                LOWER_SOUTH_WEST
        };
    }

    public class AbortBuildException extends RuntimeException {
    }
}
