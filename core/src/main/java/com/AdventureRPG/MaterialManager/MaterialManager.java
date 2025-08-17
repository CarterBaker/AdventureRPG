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
        this.gson = gameManager.gson;
        this.textureManager = gameManager.TextureManager;

        this.MATERIAL_JSON_PATH = settings.MATERIAL_JSON_PATH;

        assembleAllMaterials();

        int totalTextureIDs = textureManager.getNextTextureID();
        textureToMaterial = new int[totalTextureIDs];
        Arrays.fill(textureToMaterial, -1);
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

        // Map the albedo texture (if it exists) to this material ID
        String albedoPath = def.textureRefs.get("albedo"); // may be null
        if (albedoPath != null) {
            int textureID = textureManager.getIDFromTexture(albedoPath);
            if (textureID != -1 && textureToMaterial[textureID] == -1) {
                textureToMaterial[textureID] = id;
            }
        }
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

    public MaterialDefinition getMaterialFromTextureID(int textureID) {

        if (textureID < 0 || textureID >= textureToMaterial.length)
            return null;

        int matID = textureToMaterial[textureID];
        return matID != -1 ? getMaterial(matID) : null;
    }
}
