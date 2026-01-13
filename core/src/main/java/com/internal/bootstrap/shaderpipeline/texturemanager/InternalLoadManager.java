package com.internal.bootstrap.shaderpipeline.texturemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoadManager extends ManagerPackage {

    // Internal
    private TextureManager textureManager;
    private AliasLibrarySystem aliasLibrarySystem;
    private InternalBuildSystem internalBuildSystem;
    private Int2ObjectOpenHashMap<TextureArrayInstance> arrayMap;
    private File root;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.aliasLibrarySystem = create(AliasLibrarySystem.class);
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.arrayMap = new Int2ObjectOpenHashMap<TextureArrayInstance>();
        this.root = new File(EngineSetting.BLOCK_TEXTURE_PATH);
    }

    @Override
    protected void get() {

        // Internal
        this.textureManager = get(TextureManager.class);
    }

    @Override
    protected void release() {

        // Internal
        aliasLibrarySystem = release(AliasLibrarySystem.class);
        internalBuildSystem = release(InternalBuildSystem.class);
    }

    // File Navigation \\

    // The main method that delegates logic to helpers to return texture arrays
    void loadTextureArrays() {

        aliasLibrarySystem.loadAliases();

        FileUtility.verifyDirectory(root, "[TextureManager] The root folder could not be verified");

        Path rootPath = root.toPath();

        try (var stream = Files.walk(rootPath)) {
            stream
                    .filter(Files::isDirectory)
                    .filter(folder -> !folder.equals(rootPath)) // exclude root itself
                    .forEach(folder -> createArrayFromFolder(folder.toFile()));
        }

        catch (IOException e) {
            throwException(
                    "There was an issue loading one or more files from the source directory",
                    e);
        }

        pushTexturesToGPU(arrayMap);
    }

    // Categorization logic \\

    private void createArrayFromFolder(File directory) {

        List<File> imageFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files == null)
            return;

        for (File file : files)
            if (file.isFile() && EngineSetting.TEXTURE_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
                imageFiles.add(file);

        if (imageFiles.isEmpty())
            return;

        createTextureArray(internalBuildSystem.buildTextureArray(imageFiles, directory));
    }

    private void createTextureArray(TextureArrayInstance textureArray) {
        arrayMap.put(textureArray.getID(), textureArray);
    }

    private void pushTexturesToGPU(Int2ObjectOpenHashMap<TextureArrayInstance> textureArrays) {
        for (int i = 0; i < textureArrays.size(); i++)
            pushTextureToGPU(textureArrays.get(i));
    }

    private void pushTextureToGPU(TextureArrayInstance textureArray) {

        // First and foremost push the array to the gpu and return the handle
        int gpuHandle = GLSLUtility.pushTextureArray(textureArray.getRawImageArray());

        // Next step retrieve the tile data
        Object2ObjectOpenHashMap<String, TextureTileInstance> tileCoordinateMap = textureArray.getTileCoordinateMap();

        // For each tile int he texture array map the appropriate data
        for (Object2ObjectMap.Entry<String, TextureTileInstance> tile : tileCoordinateMap.object2ObjectEntrySet()) {

            UVRect uvCoordinate = computeUV(
                    tile.getValue().getAtlasX(),
                    tile.getValue().getAtlasY(),
                    textureArray.getAtlasSize());

            textureManager.addTextureTile(tile.getValue(), uvCoordinate);
        }

        // For each array map the appropriate data
        textureManager.addTextureArray(textureArray, gpuHandle);
    }

    // Use global image size settigns to calculate UVs for any given texture using
    // known x and y coordinate values stored internally per tile
    private UVRect computeUV(int atlasX, int atlasY, int atlasSize) {

        // Normalized tile size
        float tileSize = 1.0f / atlasSize;

        // Calculate origin
        float u0 = atlasX * tileSize;
        float v0 = atlasY * tileSize;

        // Calculate extent
        float u1 = u0 + tileSize;
        float v1 = v0 + tileSize;

        return new UVRect(u0, v0, u1, v1);
    }
}
