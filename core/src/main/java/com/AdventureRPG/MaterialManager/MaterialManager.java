package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.TextureManager.TextureManager;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import com.google.gson.Gson;

import java.io.File;
import java.util.*;

public class MaterialManager {

    private final Settings settings;
    private final Gson gson;
    private final TextureManager textureManager;

    private final String MATERIAL_JSON_PATH;

    private final Map<String, Integer> nameToID = new HashMap<>();
    private final Map<Integer, GameMaterial> idToGM = new HashMap<>();
    private final IdentityHashMap<Material, ShaderProgram> materialToShader = new IdentityHashMap<>();

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
            GameMaterial gm = MaterialDeserializer.parsePackage(file, textureManager, gson);
            register(gm);
        }
    }

    private void register(GameMaterial gm) {
        int id = nextMaterialID++;
        nameToID.put(gm.id, id);
        idToGM.put(id, gm);

        if (gm.shader != null) {
            materialToShader.put(gm.material, gm.shader);
        }

        // Optional: auto map by the albedo/diffuse texture
        if (gm.material.has(TextureAttribute.Diffuse)) {
            TextureAttribute albedo = (TextureAttribute) gm.material.get(TextureAttribute.Diffuse);
            Texture tex = albedo.textureDescription.texture;
            int textureID = textureManager.getIDFromTexture(tex.toString());
            // ^ If your TextureManager expects region/atlas names, adapt accordingly.
            if (textureID != -1) {
                ensureTextureToMaterialCapacity(textureID + 1);
                if (textureToMaterial[textureID] == -1) {
                    textureToMaterial[textureID] = id;
                }
            }
        }
    }

    private void ensureTextureToMaterialCapacity(int min) {
        if (textureToMaterial.length < min) {
            int[] n = Arrays.copyOf(textureToMaterial, min);
            Arrays.fill(n, textureToMaterial.length, n.length, -1);
            textureToMaterial = n;
        }
    }

    // -------- API (Unity-like convenience) --------

    public Material getMaterial(String name) {
        Integer id = nameToID.get(name);
        return (id != null) ? idToGM.get(id).material : null;
    }

    public ShaderProgram getShader(String name) {
        Integer id = nameToID.get(name);
        return (id != null) ? idToGM.get(id).shader : null;
    }

    public Material getMaterial(int id) {
        GameMaterial gm = idToGM.get(id);
        return gm != null ? gm.material : null;
    }

    public ShaderProgram getShaderFromID(int id) {
        GameMaterial gm = idToGM.get(id);
        return gm != null ? gm.shader : null;
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

    /** Used by the ShaderProvider during rendering. */
    public ShaderProgram getShaderForMaterial(Material mat) {
        return materialToShader.get(mat); // null means "use default shader"
    }

    /** Call on shutdown to release shader programs compiled from JSON. */
    public void disposeShaders() {
        HashSet<ShaderProgram> unique = new HashSet<>(materialToShader.values());
        unique.forEach(ShaderProgram::dispose);
        materialToShader.clear();
    }
}
