package com.internal.bootstrap.shaderpipeline.materialmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Drives the material bootstrap sequence: file discovery in scan(), one
 * material assembled per load() call, self-releases when queue empties.
 */
class InternalLoadManager extends LoaderPackage {

    // Internal
    private File root;
    private MaterialManager materialManager;
    private InternalBuildSystem internalBuildSystem;
    private int materialCount;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MATERIAL_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Material directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Material directory walk failed: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.materialCount = 0;
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        materialManager.addMaterial(
                internalBuildSystem.build(root, file, materialCount++));
    }

    // On-Demand Loading \\

    void request(String materialName) {

        File file = resourceName2File.get(materialName);

        if (file == null)
            throwException(
                    "On-demand material load failed — resource not found in scan registry: \"" + materialName + "\"");

        request(file);
    }
}