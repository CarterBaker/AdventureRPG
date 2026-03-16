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

    /*
     * Scans the world map directory for PNG files and loads each one into
     * WorldManager via InternalBuilder. World name is derived from the file
     * stem. Companion JSON is resolved by InternalBuilder if present.
     */

    // Internal
    private File root;
    private WorldManager worldManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> worldName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.WORLD_TEXTURE_PATH);
        this.worldName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "World directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.TEXTURE_FILE_EXTENSIONS.contains(
                            FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String worldName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        worldName2File.put(worldName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk world directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.worldManager = get(WorldManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String worldName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        WorldHandle handle = internalBuilder.build(file, root, worldName);
        worldManager.addWorld(worldName, handle);
    }

    // On-Demand \\

    void request(String worldName) {

        File file = worldName2File.get(worldName);

        if (file == null)
            throwException("On-demand world load failed — not found in scan registry: \""
                    + worldName + "\"");

        request(file);
    }
}