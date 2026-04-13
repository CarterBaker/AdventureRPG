package application.bootstrap.shaderpipeline.material;

import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import engine.root.InstancePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialInstance extends InstancePackage {

    /*
     * Runtime material cloned from a MaterialHandle. Wraps a deep-copied
     * MaterialData — safe to mutate uniforms and attach instance UBOs.
     * Owned by whoever requested the clone and discarded when no longer needed.
     */

    // Internal
    private MaterialData data;

    // Constructor \\

    public void constructor(MaterialData data) {
        this.data = data;
    }

    // Utility \\

    public void setUBO(UBOInstance ubo) {
        data.setUBO(ubo);
    }

    public <T> void setUniform(String uniformName, T value) {
        data.setUniform(uniformName, value);
    }

    // Accessible \\

    public MaterialData getMaterialData() {
        return data;
    }

    public String getMaterialName() {
        return data.getMaterialName();
    }

    public int getMaterialID() {
        return data.getMaterialID();
    }

    public ShaderHandle getShaderHandle() {
        return data.getShaderHandle();
    }

    public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs() {
        return data.getSourceUBOs();
    }

    public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs() {
        return data.getInstanceUBOs();
    }

    public UBOInstance getInstanceUBO(int bindingPoint) {
        return data.getInstanceUBO(bindingPoint);
    }

    public Object2ObjectOpenHashMap<String, UniformStruct<?>> getUniforms() {
        return data.getUniforms();
    }

    public ObjectArrayList<String> getUniformKeys() {
        return data.getUniformKeys();
    }

    public UniformStruct<?> getUniform(String uniformName) {
        return data.getUniform(uniformName);
    }
}