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
    private Object2IntOpenHashMap<String> textureArrayName2AtlasSize;

    // Base \\

    @Override
    protected void create() {
        this.internalLoadManager = create(InternalLoadManager.class);
        this.textureName2TileID = new Object2IntOpenHashMap<>();
        this.tileID2textureArrayID = new Int2IntArrayMap();
        this.tileID2textureArrayUV = new Int2ObjectOpenHashMap<>();
        this.textureArrayName2GPUHandle = new Object2IntOpenHashMap<>();
        this.textureArrayID2GPUHandle = new Int2IntArrayMap();
        this.textureArrayName2AtlasSize = new Object2IntOpenHashMap<>();

        // TODO: I wanna alter this to store object handles instead of seperate maps
        this.textureName2TileID.defaultReturnValue(-1);
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

    void addTextureTile(TextureTileInstance textureTileInstance, UVRect uvCoordinate, int arrayID) {
        textureName2TileID.put(textureTileInstance.getName(), textureTileInstance.getID());
        tileID2textureArrayID.put(textureTileInstance.getID(), arrayID);
        tileID2textureArrayUV.put(textureTileInstance.getID(), uvCoordinate);
    }

    void addTextureArray(TextureArrayInstance textureArrayInstance, int gpuHandle) {
        textureArrayName2GPUHandle.put(textureArrayInstance.getName(), gpuHandle);
        textureArrayID2GPUHandle.put(textureArrayInstance.getID(), gpuHandle);
        textureArrayName2AtlasSize.put(textureArrayInstance.getName(), textureArrayInstance.getAtlasSize());
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
        textureArrayName2AtlasSize.clear();
    }

    // Accessible \\

    public int getTileIDFromTextureName(String textureName) {

        int id = textureName2TileID.getInt(textureName);

        if (id == textureName2TileID.defaultReturnValue())
            throw new RuntimeException("[TextureManager] Texture not found: \"" + textureName + "\"");

        return id;
    }

    public int getTextureArrayIDFromTileID(int tileID) {
        return tileID2textureArrayID.get(tileID);
    }

    public UVRect getTextureArrayUVfromTileID(int tileID) {
        UVRect uvRect = tileID2textureArrayUV.get(tileID);
        if (uvRect == null)
            throw new RuntimeException("[TextureManager] No UVRect registered for tileID: " + tileID);
        return uvRect;
    }

    public int getGPUHandlefromTextureArrayName(String textureArrayName) {
        return textureArrayName2GPUHandle.getInt(textureArrayName);
    }

    public int getGPUHandleFromTextureArrayID(int textureArrayID) {
        return textureArrayID2GPUHandle.get(textureArrayID);
    }

    public int getAtlasSizeFromTextureArrayName(String name) {
        return textureArrayName2AtlasSize.getInt(name);
    }
}