package application.bootstrap.progressionpipeline.statmanager;

import java.io.File;

import application.bootstrap.progressionpipeline.stat.StatHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class StatLoader extends LoaderPackage {

    /*
     * Scans the stat ARPG directory and loads every stat into StatManager.
     * Each stat's name is mapped to its file during scan, so a name asked for
     * before the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private StatManager statManager;
    private StatBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> statName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.STAT_PATH);
        this.statName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Stat directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(StatBuilder.class);
    }

    @Override
    protected void get() {
        this.statManager = get(StatManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct statArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "stats");

        for (int i = 0; i < statArray.size(); i++) {
            ArpgObjectStruct statArpg = statArray.get(i).getAsObject();
            statName2File.put(ArpgUtility.validateString(statArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<StatHandle> stats = internalBuilder.build(file);

        for (int i = 0; i < stats.size(); i++)
            statManager.addStatHandle(stats.get(i));
    }

    // On-Demand \\

    void request(String statName) {

        File file = statName2File.get(statName);

        if (file == null)
            throwException("On-demand stat load failed — no file found for: \"" + statName + "\"");

        request(file);
    }
}
