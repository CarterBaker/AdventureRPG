package com.AdventureRPG.core.shaders.shaders;

import com.AdventureRPG.core.shaders.ubomanager.UBOHandle;
import com.AdventureRPG.core.shaders.uniforms.Uniform;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class Shader {

    // Internal
    public final String shaderName;
    public final int shaderID;
    public final int shaderHandle;

    private final Object2ObjectOpenHashMap<String, UBOHandle> buffers;
    private final Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public Shader(
            String shaderName,
            int shaderID,
            int shaderHandle) {

        // Internal
        this.shaderName = shaderName;
        this.shaderID = shaderID;
        this.shaderHandle = shaderHandle;

        this.buffers = new Object2ObjectOpenHashMap<>();
        this.uniforms = new Object2ObjectOpenHashMap<>();
    }

    // Utility \\

    // Buffers
    public void addBuffer(String bufferName, UBOHandle buffer) {
        buffers.put(bufferName, buffer);
    }

    // Uniforms
    public void addUniform(String uniformName, Uniform<?> uniform) {
        uniforms.put(uniformName, uniform);
    }

    public Object2ObjectOpenHashMap<String, UBOHandle> getBuffers() {
        return buffers;
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }
}
