package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.TextureManager.TextureManager;
import com.badlogic.gdx.graphics.g3d.Material;
import com.google.gson.Gson;

import java.io.File;
import java.util.*;

public class MaterialManager {

    private final Settings settings;
    private final Gson gson;
    private final TextureManager textureManager;

    private final String MATERIAL_JSON_PATH;

    private final Map<String, Integer> nameToID = new HashMap<>();
    private final Map<Integer, Material> idToMaterial = new HashMap<>();
    private int[] textureToMaterial;
    private int nextMaterialID = 0;

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
            Material mat = MaterialDeserializer.parse(file, textureManager, gson);
            register(file.getName(), mat);
        }
    }

    private void register(String name, Material mat) {
        int id = nextMaterialID++;
        nameToID.put(name, id);
        idToMaterial.put(id, mat);

        // Default mapping via albedo → textureID → materialID
        if (mat.has(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse)) {
            com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute albedo = (com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute) mat
                    .get(com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute.Diffuse);

            int textureID = textureManager.getIDFromTexture(albedo.textureDescription.texture.toString());
            if (textureID != -1 && textureToMaterial[textureID] == -1) {
                textureToMaterial[textureID] = id;
            }
        }
    }

    // API
    public Material getMaterial(String name) {
        Integer id = nameToID.get(name);
        return id != null ? idToMaterial.get(id) : null;
    }

    public Material getMaterial(int id) {
        return idToMaterial.get(id);
    }

    public int getMaterialID(String name) {
        return nameToID.getOrDefault(name, -1);
    }

    public Material getMaterialFromTextureID(int textureID) {
        if (textureID < 0 || textureID >= textureToMaterial.length)
            return null;
        int matID = textureToMaterial[textureID];
        return matID != -1 ? getMaterial(matID) : null;
    }
}
