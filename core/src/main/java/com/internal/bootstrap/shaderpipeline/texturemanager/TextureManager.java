package com.internal.bootstrap.shaderpipeline.texturemanager;

import com.internal.core.engine.ManagerPackage;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class TextureManager extends ManagerPackage {

    // Internal
    private InternalLoadManager internalLoadManager;

    // Retrieval Mapping
    private Object2IntOpenHashMap<String> textureName2TileID;
    private Int2IntArrayMap tileID2textureArrayID;
    private Int2ObjectOpenHashMap<UVRect> tileID2textureArrayUV;
    private Object2IntOpenHashMap<String> textureArrayName2GPUHandle;
    private Int2IntArrayMap textureArrayID2GPUHandle;

    // Base \\

    @Override
    protected void create() {

        // Root
        this.internalLoadManager = create(InternalLoadManager.class);

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
    protected void release() {
        internalLoadManager = release(InternalLoadManager.class);
    }

    @Override
    public void dispose() {
        disposeAllGPUResources();
    }

    // Texture Management \\

    private void compileTextureArrays() {
        internalLoadManager.loadTextureArrays();
    }

    void addTextureTile(TextureTileInstance textureTileInstance, UVRect uvCoordinate) {
        textureName2TileID.put(textureTileInstance.getName(), textureTileInstance.getID());
        tileID2textureArrayID.put(textureTileInstance.getID(), textureTileInstance.getID());
        tileID2textureArrayUV.put(textureTileInstance.getID(), uvCoordinate);
    }

    void addTextureArray(TextureArrayInstance textureArrayInstance, int gpuHandle) {
        textureArrayName2GPUHandle.put(textureArrayInstance.getName(), gpuHandle);
        textureArrayID2GPUHandle.put(textureArrayInstance.getID(), gpuHandle);
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

    public UVRect getTextureArrayUVfromTileID(int tileID) {
        return tileID2textureArrayUV.get(tileID);
    }

    public int getGPUHandlefromTextureArrayName(String textureArrayName) {
        return textureArrayName2GPUHandle.getInt(textureArrayName);
    }

    public int getGPUHandleFromTextureArrayID(int textureArrayID) {
        return textureArrayID2GPUHandle.get(textureArrayID);
    }
}