package com.AdventureRPG.WorldManager.Blocks;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;
import com.AdventureRPG.Core.RenderPipeline.MaterialSystem.MaterialSystem;
import com.AdventureRPG.Core.RenderPipeline.TextureSystem.TextureSystem;
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
        this.gson = engineManager.gson;

        // Block System
        TextureSystem textureSystem = engineManager.get(TextureSystem.class);
        MaterialSystem MaterialSystem = engineManager.get(MaterialSystem.class);
        this.blocks = Loader.LoadBlocks(
                engineManager.gson,
                textureSystem,
                MaterialSystem);
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
