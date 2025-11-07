package com.AdventureRPG.WorldManager.Biomes;

import com.AdventureRPG.Core.Root.SystemFrame;
import com.AdventureRPG.WorldManager.WorldManager;

public class BiomeSystem extends SystemFrame {

    // Biome System
    private Biome[] biomes;

    // Base \\

    @Override
    protected void init() {

        // Biome System
        this.biomes = Loader.loadBiomes(rootManager.get(WorldManager.class));
    }

    // Biome System \\

    public Biome getBiomeByID(int id) {
        return biomes[id];
    }
}
