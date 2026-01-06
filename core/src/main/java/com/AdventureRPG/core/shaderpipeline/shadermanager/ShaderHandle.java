package com.AdventureRPG.core.shaderpipeline.shadermanager;

import com.AdventureRPG.core.engine.HandlePackage;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOHandle;
import com.AdventureRPG.core.shaderpipeline.uniforms.Uniform;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class ShaderHandle extends HandlePackage {

    // Internal
    private String shaderName;
    private int shaderID;
    private int shaderHandle;

    private Object2ObjectOpenHashMap<String, UBOHandle> buffers;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    public void constructor(
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

    // Accessible \\

    public String getShaderName() {
        return shaderName;
    }

    public int getShaderID() {
        return shaderID;
    }

    public int getShaderHandle() {
        return shaderHandle;
    }

    public Object2ObjectOpenHashMap<String, UBOHandle> getBuffers() {
        return buffers;
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }
}
