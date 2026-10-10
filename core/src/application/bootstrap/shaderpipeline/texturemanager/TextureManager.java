package application.bootstrap.shaderpipeline.texturemanager;

import java.nio.ByteBuffer;

import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureData;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TextureManager extends ManagerPackage {

    /*
     * Owns every texture array and the tile lookup chain, name to ID to handle,
     * loading the parent array on a tile miss. Tile and array IDs are assigned
     * in registration order. Font atlases register through the same path as
     * single-layer arrays.
     */

    // Tile Palette
    private Object2IntOpenHashMap<String> textureName2TileID;
    private ObjectArrayList<TextureHandle> tileID2TextureHandle;

    // Array Palette
    private Object2IntOpenHashMap<String> arrayName2ArrayID;
    private ObjectArrayList<TextureHandle> arrayID2TextureHandle;

    // Base \\

    @Override
    protected void create() {

        this.textureName2TileID = RegistryUtility.createNameIndex();
        this.tileID2TextureHandle = RegistryUtility.createPalette();
        this.arrayName2ArrayID = RegistryUtility.createNameIndex();
        this.arrayID2TextureHandle = RegistryUtility.createPalette();

        create(TextureLoader.class);
    }

    @Override
    protected void dispose() {

        for (int arrayID = 0; arrayID < arrayID2TextureHandle.size(); arrayID++) {

            TextureHandle handle = arrayID2TextureHandle.get(arrayID);

            if (handle != null)
                TextureGLSLUtility.deleteTextureArray(handle.getGpuHandle());
        }

        RegistryUtility.clearPalette(textureName2TileID, tileID2TextureHandle);
        RegistryUtility.clearPalette(arrayName2ArrayID, arrayID2TextureHandle);
    }

    // Management \\

    void registerTile(
            TextureTileStruct tile,
            float u0, float v0, float u1, float v1,
            TextureArrayStruct array,
            int gpuHandle) {

        int tileID = RegistryUtility.registerID(
                textureName2TileID, tileID2TextureHandle, tile.getName(), EngineSetting.REGISTRY_INT_ID_COUNT);
        int arrayID = RegistryUtility.registerID(
                arrayName2ArrayID, arrayID2TextureHandle, array.getName(), EngineSetting.REGISTRY_INT_ID_COUNT);

        TextureData data = new TextureData(
                tile.getName(), tileID,
                arrayID, array.getName(),
                gpuHandle, array.getAtlasPixelSize(),
                tile.getTileWidth(), tile.getTileHeight(),
                u0, v0, u1, v1,
                tile.getAverageColor(),
                tile.getReveal());

        TextureHandle handle = create(TextureHandle.class);
        handle.constructor(data);

        tileID2TextureHandle.set(tileID, handle);

        if (arrayID2TextureHandle.get(arrayID) == null)
            arrayID2TextureHandle.set(arrayID, handle);
    }

    public void register(TextureArrayStruct arrayStruct, int gpuHandle) {

        float invAtlas = 1.0f / arrayStruct.getAtlasPixelSize();

        for (TextureTileStruct tile : arrayStruct.getTileCoordinateMap().values()) {
            float u0 = tile.getAtlasX() * invAtlas;
            float v0 = tile.getAtlasY() * invAtlas;
            float u1 = (tile.getAtlasX() + tile.getTileWidth()) * invAtlas;
            float v1 = (tile.getAtlasY() + tile.getTileHeight()) * invAtlas;
            registerTile(tile, u0, v0, u1, v1, arrayStruct, gpuHandle);
        }
    }

    // On-Demand \\

    public void request(String arrayName) {
        ((TextureLoader) internalLoader).request(arrayName);
    }

    // Accessible \\

    public boolean hasTexture(String textureName) {
        return RegistryUtility.getHandle(textureName2TileID, tileID2TextureHandle, textureName) != null;
    }

    public int getTileIDFromTextureName(String textureName) {

        if (!hasTexture(textureName)) {
            String arrayName = textureName.contains("/")
                    ? textureName.substring(0, textureName.lastIndexOf('/'))
                    : textureName;
            request(arrayName);
        }

        if (!hasTexture(textureName))
            throwException("Texture not found after load: \"" + textureName + "\"");

        return textureName2TileID.getInt(textureName);
    }

    public TextureHandle getTextureHandleFromTileID(int tileID) {

        TextureHandle handle = RegistryUtility.getHandle(tileID2TextureHandle, tileID);

        if (handle == null)
            throwException("No handle registered for tile ID: " + tileID);

        return handle;
    }

    public TextureHandle getTextureHandleFromTextureName(String textureName) {
        return getTextureHandleFromTileID(getTileIDFromTextureName(textureName));
    }

    public boolean hasArray(String arrayName) {
        return RegistryUtility.getHandle(arrayName2ArrayID, arrayID2TextureHandle, arrayName) != null;
    }

    public int getArrayIDFromArrayName(String arrayName) {

        if (!hasArray(arrayName))
            request(arrayName);

        if (!hasArray(arrayName))
            throwException("Array not found after load: \"" + arrayName + "\"");

        return arrayName2ArrayID.getInt(arrayName);
    }

    public TextureHandle getTextureHandleFromArrayID(int arrayID) {

        TextureHandle handle = RegistryUtility.getHandle(arrayID2TextureHandle, arrayID);

        if (handle == null)
            throwException("No handle registered for array ID: " + arrayID);

        return handle;
    }

    public TextureHandle getTextureHandleFromArrayName(String arrayName) {
        return getTextureHandleFromArrayID(getArrayIDFromArrayName(arrayName));
    }

    public ObjectArrayList<String> getTextureNamesInArray(String arrayName) {

        int arrayID = getArrayIDFromArrayName(arrayName);
        ObjectArrayList<String> textureNames = new ObjectArrayList<>();

        for (int tileID = 0; tileID < tileID2TextureHandle.size(); tileID++) {

            TextureHandle handle = tileID2TextureHandle.get(tileID);

            if (handle != null && handle.getArrayID() == arrayID)
                textureNames.add(handle.getTileName());
        }

        textureNames.sort(String.CASE_INSENSITIVE_ORDER);
        return textureNames;
    }

    public int createFloatTexture2D(float[] pixels, int width, int height, int wrapMode, int filterMode) {
        return TextureGLSLUtility.createFloatTexture2D(pixels, width, height, wrapMode, filterMode);
    }

    public int createTexture2D(int width, int height, int wrapMode, int filterMode) {
        return TextureGLSLUtility.createTexture2D(width, height, wrapMode, filterMode);
    }

    public void updateTexture2D(int handle, int x, int y, int width, int height, ByteBuffer pixels) {
        TextureGLSLUtility.updateTexture2D(handle, x, y, width, height, pixels);
    }

    public void deleteTexture2D(int handle) {
        TextureGLSLUtility.deleteTexture2D(handle);
    }
}