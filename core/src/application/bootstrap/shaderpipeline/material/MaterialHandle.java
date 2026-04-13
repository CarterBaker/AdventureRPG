package application.bootstrap.shaderpipeline.material;

import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialHandle extends HandlePackage {

    /*
     * Persistent material definition. Registered and owned by MaterialManager
     * from bootstrap until shutdown. Wraps the original MaterialData — the
     * authoritative source used to clone instances at runtime.
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