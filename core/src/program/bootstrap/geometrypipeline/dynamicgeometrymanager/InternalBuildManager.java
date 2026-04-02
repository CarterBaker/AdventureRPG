package program.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;
import program.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import program.bootstrap.geometrypipeline.dynamicmodel.DynamicModelHandle;
import program.bootstrap.geometrypipeline.dynamicpacket.DynamicPacketInstance;
import program.bootstrap.menupipeline.fonts.GlyphMetricStruct;
import program.bootstrap.worldpipeline.biome.BiomeHandle;
import program.bootstrap.worldpipeline.biomemanager.BiomeManager;
import program.bootstrap.worldpipeline.block.BlockHandle;
import program.bootstrap.worldpipeline.block.BlockPaletteHandle;
import program.bootstrap.worldpipeline.blockmanager.BlockManager;
import program.bootstrap.worldpipeline.chunk.ChunkInstance;
import program.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import program.bootstrap.worldpipeline.util.ChunkCoordinate3Int;
import program.core.engine.ManagerPackage;
import program.core.util.mathematics.extrasa.Color;
import program.core.util.mathematics.extrasa.Direction3Vector;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class InternalBuildManager extends ManagerPackage {

    /*
     * Routes per-block geometry assembly to the correct branch based on block
     * geometry type. Drives the full subchunk build loop and delegates font
     * glyph assembly to FontGeometryBranch.
     */

    // Internal
    private FullGeometryBranch fullGeometryBranch;
    private PartialGeometryBranch partialGeometryBranch;
    private ComplexGeometryBranch complexGeometryBranch;
    private LiquidGeometryBranch liquidGeometryBranch;
    private FontGeometryBranch fontGeometryBranch;
    private BiomeManager biomeManager;
    private BlockManager blockManager;

    // Settings
    private int BLOCK_COORDINATE_COUNT;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.fullGeometryBranch = create(FullGeometryBranch.class);
        this.partialGeometryBranch = create(PartialGeometryBranch.class);
        this.complexGeometryBranch = create(ComplexGeometryBranch.class);
        this.liquidGeometryBranch = create(LiquidGeometryBranch.class);
        this.fontGeometryBranch = create(FontGeometryBranch.class);

        // Settings
        this.BLOCK_COORDINATE_COUNT = ChunkCoordinate3Int.BLOCK_COORDINATE_COUNT;
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

        if (!dynamicPacketInstance.tryLock())
            return false;

        dynamicPacketInstance.clear();
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

    // Font Geometry \\

    /*
     * Caller creates and owns the DynamicModelHandle — branch just fills verts.
     */
    void buildGlyphModel(
            DynamicModelHandle model,
            GlyphMetricStruct glyph,
            int atlasPixelSize) {
        fontGeometryBranch.buildGlyphModel(model, glyph, atlasPixelSize);
    }
}