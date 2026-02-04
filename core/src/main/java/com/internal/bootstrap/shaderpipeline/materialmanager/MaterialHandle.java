package com.internal.bootstrap.shaderpipeline.materialmanager;

import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.HandlePackage;

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

    public Object2ObjectOpenHashMap<String, UBOHandle> getUBOs() {
        return buffers;
    }

    public void setUBO(String uboName, UBOHandle uboHandle) {
        buffers.put(uboName, uboHandle);
    }

    public UBOHandle getUBO(String ubo) {
        return buffers.get(ubo);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }

    @SuppressWarnings("unchecked")
    public <T> void setUniform(String uniformName, T value) {
        Uniform<?> uniform = uniforms.get(uniformName);

        if (uniform == null) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found in material '" + materialName + "'");
            return;
        }

        // Cast the uniform and set the value
        ((Uniform<T>) uniform).attribute().set(value);
    }
}
