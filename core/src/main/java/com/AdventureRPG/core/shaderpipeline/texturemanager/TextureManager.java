package com.AdventureRPG.core.shaderpipeline.texturemanager;

import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.shaderpipeline.util.UVCoordinate;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class TextureManager extends ManagerFrame {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> textureName2TileID;
    private Int2IntArrayMap tileID2textureArrayID;
    private Int2ObjectOpenHashMap<UVCoordinate> tileID2textureArrayUV;
    private Object2IntOpenHashMap<String> textureArrayName2GPUHandle;
    private Int2IntArrayMap textureArrayID2GPUHandle;

    // Base \\

    @Override
    protected void create() {

        // Root
        this.internalLoadManager = (InternalLoadManager) register(new InternalLoadManager());

        // Retrieval Mapping
        this.textureName2TileID = new Object2IntOpenHashMap<>();
        this.tileID2textureArrayID = new Int2IntArrayMap();
        this.tileID2textureArrayUV = new Int2ObjectOpenHashMap<>();
        this.textureArrayName2GPUHandle = new Object2IntOpenHashMap<>();
        this.textureArrayID2GPUHandle = new Int2IntArrayMap();
    }

    @Override
    protected void awake() {
        compileTextureArrays();
    }

    @Override
    protected void freeMemory() {
        internalLoadManager = (InternalLoadManager) release(internalLoadManager);
    }

    @Override
    public void dispose() {
        disposeAllGPUResources();
    }

    // Texture Management \\

    private void compileTextureArrays() {
        internalLoadManager.loadTextureArrays();
    }

    void addTextureTile(TextureTileInstance textureTileInstance, UVCoordinate uvCoordinate) {
        textureName2TileID.put(textureTileInstance.name, textureTileInstance.id);
        tileID2textureArrayID.put(textureTileInstance.id, textureTileInstance.id);
        tileID2textureArrayUV.put(textureTileInstance.id, uvCoordinate);
    }

    void addTextureArray(TextureArrayInstance textureArrayInstance, int gpuHandle) {
        textureArrayName2GPUHandle.put(textureArrayInstance.name, gpuHandle);
        textureArrayID2GPUHandle.put(textureArrayInstance.id, gpuHandle);
    }

    // Disposal \\

    private void disposeAllGPUResources() {

        for (int gpuHandle : textureArrayID2GPUHandle.values())
            GLSLUtility.deleteTextureArray(gpuHandle);

        textureName2TileID.clear();
        tileID2textureArrayID.clear();
        tileID2textureArrayUV.clear();
        textureArrayName2GPUHandle.clear();
        textureArrayID2GPUHandle.clear();
    }

    // Accessible \\

    public int getTileIDFromTextureName(String textureName) {
        return textureName2TileID.getInt(textureName);
    }

    public int getTextureArrayIDFromTileID(int tileID) {
        return tileID2textureArrayID.get(tileID);
    }

    public UVCoordinate getTextureArrayUVfromTileID(int tileID) {
        return tileID2textureArrayUV.get(tileID);
    }

    public int getGPUHandlefromTextureArrayName(String textureArrayName) {
        return textureArrayName2GPUHandle.getInt(textureArrayName);
    }

    public int getGPUHandleFromTextureArrayID(int textureArrayID) {
        return textureArrayID2GPUHandle.get(textureArrayID);
    }
}