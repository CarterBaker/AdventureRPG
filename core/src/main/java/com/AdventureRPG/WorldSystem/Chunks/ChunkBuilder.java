package com.AdventureRPG.WorldSystem.Chunks;

import java.util.BitSet;

import com.AdventureRPG.MaterialManager.MaterialManager;
import com.AdventureRPG.TextureManager.TextureManager;
import com.AdventureRPG.TextureManager.TextureManager.UVRect;
import com.AdventureRPG.Util.Direction2Int;
import com.AdventureRPG.Util.Direction3Int;
import com.AdventureRPG.WorldSystem.PackedCoordinate3Int;
import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Biomes.BiomeSystem;
import com.AdventureRPG.WorldSystem.Blocks.Block;
import com.AdventureRPG.WorldSystem.Blocks.Type;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.utils.IntArray;

public class ChunkBuilder {

    // Game Manager
    private final TextureManager textureManager;
    private final MaterialManager materialManager;
    private final WorldSystem worldSystem;
    private final PackedCoordinate3Int packedCoordinate3Int;
    private final BiomeSystem biomeSystem;

    // Settings
    private final int CHUNK_SIZE;
    private final int WORLD_HEIGHT;

    // Data
    private IntArray quads;
    private final int QUAD_SIZE = 9;
    private BitSet passedCoordinates;
    private BitSet batchedBlocks;
    private Color[] blendColors;

    // Base \\

    public ChunkBuilder(WorldSystem worldSystem) {

        // Game Manager
        this.textureManager = worldSystem.textureManager;
        this.materialManager = worldSystem.materialManager;
        this.worldSystem = worldSystem;
        this.packedCoordinate3Int = worldSystem.packedCoordinate3Int;
        this.biomeSystem = worldSystem.biomeSystem;

        // Settings
        this.CHUNK_SIZE = worldSystem.settings.CHUNK_SIZE;
        this.WORLD_HEIGHT = worldSystem.settings.WORLD_HEIGHT;

        // Data
        this.quads = new IntArray(worldSystem.settings.CHUNK_VERT_BUFFER);
        this.passedCoordinates = new BitSet(packedCoordinate3Int.chunkSize);
        this.batchedBlocks = new BitSet(packedCoordinate3Int.chunkSize);
        this.blendColors = new Color[8];
    }

    // Data \\

    public void build(Chunk chunk, int subChunkIndex) {

        SubChunk subChunk = chunk.getSubChunk(subChunkIndex);

        for (int axisIndex = 0; axisIndex < Axis.values().length; axisIndex++) {

            Axis axis = Axis.VALUES[axisIndex];

            for (int index = 0; index < packedCoordinate3Int.chunkSize; index++) {

                int xyz = packedCoordinate3Int.getPackedBlockCoordinate(index);

                if (batchedBlocks.get(xyz))
                    continue;

                int aX = packedCoordinate3Int.unpackX(xyz);
                int aY = packedCoordinate3Int.unpackY(xyz);
                int aZ = packedCoordinate3Int.unpackZ(xyz);

                int blockID = subChunk.getBlock(aX, aY, aZ);
                Type type = worldSystem.getBlockType(blockID);

                if (type == Type.NULL)
                    continue;

                int biomeID = subChunk.getBiome(aX, aY, aZ);

                for (int directionIndex = 0; directionIndex < 2; directionIndex++) {

                    Direction3Int direction = axis.getDirection(directionIndex);

                    assembleFace(
                            quads,
                            chunk, subChunk,
                            subChunkIndex,
                            aX, aY, aZ,
                            axis, direction,
                            biomeID, blockID,
                            type);
                }
            }
        }

        if (quads.size > 0)
            buildFromQuads(subChunk.chunkMesh, subChunkIndex);

        quads.clear();
        batchedBlocks.clear();
    }

    private void assembleFace(
            IntArray quads,
            Chunk chunk, SubChunk subChunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            Axis axis, Direction3Int direction,
            int biomeID, int blockID,
            Type type) {

        if (!blockFaceCheck(
                chunk,
                subChunkIndex,
                aX, aY, aZ,
                axis, direction,
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
                        chunk, subChunk,
                        subChunkIndex,
                        aX, aY, aZ,
                        sizeA, sizeB,
                        comparativeDirectionA, comparativeDirectionB,
                        biomeID, blockID,
                        axis, direction,
                        type))
                    sizeA++;

                else
                    checkA = false;
            }

