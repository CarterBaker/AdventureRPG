package com.AdventureRPG.WorldSystem.Biomes;

import com.AdventureRPG.Core.GameSystem;

public class BiomeSystem extends GameSystem {

    // Biome System
    private Biome[] biomes;

    // Base \\

    @Override
    public void init() {

        // Biome System
        this.biomes = Loader.loadBiomes(rootManager.worldSystem);
    }

    public void awake() {

    }

    public void start() {

    }

    public void update() {

    }

    public void render() {

    }

    // Biome System \\

    public Biome getBiomeByID(int id) {
        return biomes[id];
    }
}
