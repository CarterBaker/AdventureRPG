package com.internal.bootstrap.worldpipeline.worldstreammanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoadManager extends LoaderPackage {

    // Internal
    private File root;
    private WorldStreamManager worldStreamManager;
    private InternalBuildSystem internalBuildSystem;
    private int worldCount;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.WORLD_TEXTURE_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        if (!root.exists() || !root.isDirectory())
            throwException("World directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.TEXTURE_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("WorldStreamManager failed to walk directory: ", e);
        }
    }

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.worldCount = 0;
    }

    @Override
    protected void get() {
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        WorldHandle world = internalBuildSystem.build(file, root, worldCount++);
        worldStreamManager.addWorld(world);
    }

    // On-Demand Loading \\

    void request(String worldName) {
        File file = resourceName2File.get(worldName);
        if (file == null)
            throwException(
                    "[InternalLoadManager] On-demand world load failed — resource not found in scan registry: \""
                            + worldName + "\"");
        request(file);
    }
}