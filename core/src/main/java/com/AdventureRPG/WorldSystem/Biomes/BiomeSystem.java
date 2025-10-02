package com.AdventureRPG.WorldSystem.Biomes;

import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.WorldSystem.WorldSystem;

public class BiomeSystem {

    // Game Manager
    public final Settings settings;

    // Biome System
    private final Biome[] biomes;

    // Base \\

    public BiomeSystem(WorldSystem worldSystem) {

        // Game Manager
        this.settings = worldSystem.settings;

        // Biome System
        this.biomes = Loader.loadBiomes(worldSystem);
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
