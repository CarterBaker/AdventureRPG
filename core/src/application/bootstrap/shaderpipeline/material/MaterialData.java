package application.bootstrap.shaderpipeline.material;

import application.bootstrap.shaderpipeline.shader.ShaderHandle;
import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubo.UBOInstance;
import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.core.engine.DataPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class MaterialData extends DataPackage {

    /*
     * Full material definition. On a MaterialHandle this is the original parsed
     * definition — authoritative and never mutated by external systems. On a
     * MaterialInstance this is a deep-copied clone — safe to mutate and discard.
     * uniformKeys mirrors the uniforms map for zero-allocation clone iteration.
     */

    // Identity
    private final String materialName;
    private final int materialID;
    private final ShaderHandle shaderHandle;

    // Source UBOs — shared reference, never cloned
    private final Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs;

    // Per-instance state — deep-copied on clone
    private final Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms;
    private final ObjectArrayList<String> uniformKeys;
    private final Int2ObjectOpenHashMap<UBOInstance> instanceUBOs;

    // Constructor — original \\

    public MaterialData(
            String materialName,
            int materialID,
            ShaderHandle shaderHandle,
            Object2ObjectOpenHashMap<String, UBOHandle> sourceUBOs,
            Object2ObjectOpenHashMap<String, UniformStruct<?>> uniforms) {

        this.materialName = materialName;
        this.materialID = materialID;
        this.shaderHandle = shaderHandle;
        this.sourceUBOs = sourceUBOs;
        this.uniforms = uniforms;
        this.uniformKeys = new ObjectArrayList<>(uniforms.keySet());
        this.instanceUBOs = new Int2ObjectOpenHashMap<>();
    }

    // Constructor — clone \\

    public MaterialData(MaterialData source) {

        // Identity — shared, never copied
        this.materialName = source.materialName;
        this.materialID = source.materialID;
        this.shaderHandle = source.shaderHandle;
        this.sourceUBOs = source.sourceUBOs;

        // Per-instance state — deep copy
        this.uniforms = new Object2ObjectOpenHashMap<>(source.uniforms.size());
        this.uniformKeys = new ObjectArrayList<>(source.uniformKeys);
        this.instanceUBOs = new Int2ObjectOpenHashMap<>();

        for (int i = 0; i < source.uniformKeys.size(); i++) {
            String key = source.uniformKeys.get(i);
            this.uniforms.put(key, source.uniforms.get(key).clone());
        }
    }

    // Utility \\

    public void setUBO(UBOInstance ubo) {
        instanceUBOs.put(ubo.getBindingPoint(), ubo);
    }

    @SuppressWarnings("unchecked")
    public <T> void setUniform(String uniformName, T value) {

        UniformStruct<?> uniform = uniforms.get(uniformName);

        if (uniform == null)
            throwException("Uniform '" + uniformName + "' not found in material '" + materialName + "'");

        ((UniformStruct<T>) uniform).attribute().set(value);
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

    public Object2ObjectOpenHashMap<String, UBOHandle> getSourceUBOs() {
        return sourceUBOs;
    }

    public Int2ObjectOpenHashMap<UBOInstance> getInstanceUBOs() {
        return instanceUBOs;
    }

    public UBOInstance getInstanceUBO(int bindingPoint) {
        return instanceUBOs.get(bindingPoint);
    }

    public Object2ObjectOpenHashMap<String, UniformStruct<?>> getUniforms() {
        return uniforms;
    }

    public ObjectArrayList<String> getUniformKeys() {
        return uniformKeys;
    }

    public UniformStruct<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }
}