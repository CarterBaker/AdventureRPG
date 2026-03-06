package com.internal.bootstrap.shaderpipeline.material;

import com.internal.bootstrap.shaderpipeline.Shader.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.InstancePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Runtime material handed to external systems by MaterialManager.cloneMaterial().
 * Owns all data it needs directly — shader handle, name, ID, source UBO map,
 * a deep-copied uniform map, and a per-instance UBO map keyed by binding point
 * for render-time access without name lookup.
 */
public class MaterialInstance extends InstancePackage {

    // Internal
    private String materialName;
    private int materialID;
    private ShaderHandle shaderHandle;
    private Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs;
    private Int2ObjectOpenHashMap<UBOInstance> instanceUBOs;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;
    // Internal \\

    public void constructor(
            String materialName,
            int materialID,
            ShaderHandle shaderHandle,
            Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs,
            Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {
        this.materialName = materialName;
        this.materialID = materialID;
        this.shaderHandle = shaderHandle;
        this.sourceUBOs = sourceUBOs;
        this.instanceUBOs = new Int2ObjectOpenHashMap<>();
        this.uniforms = uniforms;
    }

    // Utility \\

    public void setUBO(UBOInstance ubo) {
        instanceUBOs.put(ubo.getBindingPoint(), ubo);
    }

    @SuppressWarnings("unchecked")
    public <T> void setUniform(String uniformName, T value) {
        Uniform<?> uniform = uniforms.get(uniformName);
        if (uniform == null)
            throwException("Uniform '" + uniformName + "' not found in material '" + materialName + "'");
        ((Uniform<T>) uniform).attribute().set(value);
    }

    // Accessible \\

    public ShaderHandle getShaderHandle() {
        return shaderHandle;
    }

    public String getMaterialName() {
        return materialName;
    }

    public int getMaterialID() {
        return materialID;
    }

    public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs() {
        return sourceUBOs;
    }

    public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs() {
        return instanceUBOs;
    }

    public UBOInstance getInstanceUBO(int bindingPoint) {
        return instanceUBOs.get(bindingPoint);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }
}