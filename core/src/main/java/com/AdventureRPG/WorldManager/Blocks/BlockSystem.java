package com.AdventureRPG.WorldManager.Blocks;

import com.AdventureRPG.Core.Root.SystemFrame;
import com.AdventureRPG.WorldManager.WorldManager;
import com.google.gson.Gson;

public class BlockSystem extends SystemFrame {

    // Root
    private Gson gson;
    private WorldManager worldManager;

    // Block System
    private Block[] blocks;

    // Base \\

    @Override
    protected void init() {

        // Root
        this.gson = rootManager.gson;
        this.worldManager = rootManager.get(WorldManager.class);

        // Block System
        this.blocks = Loader.LoadBlocks(rootManager.gson, worldManager);
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
