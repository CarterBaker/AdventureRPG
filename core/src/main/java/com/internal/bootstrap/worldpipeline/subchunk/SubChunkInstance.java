package com.internal.bootstrap.worldpipeline.subchunk;

import com.internal.bootstrap.worldpipeline.util.BlockPaletteHandle;
import com.internal.core.engine.InstancePackage;

public class SubChunkInstance extends InstancePackage {

    // Internal
    BlockPaletteHandle blocks;

    // Internal \\

    @Override
    protected void create() {

        // Internal
        this.blocks = create(BlockPaletteHandle.class);
    }

    // Accessible \\

    public BlockPaletteHandle getBlocks() {
        return blocks;
    }
}
