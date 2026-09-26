package application.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemDefinitionLoader extends LoaderPackage {

    /*
     * Scans the item JSON directory and loads all item definitions into
     * ItemDefinitionManager. Maintains a reverse mapping from item name to
     * resource name to support on-demand loading at runtime.
     */

    // Internal
    private File root;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemDefinitionBuilder internalBuilder;

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

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(ItemDefinitionBuilder.class);
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