package application.bootstrap.dialoguepipeline.verdictmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.verdict.VerdictHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class VerdictLoader extends LoaderPackage {

    /*
     * Scans the verdict ARPG directory and loads every verdict into
     * VerdictManager. Each verdict's name is mapped to its file during scan, so a
     * name asked for before the bootstrap batch reaches it loads its file on
     * demand.
     */

    // Internal
    private File root;
    private VerdictManager verdictManager;
    private VerdictBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> verdictName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.VERDICT_PATH);
        this.verdictName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Verdict directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(VerdictBuilder.class);
    }

    @Override
    protected void get() {
        this.verdictManager = get(VerdictManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct verdictArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "verdicts");

        for (int i = 0; i < verdictArray.size(); i++) {
            ArpgObjectStruct verdictArpg = verdictArray.get(i).getAsObject();
            verdictName2File.put(ArpgUtility.validateString(verdictArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<VerdictHandle> verdicts = internalBuilder.build(file);

        for (int i = 0; i < verdicts.size(); i++)
            verdictManager.addVerdictHandle(verdicts.get(i));
    }

    // On-Demand \\

    void request(String verdictName) {

        File file = verdictName2File.get(verdictName);

        if (file == null)
            throwException("On-demand verdict load failed — no file found for: \"" + verdictName + "\"");

        request(file);
    }
}
