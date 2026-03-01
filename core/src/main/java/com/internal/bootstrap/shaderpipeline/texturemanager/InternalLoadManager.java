package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.io.File;
import java.util.List;

import com.google.gson.JsonObject;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import com.internal.core.util.mathematics.vectors.Vector2;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Orchestrates the full texture-array loading pipeline during awake: discovers
 * atlas directories, delegates build work to subsystems, pushes results to the
 * GPU, then releases all intermediate image data to free heap. If a directory
 * contains a companion ubo.json, its declared UBO is seeded with alias layer
 * indices and uvPerBlock derived from the built atlas before GPU upload.
 */
class InternalLoadManager extends ManagerPackage {

    // Internal
    private TextureManager textureManager;
    private UBOManager uboManager;
    private AliasLibrarySystem aliasLibrarySystem;
    private InternalBuildSystem internalBuildSystem;
    private Int2ObjectOpenHashMap<TextureArrayData> arrayMap;
    private File root;

    // Base \\

    @Override
    protected void create() {
        this.aliasLibrarySystem = create(AliasLibrarySystem.class);
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.arrayMap = new Int2ObjectOpenHashMap<>();
        this.root = new File(EngineSetting.BLOCK_TEXTURE_PATH);
    }

    @Override
    protected void get() {
        this.textureManager = get(TextureManager.class);
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void release() {
        aliasLibrarySystem = release(AliasLibrarySystem.class);
        internalBuildSystem = release(InternalBuildSystem.class);
    }

    // File Navigation \\

    void loadTextureArrays() {
        aliasLibrarySystem.loadAliases();
        FileUtility.verifyDirectory(root, "[TextureManager] The root folder could not be verified");
        List<File> directories = FileUtility.collectAllSubdirectories(root);
        for (File directory : directories)
            createArrayFromDirectory(directory);
        pushTexturesToGPU();
    }

    // Categorisation \\

    private void createArrayFromDirectory(File directory) {
        List<File> imageFiles = FileUtility.collectFilesShallow(directory, EngineSetting.TEXTURE_FILE_EXTENSIONS);
        if (imageFiles.isEmpty())
            return;
        String arrayName = FileUtility.getPathWithFileNameWithoutExtension(root, directory);
        TextureArrayData textureArrayData = internalBuildSystem.buildTextureArray(imageFiles, directory, arrayName);
        arrayMap.put(textureArrayData.getID(), textureArrayData);
        seedCompanionUBO(directory, textureArrayData);
    }

    // UBO Companion \\

    private void seedCompanionUBO(File directory, TextureArrayData textureArrayData) {
        File companionFile = new File(directory, EngineSetting.TEXTURE_UBO_COMPANION_FILE);
        if (!companionFile.exists())
            return;
        JsonObject json = JsonUtility.loadJsonObject(companionFile);
        String uboName = JsonUtility.validateString(json, "ubo");
        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(uboName);
        if (ubo == null)
            throwException("[TextureManager] UBO not found for companion: \"" + uboName + "\"");
        if (json.has("aliases")) {
            JsonObject aliases = json.getAsJsonObject("aliases");
            for (String uniformName : aliases.keySet()) {
                String aliasName = aliases.get(uniformName).getAsString();
                int aliasId = aliasLibrarySystem.get(aliasName);
                if (aliasId == -1)
                    throwException("[TextureManager] Alias not found in companion JSON: \"" + aliasName + "\"");
                ubo.updateUniform(uniformName, aliasId);
            }
        }
        if (json.has("uvPerBlock")) {
            String uniformName = json.get("uvPerBlock").getAsString();
            float uvPerBlock = 1.0f / textureArrayData.getAtlasSize();
            ubo.updateUniform(uniformName, new Vector2(uvPerBlock, uvPerBlock));
        }
        ubo.push();
    }

    // GPU Upload \\

    private void pushTexturesToGPU() {
        for (TextureArrayData textureArrayData : arrayMap.values()) {
            pushTextureToGPU(textureArrayData);
            clearTextureImages(textureArrayData);
        }
    }

    private void pushTextureToGPU(TextureArrayData textureArrayData) {
        int gpuHandle = GLSLUtility.pushTextureArray(textureArrayData.getRawImageArray());
        int atlasSize = textureArrayData.getAtlasSize();
        float tileSize = 1.0f / atlasSize;
        Object2ObjectOpenHashMap<String, TextureTileData> tileCoordinateMap = textureArrayData.getTileCoordinateMap();
        for (Object2ObjectMap.Entry<String, TextureTileData> entry : tileCoordinateMap.object2ObjectEntrySet()) {
            TextureTileData tile = entry.getValue();
            float u0 = tile.getAtlasX() * tileSize;
            float v0 = tile.getAtlasY() * tileSize;
            textureManager.registerTile(tile, u0, v0, u0 + tileSize, v0 + tileSize, textureArrayData, gpuHandle);
        }
    }

    private void clearTextureImages(TextureArrayData textureArrayData) {
        for (TextureTileData tile : textureArrayData.getTileCoordinateMap().values())
            tile.clearImages();
        textureArrayData.clearAtlases();
    }
}