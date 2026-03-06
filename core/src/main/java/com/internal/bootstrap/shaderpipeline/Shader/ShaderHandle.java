package com.internal.bootstrap.shaderpipeline.Shader;

import com.internal.bootstrap.shaderpipeline.uniforms.Uniform;
import com.internal.core.engine.HandlePackage;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/*
 * Compiled GPU shader program with its associated uniform and UBO block metadata.
 * UBO blocks are tracked by block name only — UBOHandle references never leave
 * UBOManager. Binding is performed at assembly time via UBOManager lookup.
 */
public class ShaderHandle extends HandlePackage {

    // Internal
    private String shaderName;
    private int shaderID;
    private int shaderHandle;
    private ObjectArrayList<String> uboBlockNames;
    private Object2ObjectOpenHashMap<String, Uniform<?>> uniforms;

    // Internal \\

    public void constructor(
            String shaderName,
            int shaderID,
            int shaderHandle) {
        this.shaderName = shaderName;
        this.shaderID = shaderID;
        this.shaderHandle = shaderHandle;
        this.uboBlockNames = new ObjectArrayList<>();
        this.uniforms = new Object2ObjectOpenHashMap<>();
    }

    // Utility \\

    public void addUBOBlock(String blockName) {
        uboBlockNames.add(blockName);
    }

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

    public ObjectArrayList<String> getUBOBlockNames() {
        return uboBlockNames;
    }

    public Object2ObjectOpenHashMap<String, Uniform<?>> getUniforms() {
        return uniforms;
    }
}