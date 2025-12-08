package com.AdventureRPG.worldmanager.chunks;

import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;

import com.AdventureRPG.core.kernel.EngineSetting;
import com.AdventureRPG.core.renderpipeline.materialmanager.MaterialData;
import com.AdventureRPG.core.renderpipeline.texturemanager.TextureManager.UVRect;
import com.AdventureRPG.core.util.Methematics.Extras.Direction2Int;
import com.AdventureRPG.core.util.Methematics.Extras.Direction3Int;
import com.AdventureRPG.worldmanager.WorldManager;
import com.AdventureRPG.worldmanager.SubChunks.SubChunk;
import com.AdventureRPG.worldmanager.SubChunks.SubChunkMesh;
import com.AdventureRPG.worldmanager.biomes.BiomeSystem;
import com.AdventureRPG.worldmanager.blocks.Block
import com.AdventureRPG.worldmanager.Util.PackedCoordinate3Int;
import com.AdventureRPG.worldmanager.subchunks.SubChunk;
import com.AdventureRPG.worldmanager.subchunks.SubChunkMesh;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.IntArray;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;

public class Builder {

    // Game Manager
    private final WorldManager worldManager;
    private final BlockSystem blockSystem;
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

    // Multi-Thread
    private final AtomicBoolean threadSafety;

    // Base \\

    public Builder(WorldManager worldManager, Chunk chunk) {

        // Game Manager
        this.worldManager = worldManager;
        this.blockSystem = worldManager.blockSystem;
        this.packedCoordinate3Int = worldManager.packedCoordinate3Int;
        this.biomeSystem = worldManager.biomeSystem;

        // Settings
        this.CHUNK_SIZE = EngineSetting.CHUNK_SIZE;
        this.WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;

        // Data
        this.chunk = chunk;
        this.quads = new IntArray(EngineSetting.CHUNK_VERT_BUFFER);
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

        // Multi-Thread
        this.threadSafety = new AtomicBoolean(false);
    }

    // Build \\

    public boolean build(int subChunkIndex) {

        boolean output = false;

        // Attempt to lock
        if (!threadSafety.compareAndSet(false, true))
            throw new AbortBuildException();

        try {

            // Get the sub chunk from the input index
            SubChunk subChunk = chunk.getSubChunk(subChunkIndex);

            // Go through each and every pre-packed coordinate inside the chunk
            for (int index = 0; index < packedCoordinate3Int.chunkSize; index++) {

                // Grab the pre-packed coordinate
                int xyz = packedCoordinate3Int.getPackedBlockCoordinate(index);

                // Unpack x, y and z
                int x = packedCoordinate3Int.unpackX(xyz);
                int y = packedCoordinate3Int.unpackY(xyz);
                int z = packedCoordinate3Int.unpackZ(xyz);

                // Get the block and block type for the pre-packed coordinate
                int blockID = subChunk.getBlock(x, y, z);
                Type type = blockSystem.getBlockTypeByID(blockID); // TODO: Grab block instead?

                // Early exit for anything set to type NULL
                if (type == Type.NULL)
                    continue;

                // Get the biome type for the pre-packed coordinate
                int biomeID = subChunk.getBiome(x, y, z);

                // For this coordinate go through each direction
                for (int directionIndex = 0; directionIndex < 6; directionIndex++) {

                    // Grab the direction and batched set for the index
                    Direction3Int direction = Direction3Int.DIRECTIONS[directionIndex];
                    BitSet batchedSet = getBatchedSet(direction);

                    // If the block has already been combines with another block face skip
                    if (batchedSet.get(xyz))
                        continue;

                    // Assemble the face using the data so far
                    assembleFace(
                            batchedSet,
                            subChunk,
                            subChunkIndex,
                            x, y, z,
                            direction,
                            biomeID, blockID,
                            type);
                }
            }

            // If there is data build the mesh
            if (quads.size > 0)
                buildFromQuads(subChunk.subChunkMesh);

            output = true;
        }

        // Exit early and clear the lists if anything went wrong
        catch (AbortBuildException endEarly) {
            // TODO: Redo entire build error section
        }

        // Clear the data after a successful build
        finally {

            clearData();
            threadSafety.set(false);
        }

        return output;
    }

    // Clear all the lists for the next build
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

    // Grab the correct batched set to compare against
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

    // Assemble the face and combine like blocks into the same quad
    private void assembleFace(
            BitSet batchedSet,
            SubChunk subChunk,
            int subChunkIndex,
            int x, int y, int z,
            Direction3Int direction,
            int biomeID, int blockID,
            Type type) {

        // If this block does not need a quad for this face skip
        if (!blockFaceCheck(
                subChunkIndex,
                x, y, z,
                direction,
                type))
            return;

        // First step start with default values for the check
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
                        x, y, z,
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
                        x, y, z,
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

        // Pack the face using the data accumulated
        prepareFace(
                subChunk,
                subChunkIndex,
                x, y, z,
                sizeA, sizeB,
                direction,
                biomeID, blockID);
    }

