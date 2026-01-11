package com.AdventureRPG.bootstrap.worldpipeline.worldstreammanager;

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
    private WorldStreamManager worldStreamManager;
    private InternalBuildSystem internalBuildSystem;
    private int worldCount;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.WORLD_TEXTURE_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.worldCount = 0;
    }

    @Override
    protected void get() {

        // Internal
        this.worldStreamManager = get(WorldStreamManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // World Management \\

    void loadWorlds() {
        loadAllFiles();
    }

    // Load \\

    private void loadAllFiles() {
        if (!root.exists() || !root.isDirectory())
            throwException("World directory not found: " + root.getAbsolutePath());

        Path base = root.toPath();

        try (var stream = Files.walk(base)) {
            stream
                    .filter(Files::isRegularFile)
                    .forEach(path -> buildWorldFromFile(path.toFile()));
        } catch (IOException e) {
            throwException("WorldStreamManager failed to load one or more files: ", e);
        }
    }

    private void buildWorldFromFile(File file) {
        if (EngineSetting.TEXTURE_FILE_EXTENSIONS.contains(FileUtility.getExtension(file)))
            compileWorld(internalBuildSystem.buildWorld(root, file, worldCount++));
    }

    private void compileWorld(WorldHandle world) {
        worldStreamManager.addWorld(world);
    }
}