package com.AdventureRPG.Core.RenderPipeline.TextureManager;

import java.util.Map;

import com.AdventureRPG.Core.Bootstrap.EngineConstant;
import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.Util.GPUCall;
import com.AdventureRPG.Core.RenderPipeline.Util.UVCoordinate;

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

        AliasInstance[] aliases = internalLoadManager.getAllAliases();

        for (int i = 0; i < textureArrays.size(); i++)
            pushTextureToGPU(aliases, textureArrays.get(i));
    }

    private void pushTextureToGPU(AliasInstance[] aliases, TextureArrayInstance textureArray) {

        // First and foremost push the array to the gpu and return the handle
        int gpuHandle = GPUCall.pushTextureArray(textureArray.getRawImageArray());

        // Next step retrieve the tile data
        Map<String, TextureTileInstance> tileCoordinateMap = textureArray.getTileCoordinateMap();

        for (Map.Entry<String, TextureTileInstance> tile : tileCoordinateMap.entrySet()) {

            UVCoordinate uvCoordinate = computeUV(
                    tile.getValue().getAtlasX(),
                    tile.getValue().getAtlasY(),
                    textureArray.atlasSize);

            textureName2TileID.put(tile.getKey(), tile.getValue().id);
            tileID2textureArrayID.put(tile.getValue().id, textureArray.id);
            tileID2textureArrayUV.put(tile.getValue().id, uvCoordinate);
        }

        textureArrayID2GPUHandle.put(textureArray.id, gpuHandle);
    }

    private UVCoordinate computeUV(int atlasX, int atlasY, int atlasSize) {

        float u = (atlasX * EngineConstant.BLOCK_TEXTURE_SIZE) / (float) atlasSize;
        float v = (atlasY * EngineConstant.BLOCK_TEXTURE_SIZE) / (float) atlasSize;

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

    public int getGPUHandleFromTextureArrayID(int textureArrayID) {
        return textureArrayID2GPUHandle.get(textureArrayID);
    }
}