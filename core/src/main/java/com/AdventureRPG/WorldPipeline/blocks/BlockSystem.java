package com.AdventureRPG.WorldPipeline.blocks;

import com.AdventureRPG.core.engine.SystemPackage;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaderpipeline.texturemanager.TextureManager;
import com.google.gson.Gson;

public class BlockSystem extends SystemPackage {

    // Root
    private Gson gson;

    // Block System
    private Block[] blocks;

    // Base \\

    @Override
    protected void init() {

        // Root
        this.gson = internal.gson;

        // Block System
        TextureManager textureManager = internal.get(TextureManager.class);
        MaterialManager materialManager = internal.get(MaterialManager.class);
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
