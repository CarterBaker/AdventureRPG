package com.AdventureRPG.bootstrap.worldpipeline.subchunk;

import com.AdventureRPG.bootstrap.worldpipeline.util.BlockPaletteHandle;
import com.AdventureRPG.core.engine.InstancePackage;

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
