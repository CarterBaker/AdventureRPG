package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.Shaders.Shader;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ShaderManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> shaderName2ShaderID;
    private Int2ObjectOpenHashMap<Shader> shaderID2Shader;
    private Int2IntArrayMap shaderID2GPUHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());

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
    protected void freeMemory() {
        internalLoadManager = (InternalLoadManager) release(internalLoadManager);
    }

    // Shader Management \\

    private void compileShaders() {
        internalLoadManager.loadShaders();
    }

    public void addShader(Shader shader) {
        shaderName2ShaderID.put(shader.shaderName, shader.shaderID);
        shaderID2Shader.put(shader.shaderID, shader);
        shaderID2GPUHandle.put(shader.shaderID, shader.shaderHandle);
    }

    // Accessible \\

    public int getShaderIDFromShaderName(String shaderName) {
        return shaderName2ShaderID.getInt(shaderName);
    }

    public Shader getShaderFromShaderID(int shaderID) {
        return shaderID2Shader.get(shaderID);
    }

    public int getGPUHandleFromShaderID(int shaderID) {
        return shaderID2GPUHandle.get(shaderID);
    }
}
