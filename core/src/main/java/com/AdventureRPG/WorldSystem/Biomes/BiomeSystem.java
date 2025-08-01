package com.AdventureRPG.WorldSystem.Biomes;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import java.util.HashMap;
import java.util.Map;

public class BiomeSystem {

    // Base
    public final Settings settings;

    // Biome System
    private final Biome[] biomes;

    // Blending
    private final Map<Integer, int[]> relatedBiomes;
    private final Map<Integer, int[]> relatedSubTerrainianBiomes;
    private final Map<Integer, int[]> relatedSurfaceBiomes;

    public BiomeSystem(GameManager gameManager) {

        // Biome System
        this.biomes = Loader.LoadBiomes(gameManager); // This needs to be called as soon as possible

        // Base
        this.settings = gameManager.settings;

        // Blending
        this.relatedBiomes = OrganizeSimilarBiomes(biomes);
        this.relatedSubTerrainianBiomes = OrganizeSubTerrainianBiomes();
        this.relatedSurfaceBiomes = OrganizeSurfaceBiomes();
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
