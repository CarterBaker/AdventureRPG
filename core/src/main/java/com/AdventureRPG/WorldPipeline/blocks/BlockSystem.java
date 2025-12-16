package com.AdventureRPG.WorldPipeline.blocks;

import com.AdventureRPG.core.kernel.SystemFrame;
import com.AdventureRPG.core.shaderpipeline.materialmanager.MaterialManager;
import com.AdventureRPG.core.shaderpipeline.texturemanager.TextureManager;
import com.google.gson.Gson;

public class BlockSystem extends SystemFrame {

    // Root
    private Gson gson;

    // Block System
    private Block[] blocks;

    // Base \\

    @Override
    protected void init() {

        // Root
        this.gson = gameEngine.gson;

        // Block System
        TextureManager textureManager = gameEngine.get(TextureManager.class);
        MaterialManager materialManager = gameEngine.get(MaterialManager.class);
        this.blocks = Loader.LoadBlocks(
                gameEngine.gson,
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
