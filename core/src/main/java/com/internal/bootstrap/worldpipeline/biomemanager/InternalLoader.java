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

class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private BiomeManager biomeManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.biomeManager = get(BiomeManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BIOME_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Biome root directory not found: " + root.getAbsolutePath());

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
            throwException("Failed to walk biome directory: " + root.getAbsolutePath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        BiomeHandle biomeHandle = internalBuilder.build(file, root);

        if (biomeHandle != null)
            biomeManager.addBiome(biomeHandle);
    }

    // On-Demand \\

    void request(String biomeName) {

        File file = resourceName2File.get(biomeName);

        if (file == null)
            throwException("On-demand biome load failed — no file found for: \"" + biomeName + "\"");

        request(file);
    }
}