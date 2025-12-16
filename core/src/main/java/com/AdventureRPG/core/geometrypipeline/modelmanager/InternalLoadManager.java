package com.AdventureRPG.core.geometrypipeline.modelmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.AdventureRPG.core.geometrypipeline.Mesh.MeshHandle;
import com.AdventureRPG.core.kernel.EngineSetting;
import com.AdventureRPG.core.kernel.ManagerFrame;
import com.AdventureRPG.core.util.FileUtility;
import com.AdventureRPG.core.util.Exceptions.FileException;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class InternalLoadManager extends ManagerFrame {

    // Internal
    private File root;
    private ModelManager modelManager;
    private InternalBuildSystem internalBuildSystem;
    private int meshDataCount;

    // File Registry
    private Map<String, File> resourceName2File;

    // Base \\

    @Override
    protected void create() {
        this.root = new File(EngineSetting.MODEL_JSON_PATH);
        this.internalBuildSystem = (InternalBuildSystem) register(new InternalBuildSystem());
        this.meshDataCount = 0;
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void init() {
        this.modelManager = gameEngine.get(ModelManager.class);
    }

    @Override
    protected void freeMemory() {
        this.internalBuildSystem = (InternalBuildSystem) release(internalBuildSystem);
    }

    // MeshData Management \\

    void loadMeshData() {
        List<File> meshFiles = collectMeshFiles();
        buildFileRegistry(meshFiles);
        processMeshFiles(meshFiles);
    }

    // File Collection \\

    private List<File> collectMeshFiles() {
        validateRootDirectory();

        Path basePath = root.toPath();

        try (var stream = Files.walk(basePath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(this::isValidJsonFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new FileException.FileReadException(
                    "Failed to list mesh files in directory: " + root.getAbsolutePath(), e);
        }
    }

    private void validateRootDirectory() {
        if (!root.exists() || !root.isDirectory()) {
            throw new FileException.FileNotFoundException("Mesh JSON directory not found: " + root.getAbsolutePath());
        }
    }

    private boolean isValidJsonFile(File file) {
        String extension = FileUtility.getExtension(file);
        return EngineSetting.JSON_FILE_EXTENSIONS.contains(extension);
    }

    // File Registry \\

    private void buildFileRegistry(List<File> meshFiles) {
        for (File file : meshFiles) {
            String resourceName = FileUtility.getFileName(file);
            resourceName2File.put(resourceName, file);
        }
    }

    public File getFileByResourceName(String resourceName) {
        return resourceName2File.get(resourceName);
    }

    // File Processing \\

    private void processMeshFiles(List<File> meshFiles) {
        for (File file : meshFiles) {
            processMeshFile(file);
        }
    }

    private void processMeshFile(File file) {
        String meshName = FileUtility.getFileName(file);

        try {
            MeshHandle meshHandle = internalBuildSystem.buildMeshHandle(file, meshDataCount, this);

            if (meshHandle != null) {
                registerMeshData(meshName, meshHandle);
            }
        } catch (RuntimeException ex) {
            throw new FileException.FileReadException(
                    "Failed to build mesh from file: " + file.getAbsolutePath(), ex);
        }
    }

    private void registerMeshData(String meshName, MeshHandle meshHandle) {
        modelManager.addMeshHandle(meshName, meshDataCount, meshHandle);
        meshDataCount++;
    }
}