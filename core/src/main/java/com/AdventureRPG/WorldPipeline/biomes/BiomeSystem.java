package com.AdventureRPG.WorldPipeline.biomes;

import com.AdventureRPG.core.engine.SystemPackage;

public class BiomeSystem extends SystemPackage {

    // Biome System
    private Biome[] biomes;

    // Base \\

    @Override
    protected void init() {

        // Biome System
        this.biomes = Loader.loadBiomes(internal.gson);
    }

    // Biome System \\

    public Biome getBiomeByID(int id) {
        return biomes[id];
    }
}
