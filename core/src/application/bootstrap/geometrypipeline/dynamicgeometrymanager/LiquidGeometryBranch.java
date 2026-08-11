package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
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
     * Geometry branch for liquid blocks. Settled water (isLiquidStable() true)
     * only ever assembles its UP face — the one face a still body can actually
     * show a camera, since every other side is by design either basin wall or
     * more of the same settled liquid — while a subchunk mid-flow still emits
     * one unit quad per exposed face every rebuild, since its levels change
     * tick to tick and merging that work would be thrown away almost
     * immediately. hasExposedFace() also now occludes correctly against solid
     * neighbors: a liquid face touching an opaque FULL block can never be seen
     * from any camera angle and is dropped, exactly like two touching solids
     * already drop their shared face in FullGeometryBranch — previously this
     * only compared raw block IDs, so every water block against stone/dirt/etc.
     * wastefully emitted a hidden quad. Once a subchunk settles, its UP faces
     * greedily expand across matching biome, block, and fill level exactly
     * like FullGeometryBranch, so a still lake collapses to a handful of quads
     * instead of one per block. Fill level and "does this vertex sit at the
     * fluid surface" are always written per vertex regardless of stability,
     * packed into the two vertex fields FullGeometryBranch reserves for bevel
     * masks — liquid never bevels, so those floats are otherwise idle here —
     * letting WaterShader pull each vertex down toward the true fluid surface
     * with no VAO change anywhere in the pipeline.
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
            Color[] vertColors) {

        // Settled water is assumed fully contained by basin walls or other
        // settled water on every side but its top. Gating here — before any
        // exposure check or greedy-merge walk runs — removes the single
        // largest source of wasted liquid geometry: a still lake used to pay
        // for invisible wall and floor quads on every one of its blocks.
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

        short level = subChunkInstance.getLiquidLevelPaletteHandle().getBlock(xyz);

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
                            level, biomeHandle, blockHandle, accumulatedBatch, batchReturn)) {
                        accumulatedBatch.or(batchReturn);
                        sizeA++;
                    } else
                        checkA = false;
                }

                if (checkB) {
                    if (tryExpand(
                            chunkInstance, subChunkInstance, biomePaletteHandle, blockPaletteHandle,
                            xyz, direction3Vector, tangentB, tangentA, sizeB, sizeA,
                            level, biomeHandle, blockHandle, accumulatedBatch, batchReturn)) {
                        accumulatedBatch.or(batchReturn);
                        sizeB++;
                    } else
                        checkB = false;
                }
            } while (checkA || checkB);
        }

        finalizeFace(verts, xyz, sizeA, sizeB, direction3Vector, biomeHandle, blockHandle, level);
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
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
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
            short comparativeLevel = subChunkInstance.getLiquidLevelPaletteHandle().getBlock(checkXYZ);

            if (comparativeBlockID != blockHandle.getBlockID() ||
                    comparativeBiomeHandle != biomeHandle ||
                    comparativeLevel != level ||
                    accumulatedBatch.get(ChunkCoordinate3Int.getIndex(checkXYZ)) ||
                    !hasExposedFace(chunkInstance, subChunkInstance, checkXYZ, direction3Vector, blockHandle)) {
                batchReturn.clear();
                return false;
            }

            batchReturn.set(ChunkCoordinate3Int.getIndex(checkXYZ));
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

        int comparativeXYZ = ChunkCoordinate3Int.getNeighborAndWrap(xyz, direction3Vector);
        short comparativeBlockID = comparativeSubChunkInstance.getBlockPaletteHandle().getBlock(comparativeXYZ);

        // Same exact liquid on the other side — always an internal boundary,
        // never a real face, regardless of the two columns' relative fill.
        if (comparativeBlockID == blockHandle.getBlockID())
            return false;

        // Anything else exposes the face UNLESS that neighbor is an opaque
        // full cube — a solid block covers this entire face from every angle
        // a camera could ever reach, so the liquid's own copy of it can never
        // be seen. Air, other liquids, and any non-FULL geometry still expose.
        BlockHandle comparativeBlockHandle = blockManager.getBlockHandleFromBlockID(comparativeBlockID);

        return comparativeBlockHandle.getGeometry() != DynamicGeometryType.FULL;
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
            int xyz,
            byte sizeA,
            byte sizeB,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short level) {

        Direction3Vector tangentA = Direction3Vector.getTangentA(direction3Vector);
        Direction3Vector tangentB = Direction3Vector.getTangentB(direction3Vector);

        int vert0XYZ = ChunkCoordinate3Int.convertToVertSpace(xyz, direction3Vector);
        int vert1XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentA, sizeA);
        int vert2XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert1XYZ, tangentB, sizeB);
        int vert3XYZ = ChunkCoordinate3Int.getVertCoordinateFromOffset(vert0XYZ, tangentB, sizeB);

        // Whichever corners sit at the greatest Y are this face's "surface"
        // vertices — always all 4 for UP, none for DOWN, and the top pair
        // for a side face (or the top pair of a vertically-merged run).
        int y0 = Coordinate3Int.unpackY(vert0XYZ);
        int y1 = Coordinate3Int.unpackY(vert1XYZ);
        int y2 = Coordinate3Int.unpackY(vert2XYZ);
        int y3 = Coordinate3Int.unpackY(vert3XYZ);
        int maxY = Math.max(Math.max(y0, y1), Math.max(y2, y3));

        float nor = (float) direction3Vector.index;
        float color = Color.rgba8888(biomeHandle.getBiomeColor());
        float encodedFace = (float) (direction3Vector.ordinal() * 4);
        float quadSize = (float) ((sizeA & 0xFF) | ((sizeB & 0xFF) << 8));
        float levelF = (float) level;

        FloatArrayList buffer = verts.computeIfAbsent(blockHandle.getMaterialID(), k -> new FloatArrayList());

        writeVertex(buffer, vert0XYZ, nor, color, encodedFace, quadSize, levelF, y0 == maxY ? 1f : 0f);
        writeVertex(buffer, vert1XYZ, nor, color, encodedFace, quadSize, levelF, y1 == maxY ? 1f : 0f);
        writeVertex(buffer, vert2XYZ, nor, color, encodedFace, quadSize, levelF, y2 == maxY ? 1f : 0f);
        writeVertex(buffer, vert3XYZ, nor, color, encodedFace, quadSize, levelF, y3 == maxY ? 1f : 0f);
    }

    private void writeVertex(
            FloatArrayList buffer,
            int vertXYZ,
            float nor,
            float color,
            float encodedFace,
            float quadSize,
            float level,
            float isSurface) {

        buffer.add((float) Coordinate3Int.unpackX(vertXYZ));
        buffer.add((float) Coordinate3Int.unpackY(vertXYZ));
        buffer.add((float) Coordinate3Int.unpackZ(vertXYZ));
        buffer.add(nor);
        buffer.add(color);
        buffer.add(0f); // u — liquid is untextured
        buffer.add(0f); // v — liquid is untextured
        buffer.add(encodedFace);
        buffer.add(quadSize);
        buffer.add(level); // repurposed bevel-mask slot: fluid level, 0..LIQUID_LEVEL_MAX
        buffer.add(isSurface); // repurposed bevel-mask slot: 1 = pull to fluid surface height
        buffer.add(0f);
        buffer.add(0f);
    }
}