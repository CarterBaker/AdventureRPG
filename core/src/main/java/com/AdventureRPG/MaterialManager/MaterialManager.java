package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.TextureManager.TextureManager;
import com.google.gson.Gson;

import java.io.File;
import java.util.*;

public class MaterialManager {

    // Game Manager
    private final Settings settings;
    private final GameManager gameManager;
    private final Gson gson;
    private final TextureManager textureManager;

    // Settings
    private final String MATERIAL_JSON_PATH;

    // Registries
    private final Map<String, Integer> nameToID = new HashMap<>();
    private final Map<Integer, MaterialDefinition> idToMaterial = new HashMap<>();
    private int[] textureToMaterial;
    private int nextMaterialID = 0;

    // Base \\
    public MaterialManager(GameManager gameManager) {
        this.settings = gameManager.settings;
        this.gameManager = gameManager;
        this.gson = gameManager.gson;
        this.textureManager = gameManager.TextureManager;

        this.MATERIAL_JSON_PATH = settings.MATERIAL_JSON_PATH;

        assembleAllMaterials();
    }

    private void assembleAllMaterials() {
        File folder = new File(MATERIAL_JSON_PATH);
        File[] files = folder.listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null)
            return;

        for (File file : files) {
            MaterialDefinition def = MaterialDeserializer.parse(file, textureManager, gson);
            register(def);
        }
    }

    private void register(MaterialDefinition def) {
        int id = nextMaterialID++;
        def.id = id;
        nameToID.put(def.name, id);
        idToMaterial.put(id, def);
    }

    // API \\
    public MaterialDefinition getMaterial(String name) {
        return idToMaterial.get(nameToID.get(name));
    }

    public MaterialDefinition getMaterial(int id) {
        return idToMaterial.get(id);
    }

    public int getMaterialID(String name) {
        return nameToID.getOrDefault(name, -1);
    }
}