            // expand along B
            if (checkB) {

                if (tryExpand(
                        chunk, subChunk,
                        subChunkIndex,
                        aX, aY, aZ,
                        sizeB, sizeA,
                        comparativeDirectionB, comparativeDirectionA,
                        biomeID, blockID,
                        axis, direction,
                        type))
                    sizeB++;

                else
                    checkB = false;
            }

        }

        while (checkA || checkB);

        prepareFace(
                quads,
                chunk, subChunk,
                subChunkIndex,
                aX, aY, aZ,
                sizeA, sizeB,
                direction,
                biomeID, blockID);
    }

    private boolean tryExpand(
            Chunk chunk, SubChunk subChunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            int currentSize, int otherSize,
            Direction3Int expandDir, Direction3Int otherDir,
            int biomeID, int blockID,
            Axis axis, Direction3Int direction,
            Type type) {

        if (currentSize > CHUNK_SIZE)
            return false;

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
                    batchedBlocks.get(xyz) ||
                    !blockFaceCheck(chunk, subChunkIndex, checkX, checkY, checkZ, axis, direction, type)) {
                return false;
            }

            passedCoordinates.set(xyz);
        }

        batchedBlocks.or(passedCoordinates);
        passedCoordinates.clear();

        return true;
    }

    private boolean coordinatesOutOfBounds(int x, int y, int z) {

        return (x >= CHUNK_SIZE ||
                y >= CHUNK_SIZE ||
                z >= CHUNK_SIZE);
    }

    private boolean blockFaceCheck(
            Chunk chunk,
            int subChunkIndex,
            int aX, int aY, int aZ,
            Axis axis, Direction3Int direction,
            Type type) {

        int bX = packedCoordinate3Int.addAndWrapAxis(direction.x, aX);
        int bY = packedCoordinate3Int.addAndWrapAxis(direction.y, aY);
        int bZ = packedCoordinate3Int.addAndWrapAxis(direction.z, aZ);

        SubChunk comparativeSubChunk = getComparativeSubChunk(
                chunk,
                subChunkIndex,
                bX, bY, bZ,
                direction);

        int blockID = comparativeSubChunk.getBlock(bX, bY, bZ);
        Type comparativeType = worldSystem.getBlockType(blockID);

        return comparativeType != type;
    }

    private SubChunk getComparativeSubChunk(
            Chunk chunk,
            int subChunkIndex,
            int bX, int bY, int bZ,
            Direction3Int direction) {

        boolean isOverEdge = packedCoordinate3Int.isOverEdge(bX, bY, bZ, direction);

        if (!isOverEdge)
            return chunk.getSubChunk(subChunkIndex);

        if (direction == Direction3Int.UP || direction == Direction3Int.DOWN) {

            int outputSubChunk = subChunkIndex + direction.y;

            if (outputSubChunk > 0 && outputSubChunk < WORLD_HEIGHT)
                return chunk.getSubChunk(outputSubChunk);

            else
                return null;
        }

        else {

            Direction2Int direction2Int = direction.direction2Int;
            Chunk neighborChunk = chunk.getNeighborChunk(direction2Int);
            SubChunk outputSubChunk = neighborChunk.getSubChunk(subChunkIndex);

            return outputSubChunk;
        }
    }

    private void prepareFace(
            IntArray quads,
            Chunk chunk, SubChunk subChunk,
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

        int color0 = getVertColor(chunk, subChunk, subChunkIndex, vert0X, vert0Y, vert0Z);
        int color1 = getVertColor(chunk, subChunk, subChunkIndex, vert1X, vert1Y, vert1Z);
        int color2 = getVertColor(chunk, subChunk, subChunkIndex, vert2X, vert2Y, vert2Z);
        int color3 = getVertColor(chunk, subChunk, subChunkIndex, vert3X, vert3Y, vert3Z);

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
                color3);
    }

    private int getVertColor(
            Chunk chunk, SubChunk subChunk,
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

            int biomeID = 0;

            SubChunk neighborSubChunk = getNeighborSubChunk(
                    chunk, subChunk,
                    subChunkIndex,
                    blockX, blockY, blockZ,
                    offsetX, offsetY, offsetZ,
                    direction);

            if (neighborSubChunk != null) {

                biomeID = neighborSubChunk.getBiome(blockX, blockY, blockZ);
                blendColors[index] = biomeSystem.getBiomeByID(biomeID).biomeColor;
            }

            else
                blendColors[index] = blendColors[0];
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
            Chunk chunk, SubChunk subChunk,
            int subChunkIndex,
            int blockX, int blockY, int blockZ,
            int offsetX, int offsetY, int offsetZ,
            NeighborBlockDirection direction) {

        if (isOverEdge(offsetX, offsetY, offsetZ))
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

        if (offsetX < 0 || offsetX >= CHUNK_SIZE ||
                offsetY < 0 || offsetY >= CHUNK_SIZE ||
                offsetZ < 0 || offsetZ >= CHUNK_SIZE)
            return true;

        return false;
    }

    private Direction2Int getDirection2Int(int offsetX, int offsetZ) {

        boolean north = (offsetZ <= CHUNK_SIZE);
        boolean south = (offsetZ > 0);
        boolean east = (offsetX <= CHUNK_SIZE);
        boolean west = (offsetX > 0);

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
            int color3) {

        quads.add(xyz);
        quads.add(width);
        quads.add(height);
        quads.add(directionIndex);
        quads.add(blockID);
        quads.add(color0);
        quads.add(color1);
        quads.add(color2);
        quads.add(color3);
    }

    // Mesh \\

    private void buildFromQuads(ChunkMesh chunkMesh, int subChunkIndex) {

        chunkMesh.clear();

        // temporary color object to unpack rgba8888 ints -> floats
        com.badlogic.gdx.graphics.Color tmpColor = new com.badlogic.gdx.graphics.Color();

        for (int i = 0; i < quads.size; i += QUAD_SIZE) {

            // Quad
            int xyz = quads.get(i);
            int width = quads.get(i + 1);
            int height = quads.get(i + 2);
            int directionIndex = quads.get(i + 3);
            int blockID = quads.get(i + 4);

            int c0 = quads.get(i + 5);
            int c1 = quads.get(i + 6);
            int c2 = quads.get(i + 7);
            int c3 = quads.get(i + 8);

            // Unpack
            Direction3Int direction = Direction3Int.DIRECTIONS[directionIndex];
            Direction3Int[] tangents = Direction3Int.getTangents(direction);

            Direction3Int dirA = tangents[0];
            Direction3Int dirB = tangents[1];

            int baseX = packedCoordinate3Int.unpackX(xyz);
            // convert subchunk-local Y into world Y (so the mesh is continuous vertically)
            int baseY = packedCoordinate3Int.unpackY(xyz) + (subChunkIndex * CHUNK_SIZE);
            int baseZ = packedCoordinate3Int.unpackZ(xyz);

            // base
            int vert0X = baseX;
            int vert0Y = baseY;
            int vert0Z = baseZ;

            // base + width
            int vert1X = baseX + dirA.x * width;
            int vert1Y = baseY + dirA.y * width;
            int vert1Z = baseZ + dirA.z * width;

            // base + width + height
            int vert2X = vert1X + dirB.x * height;
            int vert2Y = vert1Y + dirB.y * height;
            int vert2Z = vert1Z + dirB.z * height;

            // base + height
            int vert3X = baseX + dirB.x * height;
            int vert3Y = baseY + dirB.y * height;
            int vert3Z = baseZ + dirB.z * height;

            Block block = worldSystem.getBlockByID(blockID);

            UVRect uv = block.getUVForSide(direction, textureManager);
            int material = block.getMatIDForSide(direction);

            // normal vector (direction points outwards)
            float nx = direction.x;
            float ny = direction.y;
            float nz = direction.z;

            // base index before we add these 4 vertices
            int baseIndex = chunkMesh.getVertexCount();

            // Vertex 0 (vert0) -> use uv.u0, uv.v0
            com.badlogic.gdx.graphics.Color.rgba8888ToColor(tmpColor, c0);
            chunkMesh.addVertex(
                    (float) vert0X, (float) vert0Y, (float) vert0Z,
                    nx, ny, nz,
                    uv.u0, uv.v0,
                    tmpColor.r, tmpColor.g, tmpColor.b, tmpColor.a);

            // Vertex 1 (vert1) -> uv.u1, uv.v0
            com.badlogic.gdx.graphics.Color.rgba8888ToColor(tmpColor, c1);
            chunkMesh.addVertex(
                    (float) vert1X, (float) vert1Y, (float) vert1Z,
                    nx, ny, nz,
                    uv.u1, uv.v0,
                    tmpColor.r, tmpColor.g, tmpColor.b, tmpColor.a);

            // Vertex 2 (vert2) -> uv.u1, uv.v1
            com.badlogic.gdx.graphics.Color.rgba8888ToColor(tmpColor, c2);
            chunkMesh.addVertex(
                    (float) vert2X, (float) vert2Y, (float) vert2Z,
                    nx, ny, nz,
                    uv.u1, uv.v1,
                    tmpColor.r, tmpColor.g, tmpColor.b, tmpColor.a);

            // Vertex 3 (vert3) -> uv.u0, uv.v1
            com.badlogic.gdx.graphics.Color.rgba8888ToColor(tmpColor, c3);
            chunkMesh.addVertex(
                    (float) vert3X, (float) vert3Y, (float) vert3Z,
                    nx, ny, nz,
                    uv.u0, uv.v1,
                    tmpColor.r, tmpColor.g, tmpColor.b, tmpColor.a);

            // Indices: two triangles (0,1,2) and (2,3,0)
            // NOTE: If you observe some faces being culled incorrectly due to winding,
            // flip the order for specific directions (swap 1 & 2, etc).
            chunkMesh.addIndex(material, (short) (baseIndex + 0));
            chunkMesh.addIndex(material, (short) (baseIndex + 1));
            chunkMesh.addIndex(material, (short) (baseIndex + 2));

            chunkMesh.addIndex(material, (short) (baseIndex + 2));
            chunkMesh.addIndex(material, (short) (baseIndex + 3));
            chunkMesh.addIndex(material, (short) (baseIndex + 0));
        }
    }

    // Utility \\

    private enum Axis {

        X(Direction3Int.EAST, Direction3Int.WEST),
        Y(Direction3Int.UP, Direction3Int.DOWN),
        Z(Direction3Int.NORTH, Direction3Int.SOUTH);

        private final Direction3Int[] directions;

        Axis(Direction3Int pos, Direction3Int neg) {

            this.directions = new Direction3Int[] { pos, neg };
        }

        public Direction3Int getDirection(int directionIndex) {
            return directions[directionIndex];
        }

        public static final Axis[] VALUES = { X, Y, Z };
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
}
