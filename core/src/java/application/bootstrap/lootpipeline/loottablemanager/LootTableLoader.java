package application.bootstrap.lootpipeline.loottablemanager;

import java.io.File;

import application.bootstrap.lootpipeline.loottable.LootTableHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class LootTableLoader extends LoaderPackage {

    /*
     * Scans the loot table ARPG directory and loads every loot table into
     * LootTableManager, one table per file, named by its path below the
     * directory. Supports on-demand loading by loot table name, and
     * requestAll() so LootTableManager completes its palette before anything
     * draws from it.
     */

    // Internal
    private File root;
    private LootTableManager lootTableManager;
    private LootTableBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> lootTableName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(LootTableBuilder.class);
    }

    @Override
    protected void get() {
        this.lootTableManager = get(LootTableManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.LOOT_TABLE_PATH);
        this.lootTableName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Loot table root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String lootTableName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            lootTableName2File.put(lootTableName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String lootTableName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        LootTableHandle lootTableHandle = internalBuilder.build(file, lootTableName);

        lootTableManager.addLootTableHandle(lootTableHandle);
    }

    // On-Demand \\

    void request(String lootTableName) {

        if (lootTableManager.hasLootTable(lootTableName))
            return;

        File file = lootTableName2File.get(lootTableName);

        if (file == null)
            throwException("On-demand loot table load failed — no file found for: \"" + lootTableName + "\"");

        request(file);
    }
}
