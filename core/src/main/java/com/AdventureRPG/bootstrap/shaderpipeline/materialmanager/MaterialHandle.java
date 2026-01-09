package com.AdventureRPG.bootstrap.shaderpipeline.materialmanager;

import com.AdventureRPG.bootstrap.shaderpipeline.shadermanager.ShaderHandle;
import com.AdventureRPG.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.AdventureRPG.bootstrap.shaderpipeline.uniforms.Uniform;
import com.AdventureRPG.core.engine.HandlePackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class MaterialHandle extends HandlePackage {

    // Internal
    private String materialName;
    private int materialID;

    private ShaderHandle shaderHandle;
    private Object2ObjectOpenHashMap<String, UBOHandle> buffers;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public void constructor(
            String materialName,
            int materialID,
            ShaderHandle shaderHandle,
            Object2ObjectOpenHashMap<String, UBOHandle> buffers,
            Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {

        // Internal
        this.materialName = materialName;
        this.materialID = materialID;

        this.shaderHandle = shaderHandle;
        this.buffers = buffers;
        this.uniforms = uniforms;
    }

    // Accessible \\

    public String getMaterialName() {
        return materialName;
    }

    public int getMaterialID() {
        return materialID;
    }

    public ShaderHandle getShaderHandle() {
        return shaderHandle;
    }

    public UBOHandle getUBO(String ubo) {
        return buffers.get(ubo);
    }

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }
}
