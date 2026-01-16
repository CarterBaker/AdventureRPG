package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.FullGeometryBranch;

import java.util.BitSet;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.ComplexGeometryBranch;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.LiquidGeometryBranch;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.PartialGeometryBranch;
import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.Extras.Coordinate3Short;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

class InternalBuildManager extends ManagerPackage {

    // Internal
    private FullGeometryBranch fullGeometryBranch;
    private PartialGeometryBranch partialGeometryBranch;
    private ComplexGeometryBranch complexGeometryBranch;
    private LiquidGeometryBranch liquidGeometryBranch;

    private BiomeManager biomeManager;
    private BlockManager blockManager;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.fullGeometryBranch = create(FullGeometryBranch.class);
        this.partialGeometryBranch = create(PartialGeometryBranch.class);
        this.complexGeometryBranch = create(ComplexGeometryBranch.class);
        this.liquidGeometryBranch = create(LiquidGeometryBranch.class);
    }

    @Override
    protected void get() {

        // Internal
        this.biomeManager = get(BiomeManager.class);
        this.blockManager = get(BlockManager.class);
    }

    // Geometry Builder \\

    public boolean build(
            DynamicGeometryAsyncInstance dynamicGeometryAsyncInstance,
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance) {

        BlockPaletteHandle biomePaletteHandle = subChunkInstance.getBiomePaletteHandle();
        BlockPaletteHandle blockPaletteHandle = subChunkInstance.getBlockPaletteHandle();

        FloatArrayList quads = dynamicGeometryAsyncInstance.getQuads();

        BitSet batchedBlocks = dynamicGeometryAsyncInstance.getBatchedBlocks();
        BitSet[] directionalBatches = dynamicGeometryAsyncInstance.getDirectionalBatches();

        for (int i = 0; i < Coordinate3Short.BLOCK_COORDINATE_COUNT; i++) {

            short xyz = Coordinate3Short.getBlockCoordinate(i);

            short biomeID = biomePaletteHandle.getBlock(xyz);
            BiomeHandle biomeHandle = biomeManager.getBiomeFromBiomeID(biomeID);

            short blockID = blockPaletteHandle.getBlock(xyz);
            BlockHandle blockHandle = blockManager.getBlockFromBlockID(blockID);

            for (int direction = 0; direction < Direction3Vector.LENGTH; direction++) {

                if (directionalBatches[direction].get(xyz))
                    continue;

                FloatArrayList blockQauds = assembleQuads(
                        blockHandle.getGeometry(),
                        chunkInstance,
                        subChunkInstance,
                        biomeHandle,
                        blockHandle,
                        xyz,
                        batchedBlocks);

                if (blockQauds == null)
                    continue;

                directionalBatches[direction].or(batchedBlocks);
                batchedBlocks.clear();
            }
        }

        return false; // TODO: Temporary
    }

    private FloatArrayList assembleQuads(
            DynamicGeometry geometry,
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            short xyz,
            BitSet batchedBlocks) {

        return switch (geometry) {

            case NONE -> null;

            case FULL -> fullGeometryBranch.assembleQuads(
                    chunkInstance,
                    subChunkInstance,
                    biomeHandle,
                    blockHandle,
                    xyz,
                    batchedBlocks);

            case PARTIAL -> partialGeometryBranch.assembleQuads(
                    chunkInstance,
                    subChunkInstance,
                    biomeHandle,
                    blockHandle,
                    xyz,
                    batchedBlocks);

            case COMPLEX -> complexGeometryBranch.assembleQuads(
                    chunkInstance,
                    subChunkInstance,
                    biomeHandle,
                    blockHandle,
                    xyz,
                    batchedBlocks);

            case LIQUID -> liquidGeometryBranch.assembleQuads(
                    chunkInstance,
                    subChunkInstance,
                    biomeHandle,
                    blockHandle,
                    xyz,
                    batchedBlocks);
        };
    }
}
