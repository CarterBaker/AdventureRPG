package com.internal.bootstrap.shaderpipeline.materialmanager;

import java.io.File;

import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.material.MaterialData;
import com.internal.bootstrap.shaderpipeline.material.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.shader.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformStruct;
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
     * Resolves shader and UBO references, clones uniforms from the compiled
     * shader and applies JSON default values. Created and owned by InternalLoader.
     */

    // Internal
    private ShaderManager shaderManager;
    private UBOManager uboManager;
    private MaterialManager materialManager;

    // Internal \\

    @Override
    protected void get() {
        this.shaderManager = get(ShaderManager.class);
        this.uboManager = get(UBOManager.class);
        this.materialManager = get(MaterialManager.class);
    }

    // Build \\

    void build(File file, String materialName) {

        if (materialManager.hasMaterial(materialName))
            return;

        JsonObject json = JsonUtility.loadJsonObject(file);
        String shaderName = JsonUtility.validateString(json, "shader");
        ShaderHandle shaderHandle = shaderManager.getShaderHandleFromShaderName(shaderName);

        Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs = new Object2ObjectOpenHashMap<>();

        if (json.has("ubos")) {
            JsonObject ubosJson = json.getAsJsonObject("ubos");
            for (String uboName : ubosJson.keySet())
                sourceUBOs.put(uboName, uboManager.getUBOHandleFromUBOName(uboName));
        }

        Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms = new Object2ObjectOpenHashMap<>();

        if (json.has("uniforms")) {

            JsonObject uniformsJson = json.getAsJsonObject("uniforms");
            Object2ObjectOpenHashMap<String, UniformStruct<?>> shaderUniforms = shaderHandle.getCompiledUniforms();

            for (String uniformName : uniformsJson.keySet()) {

                UniformStruct<?> source = shaderUniforms.get(uniformName);

                if (source == null)
                    throwException("Material '" + materialName + "' references unknown uniform: " + uniformName);

                UniformStruct<?> cloned = source.clone();

                UniformUtility.applyFromJsonObject(
                        cloned.attribute(),
                        uniformName,
                        uniformsJson.getAsJsonObject(uniformName));

                uniforms.put(uniformName, cloned);
            }
        }

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
}