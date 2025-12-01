package com.AdventureRPG.Core.RenderPipeline.TextureManager;

import com.AdventureRPG.Core.Bootstrap.EngineSetting;
import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.Util.UVCoordinate;

import it.unimi.dsi.fastutil.ints.Int2IntArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

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

    // Array Management \\

    private void compileTextureArrays() {
        pushTexturesToGPU(internalLoadManager.loadTextureArrays());
    }

    private void pushTexturesToGPU(Int2ObjectOpenHashMap<TextureArrayInstance> textureArrays) {

        // Alias files are loaded from json and define albedo, normal ect.
        AliasInstance[] aliases = internalLoadManager.getAllAliases();

        for (int i = 0; i < textureArrays.size(); i++)
            pushTextureToGPU(aliases, textureArrays.get(i));
    }

    private void pushTextureToGPU(AliasInstance[] aliases, TextureArrayInstance textureArray) {

        // First and foremost push the array to the gpu and return the handle
        int gpuHandle = GLSLUtility.pushTextureArray(textureArray.getRawImageArray());

        // Next step retrieve the tile data
        Object2ObjectOpenHashMap<String, TextureTileInstance> tileCoordinateMap = textureArray.getTileCoordinateMap();

        // For each tile int he texture array map the appropriate data
        for (Object2ObjectMap.Entry<String, TextureTileInstance> tile : tileCoordinateMap.object2ObjectEntrySet()) {

            UVCoordinate uvCoordinate = computeUV(
                    tile.getValue().getAtlasX(),
                    tile.getValue().getAtlasY(),
                    textureArray.atlasSize);

            textureName2TileID.put(tile.getKey(), tile.getValue().id);
            tileID2textureArrayID.put(tile.getValue().id, textureArray.id);
            tileID2textureArrayUV.put(tile.getValue().id, uvCoordinate);
        }

        // For each array map the appropriate data
        textureArrayName2GPUHandle.put(textureArray.name, gpuHandle);
        textureArrayID2GPUHandle.put(textureArray.id, gpuHandle);
    }

    // Use global image size settigns to calculate UVs for any given texture using
    // known x and y coordinate values stored internally per tile
    private UVCoordinate computeUV(int atlasX, int atlasY, int atlasSize) {

        float u = (atlasX * EngineSetting.BLOCK_TEXTURE_SIZE) / (float) atlasSize;
        float v = (atlasY * EngineSetting.BLOCK_TEXTURE_SIZE) / (float) atlasSize;

        return new UVCoordinate(u, v);
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