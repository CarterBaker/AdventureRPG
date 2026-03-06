package com.internal.bootstrap.shaderpipeline.texturemanager;

import com.internal.core.engine.ManagerPackage;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Owns all texture array GPU handles and the full tile lookup chain.
 * Provides runtime retrieval of TextureHandle by texture name, tile ID,
 * array name, or array ID. On accessor miss, triggers an immediate
 * synchronous load through the active InternalLoadManager.
 * GPU resources are released on dispose.
 */
public class TextureManager extends ManagerPackage {

    // Retrieval Mapping
    private Object2ObjectOpenHashMap<String, TextureHandle> textureName2Handle;
    private Int2ObjectOpenHashMap<TextureHandle> tileID2Handle;
    private Object2ObjectOpenHashMap<String, TextureHandle> arrayName2Handle;
    private Int2ObjectOpenHashMap<TextureHandle> arrayID2Handle;

    // Base \\

    @Override
    protected void create() {
        create(InternalLoadManager.class);
        this.textureName2Handle = new Object2ObjectOpenHashMap<>();
        this.tileID2Handle = new Int2ObjectOpenHashMap<>();
        this.arrayName2Handle = new Object2ObjectOpenHashMap<>();
        this.arrayID2Handle = new Int2ObjectOpenHashMap<>();
    }

    @Override
    protected void dispose() {
        disposeAllGPUResources();
    }

    // On-Demand Loading \\

    public void request(String arrayName) {
        ((InternalLoadManager) internalLoader).request(arrayName);
    }

    // Registration \\

    void registerTile(TextureTileData tile, float u0, float v0, float u1, float v1,
            TextureArrayData array, int gpuHandle) {
        UVHandle uvHandle = create(UVHandle.class);
        uvHandle.constructor(u0, v0, u1, v1);
        TextureHandle handle = create(TextureHandle.class);
        handle.constructor(tile.getID(), array.getID(), gpuHandle, array.getAtlasSize(), uvHandle);
        textureName2Handle.put(tile.getName(), handle);
        tileID2Handle.put(tile.getID(), handle);
        if (!arrayID2Handle.containsKey(array.getID())) {
            arrayName2Handle.put(array.getName(), handle);
            arrayID2Handle.put(array.getID(), handle);
        }
    }

    // Disposal \\

    private void disposeAllGPUResources() {
        for (TextureHandle handle : arrayID2Handle.values())
            GLSLUtility.deleteTextureArray(handle.getGPUHandle());
        textureName2Handle.clear();
        tileID2Handle.clear();
        arrayName2Handle.clear();
        arrayID2Handle.clear();
    }

    // Accessible \\

    public TextureHandle getHandleFromTextureName(String textureName) {
        TextureHandle handle = textureName2Handle.get(textureName);
        if (handle == null) {
            String arrayName = textureName.contains("/")
                    ? textureName.substring(0, textureName.lastIndexOf('/'))
                    : textureName;
            request(arrayName);
            handle = textureName2Handle.get(textureName);
        }
        if (handle == null)
            throwException("[TextureManager] Texture not found after load: \"" + textureName + "\"");
        return handle;
    }

    public TextureHandle getHandleFromTileID(int tileID) {
        TextureHandle handle = tileID2Handle.get(tileID);
        if (handle == null)
            throwException("[TextureManager] No handle registered for tileID: " + tileID);
        return handle;
    }

    public TextureHandle getArrayHandleFromArrayName(String arrayName) {
        TextureHandle handle = arrayName2Handle.get(arrayName);
        if (handle == null) {
            request(arrayName);
            handle = arrayName2Handle.get(arrayName);
        }
        if (handle == null)
            throwException("[TextureManager] Array not found after load: \"" + arrayName + "\"");
        return handle;
    }

    public TextureHandle getArrayHandleFromArrayID(int arrayID) {
        TextureHandle handle = arrayID2Handle.get(arrayID);
        if (handle == null)
            throwException("[TextureManager] No array registered for arrayID: " + arrayID);
        return handle;
    }
}