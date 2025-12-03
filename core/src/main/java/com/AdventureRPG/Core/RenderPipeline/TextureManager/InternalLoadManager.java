package com.AdventureRPG.Core.RenderPipeline.TextureManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Bootstrap.EngineSetting;
import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.Util.UVCoordinate;
import com.AdventureRPG.Core.Util.FileUtility;
import com.AdventureRPG.Core.Util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoadManager extends ManagerFrame {

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
        this.aliasLibrarySystem = (AliasLibrarySystem) register(new AliasLibrarySystem());
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());
        this.arrayMap = new Int2ObjectOpenHashMap<TextureArrayInstance>();
        this.root = new File(EngineSetting.BLOCK_TEXTURE_PATH);
    }

    @Override
    protected void init() {

        // Internal
        this.textureManager = gameEngine.get(TextureManager.class);
    }

    @Override
    protected void freeMemory() {

        // Internal
        aliasLibrarySystem = (AliasLibrarySystem) release(aliasLibrarySystem);
        internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
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
            throw new GraphicException.ImageReadException(
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
        arrayMap.put(textureArray.id, textureArray);
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

            UVCoordinate uvCoordinate = computeUV(
                    tile.getValue().getAtlasX(),
                    tile.getValue().getAtlasY(),
                    textureArray.atlasSize);

            textureManager.addTextureTile(tile.getValue(), uvCoordinate);
        }

        // For each array map the appropriate data
        textureManager.addTextureArray(textureArray, gpuHandle);
    }

    // Use global image size settigns to calculate UVs for any given texture using
    // known x and y coordinate values stored internally per tile
    private UVCoordinate computeUV(int atlasX, int atlasY, int atlasSize) {

        float u = (atlasX * EngineSetting.BLOCK_TEXTURE_SIZE) / (float) atlasSize;
        float v = (atlasY * EngineSetting.BLOCK_TEXTURE_SIZE) / (float) atlasSize;

        return new UVCoordinate(u, v);
    }
}
