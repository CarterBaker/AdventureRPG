package com.internal.bootstrap.worldpipeline.biomemanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.worldpipeline.biome.BiomeHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoadManager extends LoaderPackage {

    // Internal
    private File root;
    private BiomeManager biomeManager;
    private InternalBuildSystem internalBuildSystem;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BIOME_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "[BiomeManager] The root folder could not be verified");

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("BiomeManager failed to walk directory: ", e);
        }
    }

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
    }

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        BiomeHandle biome = internalBuildSystem.build(file, root);
        if (biome != null)
            biomeManager.addBiome(biome);
    }

    // On-Demand Loading \\

    void request(String biomeName) {
        File file = resourceName2File.get(biomeName);
        if (file == null)
            throwException(
                    "[InternalLoadManager] On-demand biome load failed — no file found for biome: \""
                            + biomeName + "\"");
        request(file);
    }
}