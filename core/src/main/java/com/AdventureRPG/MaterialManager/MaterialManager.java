package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.GameManager;
import com.AdventureRPG.SettingsSystem.Settings;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.ShaderManager.UniformAttribute;
import com.AdventureRPG.TextureManager.TextureManager;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class MaterialManager {

    // Game Manager
    private final Settings settings;
    private final Gson gson;
    private final TextureManager textureManager;
    private final ShaderManager shaderManager;

    // Settings
    private final String MATERIAL_JSON_PATH;

    // Material storage
    private final Map<Integer, MaterialData> materialsById = new HashMap<>();
    private final Map<String, Integer> idByName = new HashMap<>();

    // Map of Material to ShaderProgram for O(1) lookup
    private final Map<Material, ShaderProgram> shaderByMaterial = new HashMap<>();

    private int nextId = 0;

    public MaterialManager(GameManager gameManager) {

        // Game Manager
        this.settings = gameManager.settings;
        this.gson = gameManager.gson;
        this.textureManager = gameManager.textureManager;
        this.shaderManager = gameManager.shaderManager;

        // Settings
        this.MATERIAL_JSON_PATH = settings.MATERIAL_JSON_PATH;
    }

    public void awake() {
        compileMaterials();
    }

    public void start() {

    }

    public void update() {

    }

    public void dispose() {
        materialsById.clear();
        idByName.clear();
        shaderByMaterial.clear();
    }

    // Core Logic \\

    private void compileMaterials() {

        FileHandle folder = Gdx.files.internal(MATERIAL_JSON_PATH);

        for (FileHandle file : folder.list("json")) {

            try {

                MaterialDefinition def = gson.fromJson(file.reader(), MaterialDefinition.class);

                // Map shader
                int shaderID = shaderManager.getShaderID(def.shader);
                ShaderProgram shaderProgram = shaderManager.getShaderByID(shaderID);

                // Map texture arrays
                Map<String, Integer> textureIDs = new HashMap<>();

                if (def.textureArrays != null) {

                    for (String texRef : def.textureArrays) {

                        int texID = textureManager.getIDFromTexture(texRef);

                        if (texID != -1)
                            textureIDs.put(texRef, texID);
                    }
                }

                int id = nextId++;
                String name = file.nameWithoutExtension();

                // Build default LibGDX Material
                Material libgdxMaterial = new Material();

                // Build per-material uniforms map
                Map<String, UniformAttribute> uniforms = new HashMap<>();

                if (def.uniforms != null) {

                    for (UniformDefinition u : def.uniforms)
                        uniforms.put(u.name, new UniformAttribute(u.name, u.type, u.defaultValue));
                }

                MaterialData data = new MaterialData(id, name, shaderID, libgdxMaterial, uniforms, textureIDs);

                // Store
                materialsById.put(id, data);
                idByName.put(name, id);

                if (shaderProgram != null)
                    shaderByMaterial.put(libgdxMaterial, shaderProgram);

            }

            catch (Exception exception) {

            }
        }
    }

    // Utility \\

    private static class MaterialDefinition {
        public String shader;
        public String[] textureArrays;
        public UniformDefinition[] uniforms;
    }

    private static class UniformDefinition {
        public String name;
        public UniformAttribute.UniformType type;
        public Object defaultValue;
    }

    // Accessible \\

    public MaterialData getById(int id) {
        return materialsById.get(id);
    }

    public MaterialData getByName(String name) {
        Integer id = idByName.get(name);
        return id != null ? materialsById.get(id) : null;
    }

    // Retrieve the shader for a material
    public ShaderProgram getShaderForMaterial(Material material) {
        return shaderByMaterial.get(material);
    }

    // Set uniform for a material by ID
    public void setUniform(int matId, String uniformName, Object value, boolean pushNow) {
        MaterialData data = materialsById.get(matId);
        if (data == null)
            return;
        UniformAttribute attr = data.uniforms.get(uniformName);
        if (attr != null) {
            attr.value = value;
            if (pushNow)
                pushUniforms(data);
        }
    }

    // Set uniform by material name
    public void setUniform(String materialName, String uniformName, Object value, boolean pushNow) {
        MaterialData data = getByName(materialName);
        if (data != null)
            setUniform(data.id, uniformName, value, pushNow);
    }

    // Push uniforms of one material to its shader
    public void pushUniforms(MaterialData data) {
        ShaderProgram shader = shaderByMaterial.get(data.material);
        if (shader == null)
            return;

        for (UniformAttribute ua : data.uniforms.values()) {
            switch (ua.uniformType) {
                case FLOAT -> shader.setUniformf(ua.name, (float) ua.value);
                case INT -> shader.setUniformi(ua.name, (int) ua.value);
                case BOOL -> shader.setUniformi(ua.name, ((boolean) ua.value) ? 1 : 0);
                case VEC2 -> shader.setUniformf(ua.name, (com.badlogic.gdx.math.Vector2) ua.value);
                case VEC3 -> shader.setUniformf(ua.name, (com.badlogic.gdx.math.Vector3) ua.value);
                case VEC4 -> shader.setUniformf(ua.name, (com.badlogic.gdx.math.Vector4) ua.value);
                case COLOR -> shader.setUniformf(ua.name, (com.badlogic.gdx.graphics.Color) ua.value);
                case MATRIX4 -> shader.setUniformMatrix(ua.name, (com.badlogic.gdx.math.Matrix4) ua.value);
            }
        }
    }
}
