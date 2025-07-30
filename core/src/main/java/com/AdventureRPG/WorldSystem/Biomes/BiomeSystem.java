package com.AdventureRPG.WorldSystem.Biomes;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class BiomeSystem {

    // Game
    public final Settings settings;

    // Biome System
    private final Biome[] biomes;

    // Blending
    private final Map<Integer, int[]> relatedBiomes;
    private final Map<Integer, int[]> relatedSubTerrainianBiomes;
    private final Map<Integer, int[]> relatedSurfaceBiomes;

    public BiomeSystem(GameManager gameManager) {

        // Game
        this.settings = gameManager.settings;

        // Biome System
        this.biomes = loadBiomes(gameManager);

        // Blending
        this.relatedBiomes = OrganizeSimilarBiomes(biomes);
        this.relatedSubTerrainianBiomes = OrganizeSubTerrainianBiomes();
        this.relatedSurfaceBiomes = OrganizeSurfaceBiomes();
    }

    private Biome[] loadBiomes(GameManager gameManager) {

        File biomeDir = new File(settings.BIOME_PATH);
        if (!biomeDir.exists() || !biomeDir.isDirectory()) {
            throw new RuntimeException("Biome directory not found: " + settings.BIOME_PATH);
        }

        File[] jsonFiles = biomeDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null) {
            throw new RuntimeException("Failed to list files in: " + settings.BIOME_PATH);
        }

        if (jsonFiles.length == 0) {
            throw new RuntimeException("No biome JSON files found in: " + settings.BIOME_PATH);
        }

        Biome[] loadedBiomes = new Biome[jsonFiles.length];
        for (int i = 0; i < jsonFiles.length; i++) {
            File file = jsonFiles[i];
            try {
                String content = Files.readString(file.toPath());
                loadedBiomes[i] = gameManager.gson.fromJson(content, Biome.class);
            } catch (IOException | JsonSyntaxException e) {
                throw new RuntimeException("Failed to load or parse biome: " + file.getName(), e);
            }
        }

        return loadedBiomes;
    }

    private Map<Integer, int[]> OrganizeSimilarBiomes(Biome[] biomes) {

        Map<Integer, int[]> map = new HashMap<>();

        for (Biome biome : biomes) {

            int[] sortedIDs = biome.similarBiomes.stream().mapToInt(Integer::intValue).sorted().toArray();

            map.put(biome.ID, sortedIDs);
        }

        return map;
    }

    private Map<Integer, int[]> OrganizeSubTerrainianBiomes() {
        Map<Integer, int[]> map = new HashMap<>();

        for (Map.Entry<Integer, int[]> entry : relatedBiomes.entrySet()) {
            int biomeID = entry.getKey();
            int[] related = entry.getValue();

            int[] filtered = java.util.Arrays.stream(related)
                    .filter(id -> biomes[id].subTerrainian)
                    .toArray();

            map.put(biomeID, filtered);
        }

        return map;
    }

    private Map<Integer, int[]> OrganizeSurfaceBiomes() {
        Map<Integer, int[]> map = new HashMap<>();

        for (Map.Entry<Integer, int[]> entry : relatedBiomes.entrySet()) {
            int biomeID = entry.getKey();
            int[] related = entry.getValue();

            int[] filtered = java.util.Arrays.stream(related)
                    .filter(id -> !biomes[id].subTerrainian)
                    .toArray();

            map.put(biomeID, filtered);
        }

        return map;
    }

    public Biome GetBiomeByID(int ID) {
        return biomes[ID];
    }

    public int[] getRelatedBiomes(int ID) {
        return relatedBiomes.get(ID);
    }

    public int[] getRelatedSubTerrainianBiomes(int ID) {
        return relatedSubTerrainianBiomes.get(ID);
    }

    public int[] getRelatedSurfaceBiomes(int ID) {
        return relatedSurfaceBiomes.get(ID);
    }

}
