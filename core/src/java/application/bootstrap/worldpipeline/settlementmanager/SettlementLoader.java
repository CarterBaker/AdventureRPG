package application.bootstrap.worldpipeline.settlementmanager;

import java.io.File;

import application.bootstrap.worldpipeline.settlement.SettlementHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class SettlementLoader extends LoaderPackage {

    /*
     * Scans the settlement ARPG directory and loads every settlement type into
     * SettlementManager, one type per file, named by its path below the
     * directory. Supports on-demand loading by name, and requestAll()
     * so SettlementManager completes its palette before the first chunk
     * generates.
     */

    // Internal
    private File root;
    private SettlementManager settlementManager;
    private SettlementBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> settlementName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(SettlementBuilder.class);
    }

    @Override
    protected void get() {
        this.settlementManager = get(SettlementManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.SETTLEMENT_PATH);
        this.settlementName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Structure list root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String settlementName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            settlementName2File.put(settlementName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String settlementName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        SettlementHandle settlementHandle = internalBuilder.build(file, settlementName);

        settlementManager.addSettlementHandle(settlementHandle);
    }

    // On-Demand \\

    void request(String settlementName) {

        if (settlementManager.hasSettlement(settlementName))
            return;

        File file = settlementName2File.get(settlementName);

        if (file == null)
            throwException("On-demand settlement load failed — no file found for: \"" + settlementName + "\"");

        request(file);
    }
}
