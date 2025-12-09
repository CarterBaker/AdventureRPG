package com.AdventureRPG.core.geometrypipeline.modelmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.AdventureRPG.core.kernel.EngineSetting;
import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;

class InternalLoadManager extends ManagerFrame {

    // Internal
    private File root;
    private ModelManager modelManager;
    private InternalBuildSystem internalBuildSystem;

    private int meshDataCount;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.MODEL_JSON_PATH);
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());

        this.meshDataCount = 0;
    }

    @Override
    protected void init() {

        // Internal
        this.modelManager = gameEngine.get(ModelManager.class);
    }

    @Override
    protected void freeMemory() {
        this.internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // MeshData Management \\

    void loadMeshData() {
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
                    .forEach(path -> buildMeshDataFromFile(path.toFile()));
        }

        catch (IOException e) {
            throw new FileException.FileReadException("MeshDataManager failed to load one or more files: ", e);
        }
    }

    private void buildMeshDataFromFile(File file) {

        if (EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(file))) {

            MeshDataInstance meshDataInstance = internalBuildSystem.buildMeshData(file, meshDataCount++);

            if (meshDataInstance == null) {
                --meshDataCount;
                return;
            }

            compileMeshData(meshDataInstance);
        }
    }

    private void compileMeshData(MeshDataInstance meshDataInstance) {
        modelManager.addMeshData(meshDataInstance);
    }
}
