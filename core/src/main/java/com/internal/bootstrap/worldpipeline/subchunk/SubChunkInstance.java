package com.internal.bootstrap.worldpipeline.subchunk;

import com.internal.bootstrap.geometrypipeline.dynamicgeometrymanager.DynamicPacketInstance;
import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;

public class SubChunkInstance extends InstancePackage {

    // Internal
    byte subChunkCoordinate;

    BlockPaletteHandle biomePaletteHandle;
    BlockPaletteHandle blockPaletteHandle;

    // Dynamic Mesh
    private DynamicPacketInstance dynamicPacketInstance;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.biomePaletteHandle = create(BlockPaletteHandle.class);
        this.biomePaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE / EngineSetting.BIOME_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD / EngineSetting.BIOME_SIZE);

        this.blockPaletteHandle = create(BlockPaletteHandle.class);
        this.blockPaletteHandle.constructor(
                EngineSetting.CHUNK_SIZE,
                EngineSetting.BLOCK_PALETTE_THRESHOLD);

        // Dynamic Mesh
        this.dynamicPacketInstance = create(DynamicPacketInstance.class);

    }

    public void constructor(
            byte subChunkCoordinate,
            VAOHandle vaoHandle) {

        // Internal
        this.subChunkCoordinate = subChunkCoordinate;

        // Dynamic Mesh
        this.dynamicPacketInstance.constructor(vaoHandle);
    }

    // Accessible \\

    // Internal
    public byte getSubChunkCoordinate() {
        return subChunkCoordinate;
    }

    public BlockPaletteHandle getBiomePaletteHandle() {
        return biomePaletteHandle;
    }

    public BlockPaletteHandle getBlockPaletteHandle() {
        return blockPaletteHandle;
    }

    // Dynamic Mesh
    public DynamicPacketInstance getDynamicModelInstance() {
        return dynamicPacketInstance;
    }
}
