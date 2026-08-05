package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborStruct;
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

class LiquidGeometryBranch extends BranchPackage {

    /*
     * Geometry branch for liquid blocks. Emits one unit quad per exposed face
     * — never greedily merged, since a liquid's fill level can change on any
     * flow tick — using the exact vertex layout every other geometry branch
     * writes, so liquid meshes upload through the same chunk VAO. A face is
     * exposed whenever its neighbor is not the identical liquid block, so
     * adjoining faces between two different liquids (or a liquid and any
     * solid) still render. Liquids carry no rotation and are shaded by
     * material and biome color alone, so no texture tiling or bevel data is
     * written.
     */

    // Internal
    private SubChunkInstance ERROR;

    private static final int WORLD_HEIGHT = EngineSetting.WORLD_HEIGHT;

    // Internal \\

    @Override
    protected void create() {
        this.ERROR = create(SubChunkInstance.class);
    }

    // Build \\

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

        if (!hasExposedFace(chunkInstance, subChunkInstance, xyz, direction3Vector, blockHandle))
            return false;

        finalizeFace(verts, direction3Vector, blockHandle, xyz, biomeHandle);

        return true;
    }

    // Face Exposure \\

    private boolean hasExposedFace(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle) {

        SubChunkInstance comparativeSubChunkInstance = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, xyz, direction3Vector);

        if (comparativeSubChunkInstance == ERROR)
            return false;

        if (comparativeSubChunkInstance == null) {
            byte subY = (byte) subChunkInstance.getCoordinate();
            return (direction3Vector == Direction3Vector.DOWN && subY == 0)
                    || (direction3Vector == Direction3Vector.UP && subY == WORLD_HEIGHT - 1);
        }

        int comparativeXYZ = ChunkCoordinate3Int.getNeighborAndWrap(xyz, direction3Vector);
        short comparativeBlockID = comparativeSubChunkInstance.getBlockPaletteHandle().getBlock(comparativeXYZ);

        return comparativeBlockID != blockHandle.getBlockID();
    }

    private SubChunkInstance getComparativeSubChunkInstance(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector) {

        if (!ChunkCoordinate3Int.isAtEdge(xyz, direction3Vector))
            return subChunkInstance;

        byte subChunkCoordinate = (byte) subChunkInstance.getCoordinate();

        if (direction3Vector == Direction3Vector.UP || direction3Vector == Direction3Vector.DOWN) {
            byte comparativeSubChunkCoordinate = (byte) (subChunkCoordinate + direction3Vector.y);
            if (comparativeSubChunkCoordinate >= 0 && comparativeSubChunkCoordinate < WORLD_HEIGHT)
                return chunkInstance.getSubChunk(comparativeSubChunkCoordinate);
            else
                return null;
        }

        Direction2Vector direction2Vector = direction3Vector.to2D();
        ChunkNeighborStruct chunkNeighborStruct = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborStruct.getNeighborChunk(direction2Vector.index);

        if (neighborChunkInstance == null)
            return ERROR;

        SubChunkInstance comparativeSubChunkInstance = neighborChunkInstance.getSubChunk(subChunkCoordinate);

        if (comparativeSubChunkInstance == null)
            return ERROR;

        return comparativeSubChunkInstance;
    }

    // Face Finalization \\

    private void finalizeFace(
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            Direction3Vector direction3Vector,
            BlockHandle blockHandle,
            int xyz,
            BiomeHandle biomeHandle) {

        Direction3Vector tangentA = Direction3Vector.getTangentA(direction3Vector);
        Direction3Vector tangentB = Direction3Vector.getTangentB(direction3Vector);

        int vert0XYZ = ChunkCoordinate3Int.convertToVertSpace(xyz, direction3Vector);
        int vert1XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentA, 1);
        int vert2XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert1XYZ, tangentB, 1);
        int vert3XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentB, 1);

        float nor = (float) direction3Vector.index;
        float color = Color.rgba8888(biomeHandle.getBiomeColor());
        float encodedFace = (float) (direction3Vector.ordinal() * 4);
        float quadSize = (float) (1 | (1 << 8));

        FloatArrayList buffer = verts.computeIfAbsent(blockHandle.getMaterialID(), k -> new FloatArrayList());

        writeVertex(buffer, vert0XYZ, nor, color, encodedFace, quadSize);
        writeVertex(buffer, vert1XYZ, nor, color, encodedFace, quadSize);
        writeVertex(buffer, vert2XYZ, nor, color, encodedFace, quadSize);
        writeVertex(buffer, vert3XYZ, nor, color, encodedFace, quadSize);
    }

    private void writeVertex(
            FloatArrayList buffer,
            int vertXYZ,
            float nor,
            float color,
            float encodedFace,
            float quadSize) {

        buffer.add((float) Coordinate3Int.unpackX(vertXYZ));
        buffer.add((float) Coordinate3Int.unpackY(vertXYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vertXYZ));
        buffer.add(nor);
        buffer.add(color);
        buffer.add(0f);
        buffer.add(0f);
        buffer.add(encodedFace);
        buffer.add(quadSize);
        buffer.add(0f);
        buffer.add(0f);
        buffer.add(0f);
        buffer.add(0f);
    }
}