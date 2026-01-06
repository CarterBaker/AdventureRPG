package com.AdventureRPG.WorldPipeline.blocks;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaderpipeline.texturemanager.TextureManager;

public class BlockManager extends ManagerPackage {

    // Block System
    private Block[] blocks;

    // Base \\

    @Override
    protected void get() {

        // Block System
        TextureManager textureManager = get(TextureManager.class);
        MaterialManager materialManager = get(MaterialManager.class);
        this.blocks = Loader.LoadBlocks(
                internal.gson,
                textureManager,
                materialManager);
    }

    // Accessible \\

    public Block getBlockByID(int id) {
        return (id >= 0 && id < blocks.length) ? blocks[id] : null;
    }

    public Block getBlockByName(String name) {

        for (Block block : blocks)
            if (block != null && block.name.equalsIgnoreCase(name))
                return block;

        // TODO: Need to unify error to custom system
        throw new RuntimeException("Block not found: " + name);
    }

    public Type getBlockTypeByID(int id) {
        return (id >= 0 && id < blocks.length) ? blocks[id].type : null;
    }
}
