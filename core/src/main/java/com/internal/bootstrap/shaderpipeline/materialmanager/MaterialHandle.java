package com.internal.bootstrap.shaderpipeline.materialmanager;

import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.HandlePackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Compiled material owned exclusively by MaterialManager. Holds the shader reference
 * and the canonical UBO map populated at bootstrap. Never handed to external systems
 * directly — callers receive a MaterialInstance via MaterialManager.cloneMaterial().
 */
public class MaterialHandle extends HandlePackage {

    // Internal
    private String materialName;
    private int materialID;
    private ShaderHandle shaderHandle;
    private Object2ObjectOpenHashMap<String, UBOHandle> buffers;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    // Internal \\

    public void constructor(
            String materialName,
            int materialID,
            ShaderHandle shaderHandle,
            Object2ObjectOpenHashMap<String, UBOHandle> buffers,
            Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {
        this.materialName = materialName;
        this.materialID = materialID;
        this.shaderHandle = shaderHandle;
        this.buffers = buffers;
        this.uniforms = uniforms;
    }

    // Utility \\

    public void setUBO(String uboName, UBOHandle uboHandle) {
        buffers.put(uboName, uboHandle);
    }

    @SuppressWarnings("unchecked")
    public <T> void setUniform(String uniformName, T value) {
        Uniform<?> uniform = uniforms.get(uniformName);
        if (uniform == null)
            throwException("Uniform '" + uniformName + "' not found in material '" + materialName + "'");
        ((Uniform<T>) uniform).attribute().set(value);
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

    public UBOHandle getUBO(String uboName) {
        return buffers.get(uboName);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }
}