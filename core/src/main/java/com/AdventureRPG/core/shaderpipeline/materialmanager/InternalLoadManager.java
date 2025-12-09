package com.AdventureRPG.core.shaderpipeline.materialmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.AdventureRPG.core.kernel.EngineSetting;
import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.shaderpipeline.material.Material;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;

class InternalLoadManager extends ManagerFrame {

    // Internal
    private File root;
    private MaterialManager materialManager;
    private InternalBuildSystem internalBuildSystem;

    private int materialCount;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.MATERIAL_JSON_PATH);
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        this.materialCount = 0;
    }

    @Override
    protected void init() {

        // Internal
        this.materialManager = gameEngine.get(MaterialManager.class);
    }

    @Override
    protected void freeMemory() {
        this.internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // Material Management \\

    void loadMaterials() {
        loadAllFiles();
    }

    // Load \\

    private void loadAllFiles() {

        if (!root.exists() || !root.isDirectory())
            throw new FileException.FileNotFoundException("Shader directory not found: " + root.getAbsolutePath());

        Path base = root.toPath();

        try (var stream = Files.walk(base)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> buildMaterialFromFile(path.toFile()));
        }

        catch (IOException e) {
            throw new FileException.FileReadException("MaterialManager failed to load one or more files: ", e);
        }
    }

    private void buildMaterialFromFile(File file) {

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
            compileMaterial(internalBuildSystem.buildMaterial(file, materialCount++));
    }

    private void compileMaterial(Material material) {
        materialManager.addMaterial(material);
    }
}
