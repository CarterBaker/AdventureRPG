package com.AdventureRPG.WorldSystem.Biomes;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;

public class BiomeSystem {

    // Game Manager
    public final Settings settings;

    // Biome System
    private final Biome[] biomes;

    // Base \\

    public BiomeSystem(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;

        // Biome System
        this.biomes = Loader.loadBiomes(gameManager);
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
