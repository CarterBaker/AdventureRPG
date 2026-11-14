package application.bootstrap.shaderpipeline.texturemanager;

import java.io.File;
import java.nio.ByteBuffer;

import application.bootstrap.shaderpipeline.texture.TextureArrayStruct;
import application.bootstrap.shaderpipeline.texture.TextureBakeStruct;
import application.bootstrap.shaderpipeline.texture.TextureData;
import application.bootstrap.shaderpipeline.texture.TextureHandle;
import application.bootstrap.shaderpipeline.texture.TextureTileStruct;
import engine.root.EngineSetting;
import engine.root.ManagerPackage;
import engine.util.registry.RegistryUtility;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class TextureManager extends ManagerPackage {

    /*
     * Owns every texture tile and the tile lookup chain, name to ID to handle,
     * and the streaming that keeps each tile's tiers resident on the GPU.
     * Every baked tile's definition loads at bootstrap, so a tile ID is known
     * the moment it is asked for; its tiers stream into fixed pools as the
     * texture users that reference it come near, and a shader reads whatever
     * is resident through the tile lookup, the fallback pool when nothing is.
     * Tile and array IDs are assigned in registration order. Font atlases
     * register through the same palette as single-layer arrays of their own.
     */

    // Internal
    private TexturePoolBranch texturePoolBranch;
    private TextureStreamBranch textureStreamBranch;
    private AliasLibraryStruct aliasLibrary;

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

        this.texturePoolBranch = create(TexturePoolBranch.class);
        this.textureStreamBranch = create(TextureStreamBranch.class);
        create(TextureLoader.class);
    }

    @Override
    protected void awake() {
        internalLoader.requestAll();
    }

    // Bootstrap fills the base tier of every tile before the first frame
    @Override
    protected void start() {
        textureStreamBranch.completeStreaming();
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

    void setAliasLibrary(AliasLibraryStruct aliasLibrary) {
        this.aliasLibrary = aliasLibrary;
        texturePoolBranch.stageExtra(0, TextureBakeUtility.bakeDefaultExtra(aliasLibrary,
                new TextureCompressionScratchStruct()));
        texturePoolBranch.flush();
    }

    int registerTileName(String tileName) {
        return RegistryUtility.registerID(
                textureName2TileID, tileID2TextureHandle, tileName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    int registerArrayName(String arrayName) {
        return RegistryUtility.registerID(
                arrayName2ArrayID, arrayID2TextureHandle, arrayName, EngineSetting.REGISTRY_INT_ID_COUNT);
    }

    // A baked tile's handle, its tier files handed to streaming and its fallback and extra maps to the pools
    void addStreamedTile(
            TextureHandle handle,
            File[] tierFiles,
            int tierCount,
            byte[][] fallbackMaps,
            byte[] extraMap) {

        int tileID = handle.getTileID();
        addTextureHandle(handle);
        textureStreamBranch.addTile(tileID, tierCount, tierFiles);
        texturePoolBranch.stageFallback(texturePoolBranch.assignFallbackLayer(tileID), fallbackMaps);

        if (extraMap != null)
            texturePoolBranch.stageExtra(texturePoolBranch.assignExtraLayer(tileID), extraMap);
    }

    void flushUploads() {
        texturePoolBranch.flush();
        texturePoolBranch.pushLookup();
    }

    private void addTextureHandle(TextureHandle handle) {

        tileID2TextureHandle.set(handle.getTileID(), handle);

        if (arrayID2TextureHandle.get(handle.getArrayID()) == null)
            arrayID2TextureHandle.set(handle.getArrayID(), handle);
    }

    // Registers every glyph of a font atlas, each with its region of the atlas the font uploaded itself
    public void register(TextureArrayStruct arrayStruct, int gpuHandle) {

        float invAtlas = 1.0f / arrayStruct.getAtlasPixelSize();
        int arrayID = registerArrayName(arrayStruct.getName());

        for (TextureTileStruct tile : arrayStruct.getTileCoordinateMap().values()) {

            TextureData data = new TextureData(
                    tile.getName(), registerTileName(tile.getName()),
                    arrayID, arrayStruct.getName(),
                    gpuHandle,
                    tile.getTileWidth(), tile.getTileHeight(),
                    tile.getAtlasX() * invAtlas,
                    tile.getAtlasY() * invAtlas,
                    (tile.getAtlasX() + tile.getTileWidth()) * invAtlas,
                    (tile.getAtlasY() + tile.getTileHeight()) * invAtlas,
                    EngineSetting.PACKED_COLOR_WHITE,
                    null);

            TextureHandle handle = create(TextureHandle.class);
            handle.constructor(data);
            addTextureHandle(handle);
        }
    }

    // On-Demand \\

    public void request(String arrayName) {
        ((TextureLoader) internalLoader).request(arrayName);
    }

    // Streaming \\

    // A texture user in the world, referencing its tiles for as long as it occupies the given bounds in blocks
    public int registerTextureUser(IntArrayList tileIDs, float minX, float minZ, float maxX, float maxZ) {
        return textureStreamBranch.registerUser(tileIDs, false, minX, minZ, maxX, maxZ);
    }

    // A texture user on screen, such as an open inventory, which wants every tier its tiles have
    public int registerScreenTextureUser(IntArrayList tileIDs) {
        return textureStreamBranch.registerUser(tileIDs, true, 0f, 0f, 0f, 0f);
    }

    public void setTextureUserTiles(int user, IntArrayList tileIDs) {
        textureStreamBranch.setUserTiles(user, tileIDs);
    }

    public void setTextureUserBounds(int user, float minX, float minZ, float maxX, float maxZ) {
        textureStreamBranch.setUserBounds(user, minX, minZ, maxX, maxZ);
    }

    public void removeTextureUser(int user) {
        textureStreamBranch.removeUser(user);
    }

    // Fills every tier the texture users want at once, before play begins
    public void completeTextureStreaming() {
        textureStreamBranch.completeStreaming();
    }

    public void applyTextureQuality(int quality) {
        textureStreamBranch.applyQuality(quality);
    }

    public void applyTextureAnisotropy(int anisotropy) {
        texturePoolBranch.applyAnisotropy(anisotropy);
    }

    public float getTextureAnisotropy() {
        return texturePoolBranch.getAnisotropy();
    }

    // The GL texture a tile sampler uniform reads, GL_HANDLE_NONE for any uniform that is not one
    public int getTileSamplerHandle(String uniformName) {
        return texturePoolBranch.getSamplerHandle(uniformName);
    }

    // Puts a tile baked again, as the editor paints it, into every pool it is resident in
    public void replaceTileBake(int tileID, TextureBakeStruct bake) {
        textureStreamBranch.replaceBake(tileID, bake);
    }

    // Accessible \\

    public AliasLibraryStruct getAliasLibrary() {
        return aliasLibrary;
    }

    public boolean hasTexture(String textureName) {
        return RegistryUtility.getHandle(textureName2TileID, tileID2TextureHandle, textureName) != null;
    }

    public int getTileIDFromTextureName(String textureName) {

        if (!hasTexture(textureName)) {
            String arrayName = textureName.contains(EngineSetting.TEXTURE_TILE_NAME_SEPARATOR)
                    ? textureName.substring(0, textureName.lastIndexOf(EngineSetting.TEXTURE_TILE_NAME_SEPARATOR))
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
