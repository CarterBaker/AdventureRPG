package com.internal.bootstrap.shaderpipeline.shadermanager;

import com.internal.bootstrap.shaderpipeline.Shader.ShaderHandle;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ShaderManager extends ManagerPackage {

    // Internal
    private UBOManager uboManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> shaderName2ShaderID;
    private Int2ObjectOpenHashMap<ShaderHandle> shaderID2Shader;
    private Int2IntArrayMap shaderID2GPUHandle;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoader.class);
        this.shaderName2ShaderID = new Object2IntOpenHashMap<>();
        this.shaderID2Shader = new Int2ObjectOpenHashMap<>();
        this.shaderID2GPUHandle = new Int2IntArrayMap();
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void dispose() {
        ShaderHandle[] handles = shaderID2Shader.values().toArray(new ShaderHandle[0]);
        for (int i = 0; i < handles.length; i++)
            GLSLUtility.deleteShaderProgram(handles[i].getShaderHandle());
        shaderName2ShaderID.clear();
        shaderID2Shader.clear();
        shaderID2GPUHandle.clear();
    }

    // On-Demand Loading \\

    public void request(String shaderName) {
        ((InternalLoader) internalLoader).request(shaderName);
    }

    // Shader Management \\

    void addShader(ShaderHandle shader) {
        shaderName2ShaderID.put(shader.getShaderName(), shader.getShaderID());
        shaderID2Shader.put(shader.getShaderID(), shader);
        shaderID2GPUHandle.put(shader.getShaderID(), shader.getShaderHandle());
    }

    public void bindShaderToUBO(ShaderHandle shader, String blockName) {
        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(blockName);
        GLSLUtility.bindUniformBlock(
                shader.getShaderHandle(),
                blockName,
                ubo.getBindingPoint());
    }

    // Accessible \\

    public int getShaderIDFromShaderName(String shaderName) {
        if (!shaderName2ShaderID.containsKey(shaderName))
            request(shaderName);
        return shaderName2ShaderID.getInt(shaderName);
    }

    public ShaderHandle getShaderFromShaderID(int shaderID) {
        ShaderHandle shader = shaderID2Shader.get(shaderID);
        if (shader == null)
            throwException("ShaderHandle ID not found: " + shaderID);
        return shader;
    }

    public int getGPUHandleFromShaderID(int shaderID) {
        return shaderID2GPUHandle.get(shaderID);
    }
}