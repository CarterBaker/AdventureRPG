package com.internal.bootstrap.shaderpipeline.material;

import com.internal.bootstrap.shaderpipeline.materialmanager.MaterialHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOInstance;
import com.internal.bootstrap.shaderpipeline.shadermanager.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.InstancePackage;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Runtime material handed to external systems by MaterialManager.cloneMaterial().
 * Holds a back-reference to its source MaterialHandle for shared state — shader and
 * shared UBOs are never duplicated. Owns a deep-copied uniform map and a per-instance
 * UBO map keyed by binding point so no name lookup is needed at render time.
 */
public class MaterialInstance extends InstancePackage {

    // Internal
    private MaterialHandle source;
    private Int2ObjectOpenHashMap<UBOInstance> instanceUBOs;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    // Internal \\

    public void constructor(
            MaterialHandle source,
            Object2ObjectOpenHashMap<String, Uniform<?>> uniforms) {
        this.source = source;
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
            throwException("Uniform '" + uniformName + "' not found in material '" + source.getMaterialName() + "'");
        ((Uniform<T>) uniform).attribute().set(value);
    }

    // Accessible \\

    public MaterialHandle getSource() {
        return source;
    }

    public ShaderHandle getShaderHandle() {
        return source.getShaderHandle();
    }

    public String getMaterialName() {
        return source.getMaterialName();
    }

    public int getMaterialID() {
        return source.getMaterialID();
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