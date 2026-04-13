package application.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.LoaderPackage;
import engine.settings.EngineSetting;
import engine.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the item JSON directory and loads all item definitions into
     * ItemDefinitionManager. Maintains a reverse mapping from item name to
     * resource name to support on-demand loading at runtime.
     */

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

        FileUtility.verifyDirectory(root, "Item directory not found: " + root.getAbsolutePath());

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
            throwException("Failed to walk item directory: " + root.getAbsolutePath(), e);
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
        ObjectArrayList<ItemDefinitionHandle> items = internalBuilder.build(file, root);

        for (int i = 0; i < items.size(); i++) {
            itemName2ResourceName.put(items.get(i).getItemName(), resourceName);
            itemDefinitionManager.addItem(items.get(i));
        }
    }

    // On-Demand \\

    void request(String itemName) {

        String resourceName = itemName2ResourceName.get(itemName);

        if (resourceName == null)
            throwException("On-demand item load failed — no file found for: \"" + itemName + "\"");

        request(resourceName2File.get(resourceName));
    }
}