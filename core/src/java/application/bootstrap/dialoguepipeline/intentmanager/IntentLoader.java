package application.bootstrap.dialoguepipeline.intentmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.intent.IntentHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class IntentLoader extends LoaderPackage {

    /*
     * Scans the intent ARPG directory and loads every intent into IntentManager.
     * Each intent's name is mapped to its file during scan, so a name asked for
     * before the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private IntentManager intentManager;
    private IntentBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> intentName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.INTENT_PATH);
        this.intentName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Intent directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(IntentBuilder.class);
    }

    @Override
    protected void get() {
        this.intentManager = get(IntentManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct intentArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "intents");

        for (int i = 0; i < intentArray.size(); i++) {
            ArpgObjectStruct intentArpg = intentArray.get(i).getAsObject();
            intentName2File.put(ArpgUtility.validateString(intentArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<IntentHandle> intents = internalBuilder.build(file);

        for (int i = 0; i < intents.size(); i++)
            intentManager.addIntentHandle(intents.get(i));
    }

    // On-Demand \\

    void request(String intentName) {

        File file = intentName2File.get(intentName);

        if (file == null)
            throwException("On-demand intent load failed — no file found for: \"" + intentName + "\"");

        request(file);
    }
}
