package application.bootstrap.progressionpipeline.tiermanager;

import java.io.File;

import application.bootstrap.progressionpipeline.tier.TierHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TierLoader extends LoaderPackage {

    /*
     * Scans the tier ARPG directory and loads every tier into TierManager.
     * Each tier's name is mapped to its file during scan, so a name asked for
     * before the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private TierManager tierManager;
    private TierBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> tierName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.TIER_PATH);
        this.tierName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Tier directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(TierBuilder.class);
    }

    @Override
    protected void get() {
        this.tierManager = get(TierManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct tierArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "tiers");

        for (int i = 0; i < tierArray.size(); i++) {
            ArpgObjectStruct tierArpg = tierArray.get(i).getAsObject();
            tierName2File.put(ArpgUtility.validateString(tierArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<TierHandle> tiers = internalBuilder.build(file);

        for (int i = 0; i < tiers.size(); i++)
            tierManager.addTierHandle(tiers.get(i));
    }

    // On-Demand \\

    void request(String tierName) {

        File file = tierName2File.get(tierName);

        if (file == null)
            throwException("On-demand tier load failed — no file found for: \"" + tierName + "\"");

        request(file);
    }
}
