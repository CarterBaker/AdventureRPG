package com.AdventureRPG.Core.RenderPipeline.TextureManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.AdventureRPG.Core.Bootstrap.ManagerFrame;
import com.AdventureRPG.Core.RenderPipeline.Util.GlobalConstant;
import com.AdventureRPG.Core.Util.Exceptions.FileException;

class InternalLoadManager extends ManagerFrame {

    // Internal
    private InternalBuildSystem internalBuildSystem;
    private AliasLibrarySystem aliasLibrarySystem;
    private File root;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());
        this.aliasLibrarySystem = (AliasLibrarySystem) register(new AliasLibrarySystem());
        this.root = new File(GlobalConstant.BLOCK_TEXTURE_PATH);
    }

    // File Navigation \\

    // The main method that delegates logic to helpers to return texture arrays
    void compileTextureArrays() {

        if (!root.exists() || !root.isDirectory()) // TODO: move to static helper class.
            throw new FileException.FileNotFoundException(root);

        Path rootPath = root.toPath();

        try (var stream = Files.walk(rootPath)) {
            stream
                    .filter(Files::isDirectory)
                    .filter(folder -> !folder.equals(rootPath)) // exclude root itself
                    .forEach(folder -> categorizeTextureFiles(folder.toFile()));
        }

        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Categorization & Suffix logic \\

    private void categorizeTextureFiles(File directory) {

        List<File> imageFiles = new ArrayList<>();
        File[] files = directory.listFiles();

        if (files == null)
            return;

        for (File file : files) {
            if (file.isFile() && isValidImageFile(file))
                imageFiles.add(file);

        if (!imageFiles.isEmpty())
            categorizeTextureArrayInstances(imageFiles, directory);
    }

    private boolean isValidImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".png");
    }

    private void categorizeTextureArrayInstances(List<File> imageFiles, File sourceDirectory) {
        // Your texture array building logic here
    }
}