    // The main method to stretch the face
    private boolean tryExpand(
            BitSet batchedSet,
            SubChunk subChunk,
            int subChunkIndex,
            int x, int y, int z,
            int currentSize, int otherSize,
            Direction3Int expandDir, Direction3Int otherDir,
            int biomeID, int blockID,
            Direction3Int direction,
            Type type) {

        // First step is to make sure we are within the same chunk
        if (currentSize >= CHUNK_SIZE)
            return false;

        tempBatchedBlocks.clear();

        // Calculate next base coordinate along expandDir
        int nextX = x + expandDir.x * currentSize;
        int nextY = y + expandDir.y * currentSize;
        int nextZ = z + expandDir.z * currentSize;

        // Used to keep the coordinates within a single chunk
        if (coordinatesOutOfBounds(nextX, nextY, nextZ))
            return false;

        // Loop across the perpendicular dimension
        for (int i = 0; i < otherSize; i++) {

            int xCheck = nextX + otherDir.x * i;
            int yCheck = nextY + otherDir.y * i;
            int zCheck = nextZ + otherDir.z * i;

            int xyz = packedCoordinate3Int.pack(xCheck, yCheck, zCheck);

            int comparativeBiomeID = subChunk.getBiome(xCheck, yCheck, zCheck);
            int comparativeBlockID = subChunk.getBlock(xCheck, yCheck, zCheck);

            if (biomeID != comparativeBiomeID ||
                    blockID != comparativeBlockID ||
                    batchedSet.get(xyz) ||
                    !blockFaceCheck(
                            subChunkIndex,
                            xCheck, yCheck, zCheck,
                            direction,
                            type))
                return false;

            tempBatchedBlocks.set(xyz);
        }

        batchedSet.or(tempBatchedBlocks);

        tempBatchedBlocks.clear();
        return true;
    }

    // Used to keep the coordinates within a single chunk
    private boolean coordinatesOutOfBounds(int x, int y, int z) {

        return (x >= CHUNK_SIZE ||
                y >= CHUNK_SIZE ||
                z >= CHUNK_SIZE);
    }

    // The main check to see if this block should have a face
    private boolean blockFaceCheck(
            int subChunkIndex,
            int x, int y, int z,
            Direction3Int direction,
            Type type) {

        // Get raw coordinates to compare against
        int aX = x + direction.x;
        int aY = y + direction.y;
        int aZ = z + direction.z;

        // Get the correct sub chunk to compare against
        SubChunk comparativeSubChunk = getComparativeSubChunk(
                subChunkIndex,
                aX, aY, aZ,
                direction);

        // Get the correct values always wrapped within a single chunk
        int bX = addAndWrapAxis(direction.x, x);
        int bY = addAndWrapAxis(direction.y, y);
        int bZ = addAndWrapAxis(direction.z, z);

        // If the sub chunk is null return true
        if (comparativeSubChunk == null)
            return true;

        // Get the type of the neighbor block to compare against
        int blockID = comparativeSubChunk.getBlock(bX, bY, bZ);
        Type comparativeType = blockSystem.getBlockTypeByID(blockID);

        return comparativeType != type;
    }

