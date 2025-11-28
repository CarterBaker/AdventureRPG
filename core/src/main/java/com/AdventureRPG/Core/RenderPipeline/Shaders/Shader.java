package com.AdventureRPG.Core.RenderPipeline.Shaders;

import com.AdventureRPG.Core.RenderPipeline.Uniforms.Uniform;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class Shader {

    // Internal
    public final String shaderName;
    public final int shaderID;
    public final int shaderHandle;

    private final Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public Shader(
            String shaderName,
            int shaderID,
            int shaderHandle) {

        // Internal
        this.shaderName = shaderName;
        this.shaderID = shaderID;
        this.shaderHandle = shaderHandle;

        this.uniforms = new Object2ObjectOpenHashMap<>();
    }

    // Utility \\

    public void addUniform(String uniformName, Uniform<?> uniform) {
        uniforms.put(uniformName, uniform);
    }

    public Uniform<?> getUniform(String uniformName) {
        return uniforms.get(uniformName);
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }
}
