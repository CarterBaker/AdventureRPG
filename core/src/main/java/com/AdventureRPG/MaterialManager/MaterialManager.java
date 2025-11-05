package com.AdventureRPG.MaterialManager;

import com.AdventureRPG.Core.Exceptions.FileException;
import com.AdventureRPG.Core.Exceptions.GraphicException;
import com.AdventureRPG.Core.Framework.GameSystem;
import com.AdventureRPG.ShaderManager.ShaderManager;
import com.AdventureRPG.ShaderManager.UniformAttribute;
import com.AdventureRPG.TextureManager.TextureManager;
import com.AdventureRPG.Util.GlobalConstant;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.TextureArray;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// TODO: This class needs to be examined closely
public class MaterialManager extends GameSystem {

    // Game Manager
    private Gson gson;
    private TextureManager textureManager;
    private ShaderManager shaderManager;

    // Settings
    private String MATERIAL_JSON_PATH;

    // Material Reference
    private Map<Integer, MaterialData> materialsById;
    private Map<String, Integer> idByName;;

    // Data Lookup
    private List<MaterialData> materials;

    private Map<TextureArray, MaterialData> arrayToFirstMaterial;
    private Map<Material, MaterialData> dataByMaterial;
    private Map<Material, ShaderProgram> shaderByMaterial;

    private int nextId;

    @Override
    public void init() {

        // Game Manager
        this.gson = rootManager.gson;
        this.textureManager = rootManager.textureManager;
        this.shaderManager = rootManager.shaderManager;

        // Settings
        this.MATERIAL_JSON_PATH = GlobalConstant.MATERIAL_JSON_PATH;

        // Material Reference
        this.materialsById = new HashMap<>();
        this.idByName = new HashMap<>();

        // Data Lookup
        this.materials = new ArrayList<>();

        this.arrayToFirstMaterial = new HashMap<>();
        this.dataByMaterial = new HashMap<>();
        this.shaderByMaterial = new HashMap<>();

        this.nextId = 0;

        compileMaterials();
    }

    @Override
    public void update() {

        for (int i = 0, n = materials.size(); i < n; i++)
            materials.get(i).updateUniversalUniforms();
    }

    @Override
    public void dispose() {

        materialsById.clear();
        idByName.clear();
        shaderByMaterial.clear();
    }

    // Material Manager \\

    private void compileMaterials() {

        FileHandle directory = Gdx.files.internal(MATERIAL_JSON_PATH);

        if (!directory.exists() || !directory.isDirectory())
            throw new FileException.FileNotFoundException(directory.file());

        for (FileHandle file : directory.list("json")) {

            try {

                MaterialDefinition def = gson.fromJson(file.reader(), MaterialDefinition.class);

                if (def == null)
                    continue; // safety

                // Map shader
                int shaderID = shaderManager.getShaderID(def.shader);
                ShaderProgram shaderProgram = shaderManager.getShaderByID(shaderID);

                // --- IMPORTANT: use folder name only (def.texture is folder name) ---
                if (def.texture == null || def.texture.isEmpty())
                    throw new GraphicException.MissingTextureFieldException(file.name());

                // Get the TextureArray using ONLY the folder name
                TextureArray textureArray = textureManager.getArray(def.texture);
                if (textureArray == null)
                    throw new GraphicException.TextureArrayNotFoundException(def.texture, file.name());

                int id = nextId++;
                String name = file.nameWithoutExtension();

                // Build default LibGDX Material
                Material libgdxMaterial = new Material();

                // Build per-material uniforms map (always create a map; empty if none)
                Map<String, UniformAttribute> uniforms = new HashMap<>();

                if (def.uniforms != null)
                    for (UniformDefinition u : def.uniforms) {

                        if (u == null || u.name == null)
                            continue;

                        uniforms.put(u.name, new UniformAttribute(u.name, u.type, u.defaultValue));
                    }

                MaterialData data = new MaterialData(
                        id,
                        name,
                        libgdxMaterial,
                        textureArray,
                        shaderProgram,
                        uniforms,
                        shaderManager.universalUniform);

                // Store
                materials.add(data);
                arrayToFirstMaterial.putIfAbsent(textureArray, data);
                materialsById.put(id, data);
                idByName.put(name, id);

                dataByMaterial.put(libgdxMaterial, data);

                if (shaderProgram != null)
                    shaderByMaterial.put(libgdxMaterial, shaderProgram);
            }

            catch (Exception e) {
                throw new GraphicException.MaterialDefinitionException(file.name(), e);
            }
        }
    }

    // Utility \\

    private static class MaterialDefinition {
        public String shader;
        public String texture; // folder name ONLY
        public UniformDefinition[] uniforms; // optional; can be omitted in JSON
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

    public MaterialData getFirstMaterialUsingID(int textureID) {

        if (textureID < 0)
            return null;

        TextureArray array = textureManager.getArrayFromID(textureID);

        return arrayToFirstMaterial.get(array);
    }

    public MaterialData getDataForMaterial(Material material) {
        return dataByMaterial.get(material);
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