package com.AdventureRPG.Core.RenderPipeline.ShaderManager;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.LayoutBlock.LayoutBlock;
import com.AdventureRPG.Core.RenderPipeline.Shaders.Shader;
import com.AdventureRPG.Core.Util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class ShaderManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> layoutName2LayoutID;
    private Int2ObjectOpenHashMap<LayoutBlock> layoutID2Layout;
    private Int2IntArrayMap layoutID2GPUHandle;

    private Object2IntOpenHashMap<String> shaderName2ShaderID;
    private Int2ObjectOpenHashMap<Shader> shaderID2Shader;
    private Int2IntArrayMap shaderID2GPUHandle;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());

        // Retrieval Mapping
        this.layoutName2LayoutID = new Object2IntOpenHashMap<>();
        this.layoutID2Layout = new Int2ObjectOpenHashMap<>();
        this.layoutID2GPUHandle = new Int2IntArrayMap();

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

    boolean hasLayout(String layoutName) {
        return layoutName2LayoutID.containsKey(layoutName);
    }

    LayoutBlock getLayoutFromLayoutName(String layoutName) {
        int layoutID = layoutName2LayoutID.getInt(layoutName);
        return layoutID2Layout.get(layoutID);
    }

    void addLayout(LayoutBlock layoutBlock) {

        if (layoutName2LayoutID.containsKey(layoutBlock.layoutName))
            throw new GraphicException.ShaderProgramException(
                    "Uniform layout block: " + layoutBlock.layoutName + ", already exists");

        layoutName2LayoutID.put(layoutBlock.layoutName, layoutBlock.layoutID);
        layoutID2Layout.put(layoutBlock.layoutID, layoutBlock);
        layoutID2GPUHandle.put(layoutBlock.layoutID, layoutBlock.layoutHandle);
    }

    void addShader(Shader shader) {
        shaderName2ShaderID.put(shader.shaderName, shader.shaderID);
        shaderID2Shader.put(shader.shaderID, shader);
        shaderID2GPUHandle.put(shader.shaderID, shader.shaderHandle);
    }

    // Disposal \\

    private void disposeAllGPUResources() {

        // Dispose all shaders
        for (Shader shader : shaderID2Shader.values()) {
            GLSLUtility.deleteShaderProgram(shader.shaderHandle);
        }

        // Dispose all layouts
        for (LayoutBlock layout : layoutID2Layout.values()) {
            GLSLUtility.deleteUniformBuffer(layout.layoutHandle);
        }

        // Clear all mappings
        shaderName2ShaderID.clear();
        shaderID2Shader.clear();
        shaderID2GPUHandle.clear();

        layoutName2LayoutID.clear();
        layoutID2Layout.clear();
        layoutID2GPUHandle.clear();
    }

    // Accessible \\

    public int getLayoutIDFromLayoutName(String layoutName) {
        return layoutName2LayoutID.getInt(layoutName);
    }

    public LayoutBlock getLayoutFromLayoutID(int layoutID) {
        return layoutID2Layout.get(layoutID);
    }

    public int getGPUHandleFromLayoutID(int layoutID) {
        return layoutID2GPUHandle.get(layoutID);
    }

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
