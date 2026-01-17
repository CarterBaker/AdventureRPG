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
            DynamicGeometryAsyncContainer dynamicGeometryAsyncContainer,
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance) {

        BlockPaletteHandle biomePaletteHandle = subChunkInstance.getBiomePaletteHandle();
        BlockPaletteHandle blockPaletteHandle = subChunkInstance.getBlockPaletteHandle();

        FloatArrayList quads = dynamicGeometryAsyncContainer.getQuads();
        BitSet[] directionalBatches = dynamicGeometryAsyncContainer.getDirectionalBatches();
        BitSet batchReturn = dynamicGeometryAsyncContainer.getBatchReturn();

        for (int i = 0; i < Coordinate3Short.BLOCK_COORDINATE_COUNT; i++) {

            short xyz = Coordinate3Short.getBlockCoordinate(i);

            short biomeID = biomePaletteHandle.getBlock(xyz);
            BiomeHandle biomeHandle = biomeManager.getBiomeFromBiomeID(biomeID);

            short blockID = blockPaletteHandle.getBlock(xyz);
            BlockHandle blockHandle = blockManager.getBlockFromBlockID(blockID);

            for (int direction = 0; direction < Direction3Vector.LENGTH; direction++) {

                if (directionalBatches[direction].get(xyz))
                    continue;

                if (!assembleQuads(
                        blockHandle.getGeometry(),
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
                        xyz,
                        Direction3Vector.VALUES[direction],
                        biomeHandle,
                        blockHandle,
                        quads,
                        batchReturn))
                    continue;

                directionalBatches[direction].or(batchReturn);
                batchReturn.clear();
            }
        }

        return false; // TODO: Temporary
    }

    private Boolean assembleQuads(
            DynamicGeometry geometry,
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            short xyz,
            Direction3Vector direction3Vector,
            BiomeHandle biomeHandle,
            BlockHandle blockHandle,
            FloatArrayList quads,
            BitSet batchReturn) {

        return switch (geometry) {

            case NONE -> null;

            case FULL -> fullGeometryBranch.assembleQuads(
                    chunkInstance,
                    subChunkInstance,
                    biomePaletteHandle,
                    blockPaletteHandle,
                    xyz,
                    direction3Vector,
                    biomeHandle,
                    blockHandle,
                    quads,
                    batchReturn);

            case PARTIAL -> partialGeometryBranch.assembleQuads(
                    chunkInstance,
                    subChunkInstance,
                    biomePaletteHandle,
                    blockPaletteHandle,
                    xyz,
                    direction3Vector,
                    biomeHandle,
                    blockHandle,
                    quads,
                    batchReturn);

            case COMPLEX -> complexGeometryBranch.assembleQuads(
                    chunkInstance,
                    subChunkInstance,
                    biomePaletteHandle,
                    blockPaletteHandle,
                    xyz,
                    direction3Vector,
                    biomeHandle,
                    blockHandle,
                    quads,
                    batchReturn);

            case LIQUID -> liquidGeometryBranch.assembleQuads(
                    chunkInstance,
                    subChunkInstance,
                    biomePaletteHandle,
                    blockPaletteHandle,
                    xyz,
                    direction3Vector,
                    biomeHandle,
                    blockHandle,
                    quads,
                    batchReturn);
        };
    }

    // Offsett all positive directions
    // short vertXYZ = Coordinate3Short.convertToVertSpace(xyz, direction3Vector);
    // TODO: Accidentally skipped ahead will need this later.
}
