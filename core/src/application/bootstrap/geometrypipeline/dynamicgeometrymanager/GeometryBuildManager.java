package application.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import application.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import application.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import application.bootstrap.worldpipeline.biome.BiomeHandle;
import application.bootstrap.worldpipeline.biomemanager.BiomeManager;
import application.bootstrap.worldpipeline.block.BlockHandle;
import application.bootstrap.worldpipeline.block.BlockPaletteHandle;
import application.bootstrap.worldpipeline.blockmanager.BlockManager;
import application.bootstrap.worldpipeline.chunk.ChunkInstance;
import application.bootstrap.worldpipeline.chunk.ChunkNeighborHandle;
import application.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import application.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import engine.graphics.color.Color;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.mathematics.extras.Direction3Vector;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class GeometryBuildManager extends ManagerPackage {

    /*
     * Routes per-block geometry assembly to the correct branch based on block
     * geometry type. Drives the full subchunk build loop, tallies which
     * DynamicGeometryTypes the subchunk actually contains (see
     * SubChunkInstance), and delegates font glyph assembly to
     * FontGeometryBranch. A subchunk proven empty by WorldGenerationManager
     * skips the block walk entirely, since air never contributes a face —
     * in a tall world this is the majority of subchunks in any column. A
     * subchunk proven uniformly filled (deep stone, deep water) additionally
     * skips the walk whenever every one of its 6 neighbors is provably the
     * same geometry, since two identical-geometry neighbors never expose a
     * face between them — this is what keeps a mountain's buried interior
     * from paying the per-block cost. A subchunk proven opaque interior —
     * every cell FULL geometry, zero air or liquid, but not necessarily one
     * block ID — gets the same skip whenever its neighbors are equally
     * solid, since a stone/ore mix or a buried dirt/stone transition band
     * never exposes a face either; this is what keeps buried, non-uniform
     * terrain (the common case under any real surface) from paying the
     * per-block cost just because it isn't a single block ID. build() is
     * only ever called by a caller already holding the owning chunk's own
     * sync lock (BuildBranch, LiquidTickBranch, DebugWaterPlacementSystem),
     * so the packet's GENERATING/READY status is never used as an entry
     * gate here — it's set purely so anything downstream can observe it.
     */

    private static final Direction3Vector[] LATERAL_DIRECTIONS = {
            Direction3Vector.NORTH, Direction3Vector.EAST, Direction3Vector.SOUTH, Direction3Vector.WEST
    };

    // Internal
    private FullGeometryBranch fullGeometryBranch;
    private PartialGeometryBranch partialGeometryBranch;
    private ComplexGeometryBranch complexGeometryBranch;
    private LiquidGeometryBranch liquidGeometryBranch;
    private BiomeManager biomeManager;
    private BlockManager blockManager;

    // Settings
    private int BLOCK_COORDINATE_COUNT;
    private int worldHeight;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.fullGeometryBranch = create(FullGeometryBranch.class);
        this.partialGeometryBranch = create(PartialGeometryBranch.class);
        this.complexGeometryBranch = create(ComplexGeometryBranch.class);
        this.liquidGeometryBranch = create(LiquidGeometryBranch.class);

        // Settings
        this.BLOCK_COORDINATE_COUNT = ChunkCoordinate3Int.BLOCK_COORDINATE_COUNT;
        this.worldHeight = EngineSetting.WORLD_HEIGHT;
    }

    @Override
    protected void get() {

        // Internal
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Chunk Geometry \\

    boolean build(
            DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer,
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance) {

        DynamicPacketInstance dynamicPacketInstance = subChunkInstance.getDynamicPacketInstance();

        dynamicPacketInstance.beginGenerating();
        dynamicPacketInstance.clearModels();
        subChunkInstance.beginBlockTypeTally();

        if (subChunkInstance.isKnownEmpty()) {
            subChunkInstance.finalizeBlockTypeTally();
            dynamicPacketInstance.unlock();
            return true;
        }

        if (subChunkInstance.isUniformFill() && isFullyEnclosed(chunkInstance, subChunkInstance)) {
            tallyUniformFill(subChunkInstance);
            subChunkInstance.finalizeBlockTypeTally();
            dynamicPacketInstance.unlock();
            return true;
        }

        if (subChunkInstance.isOpaqueInterior() && isFullyEnclosedOpaque(chunkInstance, subChunkInstance)) {
            tallyOpaqueInterior(subChunkInstance);
            subChunkInstance.finalizeBlockTypeTally();
            dynamicPacketInstance.unlock();
            return true;
        }

        dynamicGeometryAsyncContainer.reset();

        BlockPaletteHandle biomePaletteHandle = subChunkInstance.getBiomePaletteHandle();
        BlockPaletteHandle blockPaletteHandle = subChunkInstance.getBlockPaletteHandle();
        BlockPaletteHandle rotationPaletteHandle = subChunkInstance.getBlockRotationPaletteHandle();
        Int2ObjectOpenHashMap<FloatArrayList> verts = dynamicGeometryAsyncContainer.getVerts();
        BitSet[] directionalBatches = dynamicGeometryAsyncContainer.getDirectionalBatches();
        BitSet batchReturn = dynamicGeometryAsyncContainer.getBatchReturn();
        Color[] vertColors = dynamicGeometryAsyncContainer.getVertColors();

        for (int i = 0; i < BLOCK_COORDINATE_COUNT; i++) {

            int xyz = ChunkCoordinate3Int.getBlockCoordinate(i);
            short biomeID = biomePaletteHandle.getBlock(xyz);
            BiomeHandle biomeHandle = biomeManager.getBiomeHandleFromBiomeID(biomeID);
            short blockID = blockPaletteHandle.getBlock(xyz);
            BlockHandle blockHandle = blockManager.getBlockHandleFromBlockID(blockID);
            DynamicGeometryType blockGeometry = blockHandle.getGeometry();

            if (blockGeometry == DynamicGeometryType.NONE)
                continue;

            subChunkInstance.tallyBlockType(blockGeometry);

            if (blockGeometry == DynamicGeometryType.LIQUID)
                subChunkInstance.tallyLiquidBlock(blockID);

            for (int direction = 0; direction < Direction3Vector.LENGTH; direction++) {

                batchReturn.clear();
                BitSet accumulatedBatch = directionalBatches[direction];

                if (accumulatedBatch.get(i))
                    continue;

                if (!assembleQuads(
                        blockGeometry,
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
                        rotationPaletteHandle,
                        dynamicPacketInstance,
                        xyz,
                        Direction3Vector.VALUES[direction],
                        biomeHandle,
                        blockHandle,
                        verts,
                        accumulatedBatch,
                        batchReturn,
                        vertColors))
                    continue;
            }
        }

        subChunkInstance.finalizeBlockTypeTally();

        boolean success = true;

        for (int materialID : verts.keySet()) {
            if (!dynamicPacketInstance.addVertices(materialID, verts.get(materialID)))
                success = false;
        }

        if (dynamicPacketInstance.hasModels())
            dynamicPacketInstance.setReady();
        else
            dynamicPacketInstance.unlock();

        return success;
    }

    private boolean assembleQuads(
            DynamicGeometryType geometry,
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

        return switch (geometry) {
            case FULL -> fullGeometryBranch.assembleQuads(
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
            case PARTIAL -> partialGeometryBranch.assembleQuads(
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
            case COMPLEX -> complexGeometryBranch.assembleQuads(
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
            case LIQUID -> liquidGeometryBranch.assembleQuads(
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
            case NONE -> true;
        };
    }

    // Uniform Enclosure Fast Path \\

    private boolean isFullyEnclosed(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance) {

        int subY = (int) subChunkInstance.getCoordinate();

        if (subY == 0 || subY == worldHeight - 1)
            return false;

        DynamicGeometryType type = subChunkInstance.getUniformGeometryType();
        short blockID = subChunkInstance.getUniformBlockID();

        if (!matchesUniform(chunkInstance.getSubChunk(subY - 1), type, blockID))
            return false;

        if (!matchesUniform(chunkInstance.getSubChunk(subY + 1), type, blockID))
            return false;

        ChunkNeighborHandle neighbors = chunkInstance.getChunkNeighbors();

        for (Direction3Vector direction : LATERAL_DIRECTIONS) {

            ChunkInstance neighborChunk = neighbors.getNeighborChunk(direction.to2D().index);

            if (neighborChunk == null)
                return false;

            if (!matchesUniform(neighborChunk.getSubChunk(subY), type, blockID))
                return false;
        }

        return true;
    }

    private boolean matchesUniform(SubChunkInstance other, DynamicGeometryType type, short blockID) {

        if (!other.isUniformFill() || other.getUniformGeometryType() != type)
            return false;

        return type != DynamicGeometryType.LIQUID || other.getUniformBlockID() == blockID;
    }

    private void tallyUniformFill(SubChunkInstance subChunkInstance) {

        DynamicGeometryType type = subChunkInstance.getUniformGeometryType();

        subChunkInstance.tallyBlockType(type, BLOCK_COORDINATE_COUNT);

        if (type == DynamicGeometryType.LIQUID)
            subChunkInstance.tallyLiquidBlock(subChunkInstance.getUniformBlockID());
    }

    // Opaque Interior Fast Path \\

    private boolean isFullyEnclosedOpaque(ChunkInstance chunkInstance, SubChunkInstance subChunkInstance) {

        int subY = (int) subChunkInstance.getCoordinate();

        if (subY == 0 || subY == worldHeight - 1)
            return false;

        if (!isNeighborFullySolid(chunkInstance.getSubChunk(subY - 1)))
            return false;

        if (!isNeighborFullySolid(chunkInstance.getSubChunk(subY + 1)))
            return false;

        ChunkNeighborHandle neighbors = chunkInstance.getChunkNeighbors();

        for (Direction3Vector direction : LATERAL_DIRECTIONS) {

            ChunkInstance neighborChunk = neighbors.getNeighborChunk(direction.to2D().index);

            if (neighborChunk == null)
                return false;

            if (!isNeighborFullySolid(neighborChunk.getSubChunk(subY)))
                return false;
        }

        return true;
    }

    private boolean isNeighborFullySolid(SubChunkInstance other) {

        if (other.isOpaqueInterior())
            return true;

        return other.isUniformFill() && other.getUniformGeometryType() == DynamicGeometryType.FULL;
    }

    private void tallyOpaqueInterior(SubChunkInstance subChunkInstance) {
        subChunkInstance.tallyBlockType(DynamicGeometryType.FULL, BLOCK_COORDINATE_COUNT);
    }
}