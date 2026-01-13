package com.internal.bootstrap.geometrypipeline.modelmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

public class InternalLoadManager extends ManagerPackage {

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
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.meshDataCount = 0;
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.modelManager = get(ModelManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
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
        }

        catch (IOException e) {
            throwException(
                    "Failed to list mesh files in directory: " + root.getAbsolutePath(), e);
        }

        return null;
    }

    private void validateRootDirectory() {
        if (!root.exists() || !root.isDirectory())
            throwException("Mesh JSON directory not found: " + root.getAbsolutePath());
    }

    private boolean isValidJsonFile(File file) {
        String extension = FileUtility.getExtension(file);
        return EngineSetting.JSON_FILE_EXTENSIONS.contains(extension);
    }

    // File Registry \\

    private void buildFileRegistry(List<File> meshFiles) {

        for (File file : meshFiles) {

            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

            resourceName2File.put(resourceName, file);
        }
    }

    public File getFileByResourceName(String resourceName) {
        return resourceName2File.get(resourceName);
    }

    // File Processing \\

    private void processMeshFiles(List<File> meshFiles) {
        for (File file : meshFiles)
            processMeshFile(file);
    }

    public void processMeshFile(File file) {

        String meshName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {

            int meshID = meshDataCount++;
            MeshHandle meshHandle = internalBuildSystem.buildMeshHandle(root, file, meshID, this);

            if (meshHandle != null)
                createMeshData(meshName, meshID, meshHandle);
        }

        catch (RuntimeException ex) {
            throwException(
                    "Failed to build mesh from file: " + file.getAbsolutePath(), ex);
        }
    }

    private void createMeshData(String meshName, int meshID, MeshHandle meshHandle) {
        modelManager.addMeshHandle(meshName, meshID, meshHandle);
    }
}