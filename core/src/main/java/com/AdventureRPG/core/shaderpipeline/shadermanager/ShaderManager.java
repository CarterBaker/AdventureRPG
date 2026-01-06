package com.AdventureRPG.core.shaderpipeline.shadermanager;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.shaderpipeline.ubomanager.UBOHandle;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ShaderManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> shaderName2ShaderID;
    private Int2ObjectOpenHashMap<ShaderHandle> shaderID2Shader;
    private Int2IntArrayMap shaderID2GPUHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = create(InternalLoadManager.class);

        // Retrieval Mapping
        this.shaderName2ShaderID = new Object2IntOpenHashMap<>();
        this.shaderID2Shader = new Int2ObjectOpenHashMap<>();
        this.shaderID2GPUHandle = new Int2IntArrayMap();
    }

    @Override
    protected void awake() {
        compileShaders();
    }

    @Override
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
    }

    @Override
    protected void dispose() {
        disposeAllGPUResources();
    }

    // ShaderHandle Management \\

    private void compileShaders() {
        internalLoadManager.loadShaders();
    }

    void addShader(ShaderHandle shader) {
        shaderName2ShaderID.put(shader.getShaderName(), shader.getShaderID());
        shaderID2Shader.put(shader.getShaderID(), shader);
        shaderID2GPUHandle.put(shader.getShaderID(), shader.getShaderHandle());
    }

    public void bindShaderToUBO(
            ShaderHandle shader,
            UBOHandle ubo) {
        GLSLUtility.bindUniformBlock(
                shader.getShaderHandle(),
                ubo.getBufferName(),
                ubo.getBindingPoint());
    }

    // Disposal \\

    private void disposeAllGPUResources() {

        // Dispose all shaders
        for (ShaderHandle shader : shaderID2Shader.values())
            GLSLUtility.deleteShaderProgram(shader.getShaderHandle());

        // Clear all mappings
        shaderName2ShaderID.clear();
        shaderID2Shader.clear();
        shaderID2GPUHandle.clear();
    }

    // Accessible \\

    public int getShaderIDFromShaderName(String shaderName) {

        if (!shaderName2ShaderID.containsKey(shaderName))
            throwException(
                    "ShaderHandle not found: " + shaderName);

        return shaderName2ShaderID.getInt(shaderName);
    }

    public ShaderHandle getShaderFromShaderID(int shaderID) {

        ShaderHandle shader = shaderID2Shader.get(shaderID);

        if (shader == null)
            throwException(
                    "ShaderHandle ID not found: " + shaderID);

        return shader;
    }

    public int getGPUHandleFromShaderID(int shaderID) {
        return shaderID2GPUHandle.get(shaderID);
    }
}
