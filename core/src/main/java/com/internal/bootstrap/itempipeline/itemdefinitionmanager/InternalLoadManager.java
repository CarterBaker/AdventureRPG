package com.internal.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class InternalLoadManager extends ManagerPackage {

    // Internal

    private ItemDefinitionManager itemDefinitionManager;
    private InternalBuildSystem internalBuildSystem;
    private File root;

    // Base \

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.root = new File(EngineSetting.ITEM_JSON_PATH);
    }

    @Override
    protected void get() {
        this.itemDefinitionManager = get(ItemDefinitionManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Load \

    void loadItems() {
        FileUtility.verifyDirectory(root, "[ItemDefinitionManager] The root folder could not be verified");

        Path rootPath = root.toPath();

        try (var stream = Files.walk(rootPath)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> FileUtility.hasExtension(path.toFile(), EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(path -> processJsonFile(path.toFile()));
        } catch (IOException e) {
            throwException("ItemDefinitionManager failed to load one or more files: ", e);
        }
    }

    private void processJsonFile(File jsonFile) {
        ObjectArrayList<ItemDefinitionData> items = internalBuildSystem.compileItems(jsonFile, root);
        for (int i = 0; i < items.size(); i++)
            itemDefinitionManager.addItem(items.get(i));
    }

}