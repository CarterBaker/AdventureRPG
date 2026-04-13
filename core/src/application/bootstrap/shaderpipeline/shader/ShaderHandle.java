package application.bootstrap.shaderpipeline.shader;

import application.bootstrap.shaderpipeline.uniforms.UniformStruct;
import application.core.engine.HandlePackage;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShaderHandle extends HandlePackage {

    /*
     * Persistent compiled shader program. Wraps ShaderData and delegates all
     * access through it. Registered in ShaderManager from compile time to
     * shutdown. Source phase data never reaches this class — it lives and dies
     * in ShaderSourceStruct during bootstrap.
     */

    // Internal
    private ShaderData shaderData;

    // Internal \\

    public void constructor(ShaderData shaderData) {
        this.shaderData = shaderData;
    }

    // Management \\

    public void addCompiledUniform(String name, UniformStruct<?> uniform) {
        shaderData.addCompiledUniform(name, uniform);
    }

    public void addCompiledUBOBlockName(String blockName) {
        shaderData.addCompiledUBOBlockName(blockName);
    }

    // Accessible \\

    public ShaderData getShaderData() {
        return shaderData;
    }

    public String getShaderName() {
        return shaderData.getShaderName();
    }

    public int getShaderID() {
        return shaderData.getShaderID();
    }

    public int getGpuHandle() {
        return shaderData.getGpuHandle();
    }

    public Object2ObjectOpenHashMap<String, UniformStruct<?>> getCompiledUniforms() {
        return shaderData.getCompiledUniforms();
    }

    public ObjectArrayList<String> getCompiledUBOBlockNames() {
        return shaderData.getCompiledUBOBlockNames();
    }
}