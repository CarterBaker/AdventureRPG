package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinateUtility;
import application.bootstrap.worldpipeline.util.SubBlockUtility;
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
     * Geometry branch for liquid blocks. Settled water emits only greedily
     * merged UP faces; flowing water emits one quad per exposed face. Faces
     * against opaque blocks are dropped, and each vertex carries its fill level
     * plus surface and tidal flags for the water shader.
     */

    // Internal
    private BiomeManager biomeManager;
    private BlockManager blockManager;
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
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
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
            Color vertColorAccumulator) {

        // Settled water only ever shows its top face
        if (subChunkInstance.isLiquidStable() && direction3Vector != Direction3Vector.UP)
            return false;

        if (!hasExposedFace(chunkInstance, subChunkInstance, xyz, direction3Vector, blockHandle))
            return false;

        assembleQuad(
                chunkInstance,
                subChunkInstance,
                biomePaletteHandle,
                blockPaletteHandle,
                xyz,
                direction3Vector,
                biomeHandle,
                blockHandle,
                verts,
                accumulatedBatch,
                batchReturn);

        return true;
    }

    // Greedy Expansion \\

    private void assembleQuad(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            int xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            Int2ObjectOpenHashMap<FloatArrayList> verts,
            BitSet accumulatedBatch,
            BitSet batchReturn) {

        short level = blockPaletteHandle.getLiquidLevel(xyz);
        boolean tidal = subChunkInstance.isLiquidTidal(xyz);
        boolean tidalSurface = tidal
                && isBeneathOpenSurface(chunkInstance, subChunkInstance, xyz, blockHandle);

        byte sizeA = 1;
        byte sizeB = 1;

        // Only a settled subchunk pays for expansion — a flowing one rebuilds
        // every tick anyway, so merged runs would just be discarded unused.
        if (subChunkInstance.isLiquidStable()) {

            Direction3Vector tangentA = Direction3Vector.getTangentA(direction3Vector);
            Direction3Vector tangentB = Direction3Vector.getTangentB(direction3Vector);

            boolean checkA = true;
            boolean checkB = true;

            do {
                if (checkA) {
                    if (tryExpand(
                            chunkInstance, subChunkInstance, biomePaletteHandle, blockPaletteHandle,
                            xyz, direction3Vector, tangentA, tangentB, sizeA, sizeB,
                            level, tidal, biomeHandle, blockHandle, accumulatedBatch, batchReturn)) {
                        accumulatedBatch.or(batchReturn);
                        sizeA++;
                    } else
                        checkA = false;
                }

                if (checkB) {
                    if (tryExpand(
                            chunkInstance, subChunkInstance, biomePaletteHandle, blockPaletteHandle,
                            xyz, direction3Vector, tangentB, tangentA, sizeB, sizeA,
                            level, tidal, biomeHandle, blockHandle, accumulatedBatch, batchReturn)) {
                        accumulatedBatch.or(batchReturn);
                        sizeB++;
                    } else
                        checkB = false;
                }
            } while (checkA || checkB);
        }

        finalizeFace(verts, xyz, sizeA, sizeB, direction3Vector, biomeHandle, blockHandle, level, tidalSurface);
    }

    private boolean tryExpand(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            int xyz,
            Direction3Vector direction3Vector,
            Direction3Vector expandDirection,
            Direction3Vector tangentDirection,
            int currentSize,
            int tangentSize,
            short level,
            boolean tidal,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            BitSet accumulatedBatch,
            BitSet batchReturn) {

        if (currentSize >= CHUNK_SIZE)
            return false;

        int nextXYZ = ChunkCoordinateUtility.getNeighborWithOffset(xyz, expandDirection, currentSize);

        if (nextXYZ == -1)
            return false;

        for (int i = 0; i < tangentSize; i++) {

            int checkXYZ = ChunkCoordinateUtility.getNeighborWithOffset(nextXYZ, tangentDirection, i);

            if (checkXYZ == -1)
                return false;

            short comparativeBiomeID = biomePaletteHandle.getBlock(checkXYZ);
            BiomeHandle comparativeBiomeHandle = biomeManager.getBiomeHandleFromBiomeID(comparativeBiomeID);
            short comparativeBlockID = blockPaletteHandle.getBlock(checkXYZ);
            short comparativeLevel = blockPaletteHandle.getLiquidLevel(checkXYZ);

            if (comparativeBlockID != blockHandle.getBlockID() ||
                    comparativeBiomeHandle != biomeHandle ||
                    comparativeLevel != level ||
                    subChunkInstance.isLiquidTidal(checkXYZ) != tidal ||
                    accumulatedBatch.get(ChunkCoordinateUtility.getIndex(checkXYZ)) ||
                    !hasExposedFace(chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle)) {
                batchReturn.clear();
                return false;
            }

            batchReturn.set(ChunkCoordinateUtility.getIndex(checkXYZ));
        }

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

        int comparativeXYZ = ChunkCoordinateUtility.getNeighborAndWrap(xyz, direction3Vector);
        short comparativeBlockID = comparativeSubChunkInstance.getBlockPaletteHandle().getBlock(comparativeXYZ);

        // Same liquid on the other side is an internal boundary, except
        // sideways against a lower neighbor — that step down in the surface
        // is exactly what a flowing stream needs to show.
        if (comparativeBlockID == blockHandle.getBlockID())
            return isLateral(direction3Vector)
                    && comparativeSubChunkInstance.getLiquidLevel(comparativeXYZ)
                            < subChunkInstance.getLiquidLevel(xyz);

        // Any neighbor but a whole opaque cube exposes the face
        BlockHandle comparativeBlockHandle = blockManager.getBlockHandleFromBlockID(comparativeBlockID);

        if (SubBlockUtility.isSubdivided(comparativeSubChunkInstance.getSubBlockMask(comparativeXYZ)))
            return true;

        return comparativeBlockHandle.getGeometry() != DynamicGeometryType.FULL;
    }

    private boolean isBeneathOpenSurface(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            BlockHandle blockHandle) {

        SubChunkInstance aboveSubChunkInstance = getComparativeSubChunkInstance(
                chunkInstance, subChunkInstance, xyz, Direction3Vector.UP);

        if (aboveSubChunkInstance == null)
            return true;

        int aboveXYZ = ChunkCoordinateUtility.getNeighborAndWrap(xyz, Direction3Vector.UP);

        return aboveSubChunkInstance.getBlock(aboveXYZ) != blockHandle.getBlockID();
    }

    private boolean isLateral(Direction3Vector direction3Vector) {
        return direction3Vector != Direction3Vector.UP && direction3Vector != Direction3Vector.DOWN;
    }

    private SubChunkInstance getComparativeSubChunkInstance(
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            int xyz,
            Direction3Vector direction3Vector) {

        if (!ChunkCoordinateUtility.isAtEdge(xyz, direction3Vector))
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
        ChunkNeighborHandle chunkNeighborHandle = chunkInstance.getChunkNeighbors();
        ChunkInstance neighborChunkInstance = chunkNeighborHandle.getNeighborChunk(direction2Vector.index);

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
            int xyz,
            byte sizeA,
            byte sizeB,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short level,
            boolean tidalSurface) {

        Direction3Vector tangentA = Direction3Vector.getTangentA(direction3Vector);
        Direction3Vector tangentB = Direction3Vector.getTangentB(direction3Vector);

        int vert0XYZ = ChunkCoordinateUtility.convertToVertSpace(xyz, direction3Vector);
        int vert1XYZ = ChunkCoordinateUtility.getVertCoordinateFromOffset(vert0XYZ, tangentA, sizeA);
        int vert2XYZ = ChunkCoordinateUtility.getVertCoordinateFromOffset(vert1XYZ, tangentB, sizeB);
        int vert3XYZ = ChunkCoordinateUtility.getVertCoordinateFromOffset(vert0XYZ, tangentB, sizeB);

        // Whichever corners sit at the greatest Y are this face's "surface"
        // vertices — always all 4 for UP, none for DOWN, and the top pair
        // for a side face (or the top pair of a vertically-merged run).
        int y0 = Coordinate3Int.unpackY(vert0XYZ);
        int y1 = Coordinate3Int.unpackY(vert1XYZ);
        int y2 = Coordinate3Int.unpackY(vert2XYZ);
        int y3 = Coordinate3Int.unpackY(vert3XYZ);
        int maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));

        int meta = (direction3Vector.index & 0x7)
                | (((direction3Vector.ordinal() * 4) & 0x3F) << 3)
                | (((sizeA - 1) & 0xF) << 9)
                | (((sizeB - 1) & 0xF) << 13);

        float fMeta = (float) meta;
        float color = packColor(biomeHandle.getBiomeColor());
        float levelF = (float) level;

        FloatArrayList buffer = verts.computeIfAbsent(blockHandle.getMaterialID(), k -> new FloatArrayList());

        float surface0 = surfaceFlag(direction3Vector, y0, maxY);
        float surface1 = surfaceFlag(direction3Vector, y1, maxY);
        float surface2 = surfaceFlag(direction3Vector, y2, maxY);
        float surface3 = surfaceFlag(direction3Vector, y3, maxY);

        writeVertex(buffer, vert0XYZ, fMeta, color, levelF, surface0, tidalFlag(tidalSurface, surface0));
        writeVertex(buffer, vert1XYZ, fMeta, color, levelF, surface1, tidalFlag(tidalSurface, surface1));
        writeVertex(buffer, vert2XYZ, fMeta, color, levelF, surface2, tidalFlag(tidalSurface, surface2));
        writeVertex(buffer, vert3XYZ, fMeta, color, levelF, surface3, tidalFlag(tidalSurface, surface3));
    }

    private float surfaceFlag(Direction3Vector direction3Vector, int vertY, int maxY) {
        return direction3Vector != Direction3Vector.DOWN && vertY == maxY ? 1f : 0f;
    }

    private float tidalFlag(boolean tidalSurface, float isSurface) {
        return tidalSurface ? isSurface : 0f;
    }

    private float packColor(Color color) {

        int r = Math.round(Math.min(Math.max(color.r, 0f), 1f) * 255f);
        int g = Math.round(Math.min(Math.max(color.g, 0f), 1f) * 255f);
        int b = Math.round(Math.min(Math.max(color.b, 0f), 1f) * 255f);

        return (float) ((r << 16) | (g << 8) | b);
    }

    private void writeVertex(
            FloatArrayList buffer,
            int vertXYZ,
            float meta,
            float color,
            float level,
            float isSurface,
            float isTidalSurface) {

        buffer.add((float) Coordinate3Int.unpackX(vertXYZ));
        buffer.add((float) Coordinate3Int.unpackY(vertXYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vertXYZ));
        buffer.add(0f); // u — liquid is untextured
        buffer.add(0f); // v — liquid is untextured
        buffer.add(meta);
        buffer.add(color);
        buffer.add(level); // edge A0 slot: fluid level, 0..LIQUID_LEVEL_MAX
        buffer.add(isSurface); // edge A1 slot: 1 = pull to fluid surface height
        buffer.add(isTidalSurface); // edge B0 slot: 1 = ride the live tide and ocean waves
        buffer.add(0f);
        buffer.add(0f); // high edge slots — solid geometry only
        buffer.add(0f);
        buffer.add(0f);
        buffer.add(0f);
    }
}