package com.internal.bootstrap.worldpipeline.blockmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.bootstrap.worldpipeline.block.BlockHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private BlockManager blockManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;
    private Object2ObjectOpenHashMap<String, String> blockName2ResourceName;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.blockManager = get(BlockManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BLOCK_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.blockName2ResourceName = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Block root directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        preRegisterBlockNames(file, resourceName);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk block directory: " + root.getAbsolutePath(), e);
        }
    }

    // Pre-Registration \\

    /*
     * Peeks the JSON during scan to extract block names and build the reverse
     * lookup before any load() fires. This is what allows on-demand requests
     * from awake() to resolve correctly — the name → file mapping must be
     * complete before the batch phase begins.
     */
    private void preRegisterBlockNames(File file, String resourceName) {

        try {
            JsonObject json = JsonUtility.loadJsonObject(file);
            JsonArray blockArray = json.getAsJsonArray("blocks");

            if (blockArray == null)
                return;

            for (int i = 0; i < blockArray.size(); i++) {
                JsonObject blockJson = blockArray.get(i).getAsJsonObject();
                if (!blockJson.has("name"))
                    continue;
                String localName = blockJson.get("name").getAsString();
                String blockName = resourceName + "/" + localName;
                blockName2ResourceName.put(blockName, resourceName);
            }
        } catch (Exception e) {
            throwException("Failed to pre-register block names from: " + file.getPath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<BlockHandle> blocks = internalBuilder.build(file, root);

        for (int i = 0; i < blocks.size(); i++)
            blockManager.addBlock(blocks.get(i));
    }

    // On-Demand \\

    void request(String blockName) {

        String resourceName = blockName2ResourceName.get(blockName);

        if (resourceName == null)
            throwException("On-demand block load failed — no file found for: \"" + blockName + "\"");

        request(resourceName2File.get(resourceName));
    }
}