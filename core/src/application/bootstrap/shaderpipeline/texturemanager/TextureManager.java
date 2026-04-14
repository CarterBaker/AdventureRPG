package application.bootstrap.shaderpipeline.texturemanager;

import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureData;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class TextureManager extends ManagerPackage {

    /*
     * Owns all texture array GPU handles and the full tile lookup chain.
     * Two palettes — tile and array — both following the standard name→ID→handle
     * pattern. On tile miss, the parent array name is extracted and an on-demand
     * load is triggered. GPU resources are released on dispose.
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

        create(InternalLoader.class);
    }

    @Override
    protected void dispose() {

        for (TextureHandle handle : arrayID2TextureHandle.values())
            GLSLUtility.deleteTextureArray(handle.getGpuHandle());

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

    // On-Demand \\

    public void request(String arrayName) {
        ((InternalLoader) internalLoader).request(arrayName);
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
}