package com.internal.bootstrap.worldpipeline.worldmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.worldpipeline.world.WorldHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private WorldManager worldManager;
    private InternalBuilder internalBuilder;
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
        this.internalBuilder = create(InternalBuilder.class);
        this.worldCount = 0;
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        WorldHandle world = internalBuilder.build(file, root, worldCount++);
        worldManager.addWorld(world);
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