    // Method to grab the neighbor sub chunk
    private SubChunk getComparativeSubChunk(
            int subChunkIndex,
            int aX, int aY, int aZ,
            Direction3Int direction) {

        // If the current coordinates are not over the edge return the same sub chunk
        if (!isBlockOverEdge(aX, aY, aZ))
            return chunk.getSubChunk(subChunkIndex);

        // If the direction is vertical return within the same chunk
        if (direction == Direction3Int.UP || direction == Direction3Int.DOWN) {

            int outputSubChunk = subChunkIndex + direction.y;

            if (outputSubChunk >= 0 && outputSubChunk < WORLD_HEIGHT)
                return chunk.getSubChunk(outputSubChunk);

            else
                return null;
        }

        // Otherwise grab the correct neighbor subchunk
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

    private boolean isBlockOverEdge(
            int aX, int aY, int aZ) {
        return (aX < 0 || aX >= CHUNK_SIZE ||
                aY < 0 || aY >= CHUNK_SIZE ||
                aZ < 0 || aZ >= CHUNK_SIZE);
    }

    public int addAndWrapAxis(int axisA, int axisB) {
        return (axisA + axisB) & (CHUNK_SIZE - 1);
    }

    // Finalize \\

    private void prepareFace(
            SubChunk subChunk,
            int subChunkIndex,
            int x, int y, int z,
            int width, int height,
            Direction3Int direction,
            int biomeID, int blockID) {

        // Offsett all positive directions
        x = convertToVertSpace(x, direction.x);
        y = convertToVertSpace(y, direction.y);
        z = convertToVertSpace(z, direction.z);

        int xyz = packedCoordinate3Int.pack(x, y, z);

        int directionIndex = direction.index;

        Direction3Int[] tangents = Direction3Int.getTangents(direction);
        Direction3Int dirA = tangents[0];
        Direction3Int dirB = tangents[1];

        // base
        int vert0X = x;
        int vert0Y = y;
        int vert0Z = z;

        // base + width
        int vert1X = x + dirA.x * width;
        int vert1Y = y + dirA.y * width;
        int vert1Z = z + dirA.z * width;

        // base + width + height
        int vert2X = vert1X + dirB.x * height;
        int vert2Y = vert1Y + dirB.y * height;
        int vert2Z = vert1Z + dirB.z * height;

        // base + height
        int vert3X = x + dirB.x * height;
        int vert3Y = y + dirB.y * height;
        int vert3Z = z + dirB.z * height;

        int color0 = getVertColor(subChunk, subChunkIndex, vert0X, vert0Y, vert0Z);
        int color1 = getVertColor(subChunk, subChunkIndex, vert1X, vert1Y, vert1Z);
        int color2 = getVertColor(subChunk, subChunkIndex, vert2X, vert2Y, vert2Z);
        int color3 = getVertColor(subChunk, subChunkIndex, vert3X, vert3Y, vert3Z);

        int materialDataID = blockSystem.getBlockByID(blockID).getMaterialDataForSide(direction).id;

        packQuad(
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

    public int convertToVertSpace(int blockAxis, int direction3IntAxis) {
        return blockAxis += (direction3IntAxis > 0 ? 1 : 0);
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

        if (vertAxis < 0)
            return CHUNK_SIZE - 1;

        if (vertAxis >= CHUNK_SIZE)
            return 0;

        return vertAxis;
    }

    private SubChunk getNeighborSubChunk(
            SubChunk subChunk,
            int subChunkIndex,
            int blockX, int blockY, int blockZ,
            int offsetX, int offsetY, int offsetZ,
            NeighborBlockDirection direction) {

        if (!isVertOverEdge(offsetX, offsetY, offsetZ))
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

    private boolean isVertOverEdge(int offsetX, int offsetY, int offsetZ) {
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

    // TODO: May need to make this more efficient
    private void buildFromQuads(SubChunkMesh subChunkMesh) {

        subChunkMesh.clear();
        final int stride = EngineSetting.STATIC_VERT_STRIDE;

        for (int i = 0; i < quads.size; i += QUAD_SIZE) {

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

            int x = packedCoordinate3Int.unpackX(xyz);
            int y = packedCoordinate3Int.unpackY(xyz);
            int z = packedCoordinate3Int.unpackZ(xyz);

            // Compute quad vertex positions
            int[][] verts = new int[4][3];
            verts[0] = new int[] { x, y, z };
            verts[1] = new int[] { x + dirA.x * width, y + dirA.y * width, z + dirA.z * width };
            verts[2] = new int[] { verts[1][0] + dirB.x * height, verts[1][1] + dirB.y * height,
                    verts[1][2] + dirB.z * height };
            verts[3] = new int[] { x + dirB.x * height, y + dirB.y * height, z + dirB.z * height };

            // Block data
            Block block = blockSystem.getBlockByID(blockID);
            UVRect uv = block.getUVForSide(direction);
            MaterialData materialData = block.getMaterialDataForSide(direction);
            int matId = materialData.id;

            // Colors
            Color.rgba8888ToColor(tmpColor, color0);
            float c0 = tmpColor.toFloatBits();
            Color.rgba8888ToColor(tmpColor, color1);
            float c1 = tmpColor.toFloatBits();
            Color.rgba8888ToColor(tmpColor, color2);
            float c2 = tmpColor.toFloatBits();
            Color.rgba8888ToColor(tmpColor, color3);
            float c3 = tmpColor.toFloatBits();

            // UVs
            float[][] uvs = {
                    { uv.u0, uv.v0 },
                    { uv.u1, uv.v0 },
                    { uv.u1, uv.v1 },
                    { uv.u0, uv.v1 }
            };

            // Pack vertices into float array
            float[] quadVertices = new float[stride * 4];

            for (int vi = 0; vi < 4; vi++) {

                int base = vi * stride;

                quadVertices[base + 0] = verts[vi][0];
                quadVertices[base + 1] = verts[vi][1];
                quadVertices[base + 2] = verts[vi][2];
                quadVertices[base + 3] = direction.x;
                quadVertices[base + 4] = direction.y;
                quadVertices[base + 5] = direction.z;
                quadVertices[base + 6] = (vi == 0 ? c0 : vi == 1 ? c1 : vi == 2 ? c2 : c3);
                quadVertices[base + 7] = uvs[vi][0];
                quadVertices[base + 8] = uvs[vi][1];
            }

            subChunkMesh.addVertices(matId, quadVertices);
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

    // Accessible \\

    public boolean isRunning() {
        return threadSafety.get();
    }
}
