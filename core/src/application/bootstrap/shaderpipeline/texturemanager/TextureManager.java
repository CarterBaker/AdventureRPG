package application.bootstrap.shaderpipeline.texturemanager;

import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureData;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TextureManager extends ManagerPackage {

    /*
     * Owns every texture array and the tile lookup chain, name to ID to handle,
     * loading the parent array on a tile miss. Font atlases register through
     * the same path as single-layer arrays.
     */

    // Tile Palette
    private Object2IntOpenHashMap<String> textureName2TileID;
    private Int2ObjectOpenHashMap<TextureHandle> tileID2TextureHandle;

    // Array Palette
    private Object2IntOpenHashMap<String> arrayName2ArrayID;
    private Int2ObjectOpenHashMap<TextureHandle> arrayID2TextureHandle;

    // Base \\

    @Override
    protected void create() {

        this.textureName2TileID = new Object2IntOpenHashMap<>();
        this.tileID2TextureHandle = new Int2ObjectOpenHashMap<>();
        this.arrayName2ArrayID = new Object2IntOpenHashMap<>();
        this.arrayID2TextureHandle = new Int2ObjectOpenHashMap<>();

        create(TextureLoader.class);
    }

    @Override
    protected void dispose() {

        for (TextureHandle handle : arrayID2TextureHandle.values())
            TextureGLSLUtility.deleteTextureArray(handle.getGpuHandle());

        textureName2TileID.clear();
        tileID2TextureHandle.clear();
        arrayName2ArrayID.clear();
        arrayID2TextureHandle.clear();
    }

    // Management \\

    void registerTile(
            TextureTileStruct tile,
            float u0, float v0, float u1, float v1,
            TextureArrayStruct array,
            int gpuHandle) {

        int tileID = RegistryUtility.toIntID(tile.getName());
        int arrayID = RegistryUtility.toIntID(array.getName());

        TextureData data = new TextureData(
                tile.getName(), tileID,
                arrayID, array.getName(),
                gpuHandle, array.getAtlasPixelSize(),
                tile.getTileWidth(), tile.getTileHeight(),
                u0, v0, u1, v1);

        TextureHandle handle = create(TextureHandle.class);
        handle.constructor(data);

        textureName2TileID.put(tile.getName(), tileID);
        tileID2TextureHandle.put(tileID, handle);

        if (!arrayName2ArrayID.containsKey(array.getName())) {
            arrayName2ArrayID.put(array.getName(), arrayID);
            arrayID2TextureHandle.put(arrayID, handle);
        }
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
        return textureName2TileID.containsKey(textureName);
    }

    public int getTileIDFromTextureName(String textureName) {

        if (!textureName2TileID.containsKey(textureName)) {
            String arrayName = textureName.contains("/")
                    ? textureName.substring(0, textureName.lastIndexOf('/'))
                    : textureName;
            request(arrayName);
        }

        if (!textureName2TileID.containsKey(textureName))
            throwException("Texture not found after load: \"" + textureName + "\"");

        return textureName2TileID.getInt(textureName);
    }

    public TextureHandle getTextureHandleFromTileID(int tileID) {

        TextureHandle handle = tileID2TextureHandle.get(tileID);

        if (handle == null)
            throwException("No handle registered for tile ID: " + tileID);

        return handle;
    }

    public TextureHandle getTextureHandleFromTextureName(String textureName) {
        return getTextureHandleFromTileID(getTileIDFromTextureName(textureName));
    }

    public boolean hasArray(String arrayName) {
        return arrayName2ArrayID.containsKey(arrayName);
    }

    public int getArrayIDFromArrayName(String arrayName) {

        if (!arrayName2ArrayID.containsKey(arrayName))
            request(arrayName);

        if (!arrayName2ArrayID.containsKey(arrayName))
            throwException("Array not found after load: \"" + arrayName + "\"");

        return arrayName2ArrayID.getInt(arrayName);
    }

    public TextureHandle getTextureHandleFromArrayID(int arrayID) {

        TextureHandle handle = arrayID2TextureHandle.get(arrayID);

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

        for (TextureHandle handle : tileID2TextureHandle.values())
            if (handle.getArrayID() == arrayID)
                textureNames.add(handle.getTileName());

        textureNames.sort(String.CASE_INSENSITIVE_ORDER);
        return textureNames;
    }

    public int createFloatTexture2D(float[] pixels, int width, int height, int wrapMode, int filterMode) {
        return TextureGLSLUtility.createFloatTexture2D(pixels, width, height, wrapMode, filterMode);
    }

    public void deleteTexture2D(int handle) {
        TextureGLSLUtility.deleteTexture2D(handle);
    }
}