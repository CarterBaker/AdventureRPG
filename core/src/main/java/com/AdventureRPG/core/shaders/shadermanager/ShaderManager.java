package com.AdventureRPG.core.shaders.shadermanager;

import com.AdventureRPG.core.engine.ManagerFrame;
import com.AdventureRPG.core.shaders.shaders.Shader;
import com.AdventureRPG.core.shaders.ubomanager.UBOData;
import com.AdventureRPG.core.shaders.ubomanager.UBOHandle;
import com.AdventureRPG.core.util.Exceptions.GraphicException;

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

    @Override
    protected void dispose() {
        disposeAllGPUResources();
    }

    // Shader Management \\

    private void compileShaders() {
        internalLoadManager.loadShaders();
    }

    void addShader(Shader shader) {
        shaderName2ShaderID.put(shader.shaderName, shader.shaderID);
        shaderID2Shader.put(shader.shaderID, shader);
        shaderID2GPUHandle.put(shader.shaderID, shader.shaderHandle);
    }

    public void bindShaderToUBO(
            Shader shader,
            UBOHandle ubo) {
        GLSLUtility.bindUniformBlock(
                shader.shaderHandle,
                ubo.bufferName,
                ubo.bindingPoint);
    }

    // Disposal \\

    private void disposeAllGPUResources() {

        // Dispose all shaders
        for (Shader shader : shaderID2Shader.values())
            GLSLUtility.deleteShaderProgram(shader.shaderHandle);

        // Clear all mappings
        shaderName2ShaderID.clear();
        shaderID2Shader.clear();
        shaderID2GPUHandle.clear();
    }

    // Accessible \\

    public int getShaderIDFromShaderName(String shaderName) {

        if (!shaderName2ShaderID.containsKey(shaderName))
            throw new GraphicException.ShaderProgramException(
                    "Shader not found: " + shaderName);

        return shaderName2ShaderID.getInt(shaderName);
    }

    public Shader getShaderFromShaderID(int shaderID) {

        Shader shader = shaderID2Shader.get(shaderID);

        if (shader == null)
            throw new GraphicException.ShaderProgramException(
                    "Shader ID not found: " + shaderID);

        return shader;
    }

    public int getGPUHandleFromShaderID(int shaderID) {
        return shaderID2GPUHandle.get(shaderID);
    }
}
