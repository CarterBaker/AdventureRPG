package com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager;

import java.util.BitSet;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.ComplexGeometryBranch;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.FullGeometryBranch;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.LiquidGeometryBranch;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.dynamicgeometry.PartialGeometryBranch;
import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.util.DynamicGeometryAsyncContainer;
import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.bootstrap.worldpipeline.biomemanager.BiomeManager;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.bootstrap.worldpipeline.block.BlockPaletteHandle;
import com.internal.bootstrap.worldpipeline.blockmanager.BlockManager;
import com.internal.bootstrap.worldpipeline.chunk.ChunkInstance;
import com.internal.bootstrap.worldpipeline.subchunk.SubChunkInstance;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.mathematics.Extras.Color;
import com.internal.core.util.mathematics.Extras.Coordinate2Long;
import com.internal.core.util.mathematics.Extras.Coordinate3Short;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class InternalBuildManager extends ManagerPackage {

    // Internal
    private FullGeometryBranch fullGeometryBranch;
    private PartialGeometryBranch partialGeometryBranch;
    private ComplexGeometryBranch complexGeometryBranch;
    private LiquidGeometryBranch liquidGeometryBranch;

    private BiomeManager biomeManager;
    private BlockManager blockManager;

    private int BLOCK_COORDINATE_COUNT;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.fullGeometryBranch = create(FullGeometryBranch.class);
        this.partialGeometryBranch = create(PartialGeometryBranch.class);
        this.complexGeometryBranch = create(ComplexGeometryBranch.class);
        this.liquidGeometryBranch = create(LiquidGeometryBranch.class);

        this.BLOCK_COORDINATE_COUNT = Coordinate3Short.BLOCK_COORDINATE_COUNT;
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

        DynamicPacketInstance dynamicPacketInstance = subChunkInstance.getDynamicPacketInstance();

        if (!dynamicPacketInstance.tryLock())
            return false;

        dynamicPacketInstance.clear();
        dynamicGeometryAsyncContainer.reset();

        BlockPaletteHandle biomePaletteHandle = subChunkInstance.getBiomePaletteHandle();
        BlockPaletteHandle blockPaletteHandle = subChunkInstance.getBlockPaletteHandle();

        Int2ObjectOpenHashMap<FloatArrayList> verts = dynamicGeometryAsyncContainer.getVerts();
        BitSet[] directionalBatches = dynamicGeometryAsyncContainer.getDirectionalBatches();
        BitSet batchReturn = dynamicGeometryAsyncContainer.getBatchReturn();

        Color[] vertColors = dynamicGeometryAsyncContainer.getVertColors();

        for (int i = 0; i < BLOCK_COORDINATE_COUNT; i++) {

            short xyz = Coordinate3Short.getBlockCoordinate(i);

            short biomeID = biomePaletteHandle.getBlock(xyz);
            BiomeHandle biomeHandle = biomeManager.getBiomeFromBiomeID(biomeID);

            short blockID = blockPaletteHandle.getBlock(xyz);
            BlockHandle blockHandle = blockManager.getBlockFromBlockID(blockID);

            for (int direction = 0; direction < Direction3Vector.LENGTH; direction++) {

                batchReturn.clear();

                BitSet accumulatedBatch = directionalBatches[direction];

                if (accumulatedBatch.get(xyz))
                    continue;

                DynamicGeometryType blockGeometry = blockHandle.getGeometry();
                if (blockGeometry == DynamicGeometryType.NONE)
                    continue;

                if (!assembleQuads(
                        blockGeometry,
                        chunkInstance,
                        subChunkInstance,
                        biomePaletteHandle,
                        blockPaletteHandle,
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

                accumulatedBatch.or(batchReturn);
            }
        }

        for (int materialID : verts.keySet())
            dynamicPacketInstance.addVertices(materialID, verts.get(materialID));

        if (dynamicPacketInstance.getTotalVertexCount() > 0)
            dynamicPacketInstance.setReady();
        else
            dynamicPacketInstance.unlock();

        return true;
    }

    private boolean assembleQuads(
            DynamicGeometryType geometry,
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance,
            BlockPaletteHandle biomePaletteHandle,
            BlockPaletteHandle blockPaletteHandle,
            DynamicPacketInstance dynamicPacketInstance,
            short xyz,
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
                    dynamicPacketInstance,
                    xyz,
                    direction3Vector,
                    biomeHandle,
                    blockHandle,
                    verts,
                    accumulatedBatch,
                    batchReturn,
                    vertColors);

            case NONE -> true; // Not reachable
        };
    }

    // TODO: Remove

    private void debugBuilder(
            String message,
            ChunkInstance chunkInstance,
            SubChunkInstance subChunkInstance) {

        long chunkCoord = chunkInstance.getCoordinate();
        if (chunkCoord != Coordinate2Long.pack(0, 0))
            return;

        if (subChunkInstance.getCoordinate() != 0)
            return;

        System.out.println(message);
    }
}
