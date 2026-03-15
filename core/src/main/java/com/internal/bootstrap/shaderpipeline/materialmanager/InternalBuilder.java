package com.internal.bootstrap.shaderpipeline.materialmanager;

import java.io.File;
import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.material.MaterialData;
import com.internal.bootstrap.shaderpipeline.material.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.shader.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.texturemanager.TextureManager;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttributeStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformStruct;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformUtility;
import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderManager;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.BuilderPackage;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalBuilder extends BuilderPackage {

    /*
     * Parses material JSON files into MaterialHandles during bootstrap.
     * Uniforms are cloned from the compiled shader — the optional "uniforms"
     * block applies default values to named uniforms. Sampler uniforms with
     * string values are resolved through TextureManager to GPU handles.
     */

    // Internal
    private ShaderManager shaderManager;
    private UBOManager uboManager;
    private MaterialManager materialManager;
    private TextureManager textureManager;

    // Base \\

    @Override
    protected void get() {

        // Internal
        this.shaderManager = get(ShaderManager.class);
        this.uboManager = get(UBOManager.class);
        this.materialManager = get(MaterialManager.class);
        this.textureManager = get(TextureManager.class);
    }

    // Build \\

    void build(File file, String materialName) {

        if (materialManager.hasMaterial(materialName))
            return;

        JsonObject json = JsonUtility.loadJsonObject(file);
        String shaderName = JsonUtility.validateString(json, "shader");
        ShaderHandle shaderHandle = shaderManager.getShaderHandleFromShaderName(shaderName);

        // UBOs
        Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs = new Object2ObjectOpenHashMap<>();

        if (json.has("ubos")) {
            JsonObject ubosJson = json.getAsJsonObject("ubos");
            for (String uboName : ubosJson.keySet())
                sourceUBOs.put(uboName, uboManager.getUBOHandleFromUBOName(uboName));
        }

        // Uniforms — clone all from shader, apply JSON overrides where declared
        Object2ObjectOpenHashMap<String, UniformStruct<?>> shaderUniforms = shaderHandle.getCompiledUniforms();
        Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms = new Object2ObjectOpenHashMap<>();

        for (String uniformName : shaderUniforms.keySet())
            uniforms.put(uniformName, shaderUniforms.get(uniformName).clone());

        if (json.has("uniforms")) {

            JsonObject uniformsJson = json.getAsJsonObject("uniforms");

            for (String uniformName : uniformsJson.keySet()) {

                UniformStruct<?> uniform = uniforms.get(uniformName);

                if (uniform == null)
                    throwException("Material '" + materialName
                            + "' references unknown uniform: " + uniformName);

                JsonObject uniformJson = uniformsJson.getAsJsonObject(uniformName);
                UniformAttributeStruct<?> attribute = uniform.attribute();

                if (isSamplerType(attribute.getUniformType())
                        && uniformJson.has("value")
                        && uniformJson.get("value").isJsonPrimitive()
                        && !uniformJson.get("value").getAsJsonPrimitive().isNumber()) {

                    String textureName = uniformJson.get("value").getAsString();
                    int gpuHandle = resolveTextureHandle(textureName, uniformName, materialName);

                    @SuppressWarnings("unchecked")
                    UniformAttributeStruct<Integer> samplerAttr = (UniformAttributeStruct<Integer>) attribute;
                    samplerAttr.set(gpuHandle);
                } else {
                    UniformUtility.applyFromJsonObject(attribute, uniformName, uniformJson);
                }
            }
        }

        // Construct
        int materialID = RegistryUtility.toIntID(materialName);
        MaterialData data = new MaterialData(
                materialName,
                materialID,
                shaderHandle,
                sourceUBOs,
                uniforms);

        MaterialHandle handle = create(MaterialHandle.class);
        handle.constructor(data);
        materialManager.addMaterial(materialName, handle);
    }

    // Sampler Resolution \\

    private boolean isSamplerType(UniformType type) {
        return type == UniformType.SAMPLE_IMAGE_2D
                || type == UniformType.SAMPLE_IMAGE_2D_ARRAY;
    }

    private int resolveTextureHandle(
            String textureName,
            String uniformName,
            String materialName) {

        if (textureManager.hasTexture(textureName))
            return textureManager.getTextureHandleFromTextureName(textureName).getGpuHandle();

        return textureManager.getTextureHandleFromArrayName(textureName).getGpuHandle();
    }
}