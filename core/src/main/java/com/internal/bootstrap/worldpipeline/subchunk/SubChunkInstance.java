package com.internal.bootstrap.worldpipeline.subchunk;

import com.internal.bootstrap.geometrypipeline.vaomanager.VAOHandle;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderInstance;
import com.internal.bootstrap.worldpipeline.worldrendersystem.WorldRenderSystem;
import com.internal.bootstrap.worldpipeline.worldstreammanager.WorldHandle;
import com.internal.core.engine.settings.EngineSetting;

public class SubChunkInstance extends WorldRenderInstance {

    // Internal
    byte subChunkCoordinate;

    BlockPaletteHandle biomePaletteHandle;
    BlockPaletteHandle blockPaletteHandle;

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

        super.create();
    }

    public void constructor(
            WorldRenderSystem worldRenderSystem,
            WorldHandle worldHandle,
            long coordinate,
            byte subChunkCoordinate,
            VAOHandle vaoHandle) {

        super.constructor(
                worldRenderSystem,
                worldHandle,
                coordinate,
                vaoHandle);

        // Internal
        this.subChunkCoordinate = subChunkCoordinate;
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
}
