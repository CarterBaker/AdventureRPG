package application.bootstrap.itempipeline.itemdefinitionmanager;

import java.io.File;

import application.bootstrap.itempipeline.itemdefinition.ItemDefinitionHandle;
import application.bootstrap.itempipeline.util.ItemRegistryUtility;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ItemDefinitionLoader extends LoaderPackage {

    /*
     * Scans the item ARPG directory and loads all item definitions into
     * ItemDefinitionManager. An item's name begins with the resource name of
     * the file that defines it, so an on-demand request finds that file
     * before it has been loaded.
     */

    // Internal
    private File root;
    private ItemDefinitionManager itemDefinitionManager;
    private ItemDefinitionBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ITEM_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Item directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
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

        resourceName2File.remove(FileUtility.getPathWithFileNameWithoutExtension(root, file));
        ObjectArrayList<ItemDefinitionHandle> items = internalBuilder.build(file, root);

        for (int i = 0; i < items.size(); i++)
            itemDefinitionManager.addItem(items.get(i));
    }

    // On-Demand \\

    void request(String itemName) {

        File file = resourceName2File.get(ItemRegistryUtility.toDefinitionName(itemName));

        if (file == null)
            throwException("On-demand item load failed — no unloaded file defines: \"" + itemName + "\"");

        request(file);
    }
}