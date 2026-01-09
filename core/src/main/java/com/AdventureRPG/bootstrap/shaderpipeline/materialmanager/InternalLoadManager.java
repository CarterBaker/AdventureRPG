package com.AdventureRPG.bootstrap.shaderpipeline.materialmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.AdventureRPG.core.engine.ManagerPackage;
import com.AdventureRPG.core.engine.settings.EngineSetting;
import com.AdventureRPG.core.util.FileUtility;

class InternalLoadManager extends ManagerPackage {

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
        this.internalBuildSystem = create(InternalBuildSystem.class);

        this.materialCount = 0;
    }

    @Override
    protected void get() {

        // Internal
        this.materialManager = get(MaterialManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Material Management \\

    void loadMaterials() {
        loadAllFiles();
    }

    // Load \\

    private void loadAllFiles() {

        if (!root.exists() || !root.isDirectory())
            throwException("Shader directory not found: " + root.getAbsolutePath());

        Path base = root.toPath();

        try (var stream = Files.walk(base)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> buildMaterialFromFile(path.toFile()));
        }

        catch (IOException e) {
            throwException("MaterialManager failed to load one or more files: ", e);
        }
    }

    private void buildMaterialFromFile(File file) {

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
            compileMaterial(internalBuildSystem.buildMaterial(root, file, materialCount++));
    }

    private void compileMaterial(MaterialHandle material) {
        materialManager.addMaterial(material);
    }
}
