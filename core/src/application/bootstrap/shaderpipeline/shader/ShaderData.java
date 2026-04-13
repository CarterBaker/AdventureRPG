package application.bootstrap.shaderpipeline.shader;

import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.core.engine.DataPackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderData extends DataPackage {

    /*
     * Persistent runtime payload for a compiled GPU shader program. Owned by
     * ShaderHandle for the full engine session. Contains only what survives
     * past compilation — identity, the GL program integer, compiled uniforms,
     * and UBO block names.
     */

    // Identity
    private final String shaderName;
    private final int shaderID;
    private final int gpuHandle;

    // Runtime
    private final Object2ObjectOpenHashMap<String, UniformStruct<?>> compiledUniforms;
    private final ObjectArrayList<String> compiledUBOBlockNames;

    // Constructor \\

    public ShaderData(String shaderName, int shaderID, int gpuHandle) {

        this.shaderName = shaderName;
        this.shaderID = shaderID;
        this.gpuHandle = gpuHandle;
        this.compiledUniforms = new Object2ObjectOpenHashMap<>();
        this.compiledUBOBlockNames = new ObjectArrayList<>();
    }

    // Management \\

    void addCompiledUniform(String name, UniformStruct<?> uniform) {
        compiledUniforms.put(name, uniform);
    }

    void addCompiledUBOBlockName(String blockName) {
        compiledUBOBlockNames.add(blockName);
    }

    // Accessible \\

    public String getShaderName() {
        return shaderName;
    }

    public int getShaderID() {
        return shaderID;
    }

    public int getGpuHandle() {
        return gpuHandle;
    }

    public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms() {
        return compiledUniforms;
    }

    public ObjectArrayList<String> getCompiledUBOBlockNames() {
        return compiledUBOBlockNames;
    }
}