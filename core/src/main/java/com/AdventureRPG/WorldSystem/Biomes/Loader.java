package com.AdventureRPG.WorldSystem.Biomes;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.google.gson.JsonSyntaxException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Loader {
    public static Biome[] LoadBiomes(GameManager gameManager) {

        Settings settings = gameManager.settings;
        File biomeDir = new File(settings.BIOME_PATH);

        if (!biomeDir.exists() || !biomeDir.isDirectory())
            throw new RuntimeException("Biome directory not found: " + settings.BIOME_PATH);

        File[] jsonFiles = biomeDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0)
            throw new RuntimeException("No biome JSON files found in: " + settings.BIOME_PATH);

        List<Biome> biomeList = new ArrayList<>();
        for (File file : jsonFiles) {
            try {
                String content = Files.readString(file.toPath());
                Biome biome = gameManager.gson.fromJson(content, Biome.class);
                biomeList.add(biome);
            } catch (IOException | JsonSyntaxException e) {
                throw new RuntimeException("Failed to load or parse biome: " + file.getName(), e);
            }
        }

        return biomeList.toArray(new Biome[0]);
    }
}
