package com.internal.bootstrap.shaderpipeline.materialmanager;

import java.io.File;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.Shader.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.Texture.TextureHandle;
import com.internal.bootstrap.shaderpipeline.material.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformUtility;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Constructs MaterialHandle objects from JSON descriptors during bootstrap.
 * Resolves shader and UBO references by name, parses uniform initial values
 * via UniformJsonUtility, and resolves texture names to GPU handles.
 * Released after bootstrap completes.
 */
class InternalBuilder extends BuilderPackage {

    // Internal
    private TextureManager textureManager;
    private ShaderManager shaderManager;
    private UBOManager uboManager;

    // Base \\

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.shaderManager = get(ShaderManager.class);
        this.uboManager = get(UBOManager.class);
    }

    // Build \\

    MaterialHandle build(File root, File file, int materialID) {

        String materialName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        JsonObject json = JsonUtility.loadJsonObject(file);
        int shaderID = getShaderID(json);
        ShaderHandle shader = shaderManager.getShaderFromShaderID(shaderID);

        Object2ObjectOpenHashMap<String, UBOHandle> ubos = buildUBOMap(shader);
        Object2ObjectOpenHashMap<String, Uniform<?>> uniforms = shader.getUniforms();

        updateUniforms(json, uniforms);

        MaterialHandle materialHandle = create(MaterialHandle.class);
        materialHandle.constructor(materialName, materialID, shader, ubos, uniforms);

        return materialHandle;
    }

    private int getShaderID(JsonObject json) {
        String shaderName = JsonUtility.validateString(json, "shader");
        return shaderManager.getShaderIDFromShaderName(shaderName);
    }

    private Object2ObjectOpenHashMap<String, UBOHandle> buildUBOMap(ShaderHandle shader) {

        ObjectArrayList<String> blockNames = shader.getUBOBlockNames();
        Object2ObjectOpenHashMap<String, UBOHandle> ubos = new Object2ObjectOpenHashMap<>();

        for (int i = 0; i < blockNames.size(); i++) {
            String name = blockNames.get(i);
            ubos.put(name, uboManager.getUBOHandleFromUBOName(name));
        }

        return ubos;
    }

    // Uniforms \\

    private void updateUniforms(
            JsonObject json,
            Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {

        for (String key : json.keySet()) {

            if (key.equals("shader"))
                continue;

            Uniform<?> uniform = uniforms.get(key);
            if (uniform == null)
                continue;

            updateUniform(json, key, uniform);
        }
    }

    private void updateUniform(JsonObject json, String uniformName, Uniform<?> uniform) {

        JsonElement element = json.get(uniformName);

        if (!element.isJsonObject())
            throwException("Uniform '" + uniformName + "' must be a JSON object with 'type' and 'value' fields");

        JsonObject uniformData = element.getAsJsonObject();

        // Texture samplers are resolved separately — they don't go through
        // UniformJsonUtility because their "value" is a name, not a data literal.
        if (isTextureSampler(uniformData)) {
            resolveAndApplyTexture(uniform.attribute(), uniformData.get("value").getAsString());
            return;
        }

        UniformUtility.applyFromJsonObject(uniform.attribute(), uniformName, uniformData);
    }

    private boolean isTextureSampler(JsonObject uniformData) {
        if (!uniformData.has("type"))
            return false;
        String type = uniformData.get("type").getAsString();
        return type.equals("SAMPLE_IMAGE_2D") || type.equals("SAMPLE_IMAGE_2D_ARRAY");
    }

    @SuppressWarnings("unchecked")
    private void resolveAndApplyTexture(UniformAttribute<?> attribute, String textureArrayName) {
        ((UniformAttribute<Integer>) attribute).set(resolveTextureGPUHandle(textureArrayName));
    }

    private int resolveTextureGPUHandle(String textureArrayName) {
        TextureHandle textureHandle = textureManager.getArrayHandleFromArrayName(textureArrayName);
        if (textureHandle == null)
            throwException("Texture array not found: " + textureArrayName);
        return textureHandle.getGPUHandle();
    }
}