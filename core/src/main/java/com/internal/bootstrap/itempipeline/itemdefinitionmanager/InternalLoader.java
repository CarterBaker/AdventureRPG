package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.bootstrap.itempipeline.itemdefinition.ItemDefinitionData;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private ItemDefinitionManager itemDefinitionManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;
    private Object2ObjectOpenHashMap<String, String> itemName2ResourceName;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ITEM_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.itemName2ResourceName = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "[ItemDefinitionManager] The root folder could not be verified");

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
            throwException("ItemDefinitionManager failed to walk directory: ", e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ObjectArrayList<ItemDefinitionData> items = internalBuilder.build(file, root);
        for (int i = 0; i < items.size(); i++) {
            itemName2ResourceName.put(items.get(i).getItemName(), resourceName);
            itemDefinitionManager.addItem(items.get(i));
        }
    }

    // On-Demand Loading \\

    void request(String itemName) {

        String resourceName = itemName2ResourceName.get(itemName);

        if (resourceName == null)
            throwException(
                    "On-demand item load failed — no file found for item: \"" + itemName + "\"");

        request(resourceName2File.get(resourceName));
    }
}