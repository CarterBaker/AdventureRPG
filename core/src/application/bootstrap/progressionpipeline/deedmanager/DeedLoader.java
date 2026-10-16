package application.bootstrap.progressionpipeline.deedmanager;

import java.io.File;

import application.bootstrap.progressionpipeline.deed.DeedHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class DeedLoader extends LoaderPackage {

    /*
     * Scans the deed ARPG directory and loads every deed into DeedManager.
     * Each deed's name is mapped to its file during scan, so a name asked for
     * before the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private DeedManager deedManager;
    private DeedBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> deedName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.DEED_PATH);
        this.deedName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Deed directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(DeedBuilder.class);
    }

    @Override
    protected void get() {
        this.deedManager = get(DeedManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct deedArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "deeds");

        for (int i = 0; i < deedArray.size(); i++) {
            ArpgObjectStruct deedArpg = deedArray.get(i).getAsObject();
            deedName2File.put(ArpgUtility.validateString(deedArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<DeedHandle> deeds = internalBuilder.build(file);

        for (int i = 0; i < deeds.size(); i++)
            deedManager.addDeedHandle(deeds.get(i));
    }

    // On-Demand \\

    void request(String deedName) {

        File file = deedName2File.get(deedName);

        if (file == null)
            throwException("On-demand deed load failed — no file found for: \"" + deedName + "\"");

        request(file);
    }
}
