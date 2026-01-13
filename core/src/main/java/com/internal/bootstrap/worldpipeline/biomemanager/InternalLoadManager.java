package com.internal.bootstrap.worldpipeline.biomemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

public class InternalLoadManager extends ManagerPackage {

    // Internal
    private BiomeManager biomeManager;
    private InternalBuildSystem internalBuildSystem;
    private File root;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.root = new File(EngineSetting.BIOME_JSON_PATH);
    }

    @Override
    protected void get() {

        // Internal
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void release() {

        // Internal
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Load \\

    void loadBiomes() {

        FileUtility.verifyDirectory(root, "[BiomeManager] The root folder could not be verified");

        Path rootPath = root.toPath();

        try (var stream = Files.walk(rootPath)) {

            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> FileUtility.hasExtension(path.toFile(), EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(path -> processJsonFile(path.toFile()));
        }

        catch (IOException e) {
            throwException("BiomeManager failed to load one or more files: ", e);
        }
    }

    private void processJsonFile(File jsonFile) {

        BiomeHandle biome = internalBuildSystem.compileBiome(jsonFile);

        if (biome != null)
            biomeManager.addBiome(biome);
    }
}