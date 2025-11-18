package com.AdventureRPG.WorldManager.Biomes;

import com.AdventureRPG.Core.Bootstrap.SystemFrame;

public class BiomeSystem extends SystemFrame {

    // Biome System
    private Biome[] biomes;

    // Base \\

    @Override
    protected void init() {

        // Biome System
        this.biomes = Loader.loadBiomes(gameEngine.gson);
    }

    // Biome System \\

    public Biome getBiomeByID(int id) {
        return biomes[id];
    }
}
