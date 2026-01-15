package com.internal.bootstrap.worldpipeline.subchunk;

import com.internal.core.engine.InstancePackage;
import com.internal.core.engine.settings.EngineSetting;

public class SubChunkInstance extends InstancePackage {

    // Internal
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

    }

    // Accessible \\

    public BlockPaletteHandle getBiomePaletteHandle() {
        return biomePaletteHandle;
    }

    public BlockPaletteHandle getBlockPaletteHandle() {
        return blockPaletteHandle;
    }
}
