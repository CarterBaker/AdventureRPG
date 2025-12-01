package com.AdventureRPG.Core.RenderPipeline.TextureManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Bootstrap.EngineSetting;
import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.Util.FileUtility;
import com.AdventureRPG.Core.Util.Exceptions.GraphicException;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

class InternalLoadManager extends ManagerFrame {

    // Internal
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
    protected void freeMemory() {

        // Internal
        aliasLibrarySystem = (AliasLibrarySystem) release(aliasLibrarySystem);
        internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // File Navigation \\

    // The main method that delegates logic to helpers to return texture arrays
    Int2ObjectOpenHashMap<TextureArrayInstance> loadTextureArrays() {

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

        return arrayMap;
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

    // Accessbile \\

    AliasInstance[] getAllAliases() {
        return aliasLibrarySystem.getAllAliases();
    }
}
