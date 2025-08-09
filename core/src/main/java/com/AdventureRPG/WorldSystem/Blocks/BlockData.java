package com.AdventureRPG.WorldSystem.Blocks;

import com.AdventureRPG.WorldSystem.WorldSystem;
import com.AdventureRPG.WorldSystem.Biomes.Biome;

public class BlockData {

    // Game Manager
    private final WorldSystem worldSystem;

    // Block
    public int blockID;
    private Block block;

    // Biome
    public final int biomeID;
    private final Biome biome;

    // Base \\

    // New BlockData
    public BlockData(WorldSystem worldSystem, Biome biome) {

        // Game Manager
        this.worldSystem = worldSystem;
        this.block = null;

        // Biome
        this.biomeID = biome.ID;
        this.biome = biome;
    }

    // Load BlockData
    public BlockData(WorldSystem worldSystem, int biomeID, int blockID) {

        // Game Manager
        this.worldSystem = worldSystem;

        // Block
        this.blockID = blockID;
        this.block = worldSystem.GetBlockByID(blockID);

        // Biome
        this.biomeID = biomeID;
        this.biome = worldSystem.biomeSystem.GetBiomeByID(biomeID);
    }

    // Block \\

    public void PlaceBlock(int blockID) {

        this.blockID = blockID;
        this.block = worldSystem.GetBlockByID(blockID);
    }

    public void BreakBlock() {

        this.blockID = biome.airBlock;
        this.block = worldSystem.GetBlockByID(biome.airBlock);
    }
}
