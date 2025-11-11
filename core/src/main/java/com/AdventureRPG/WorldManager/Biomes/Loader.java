package com.AdventureRPG.WorldManager.Biomes;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Util.GlobalConstant;
import com.AdventureRPG.WorldManager.WorldManager;
import com.google.gson.JsonSyntaxException;

public class Loader {

    public static Biome[] loadBiomes(WorldManager worldManager) {

        File biomeDir = new File(GlobalConstant.BIOME_JSON_PATH);

        if (!biomeDir.exists() || !biomeDir.isDirectory())
            throw new RuntimeException("Biome directory not found: " + GlobalConstant.BIOME_JSON_PATH);

        File[] jsonFiles = biomeDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0)
            throw new RuntimeException("No biome JSON files found in: " + GlobalConstant.BIOME_JSON_PATH);

        List<Biome> biomeList = new ArrayList<>();
        for (File file : jsonFiles) {
            try {
                String content = Files.readString(file.toPath());
                Biome biome = worldManager.gson.fromJson(content, Biome.class);
                biomeList.add(biome);
            } catch (IOException | JsonSyntaxException e) {
                throw new RuntimeException("Failed to load or parse biome: " + file.getName(), e);
            }
        }

        return biomeList.toArray(new Biome[0]);
    }
}
