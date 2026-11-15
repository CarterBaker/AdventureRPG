package application.bootstrap.dialoguepipeline.moodmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.mood.MoodHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class MoodLoader extends LoaderPackage {

    /*
     * Scans the mood ARPG directory and loads every mood into MoodManager. Each
     * mood's name is mapped to its file during scan, so a name asked for before
     * the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private MoodManager moodManager;
    private MoodBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> moodName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MOOD_PATH);
        this.moodName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Mood directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(MoodBuilder.class);
    }

    @Override
    protected void get() {
        this.moodManager = get(MoodManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct moodArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "moods");

        for (int i = 0; i < moodArray.size(); i++) {
            ArpgObjectStruct moodArpg = moodArray.get(i).getAsObject();
            moodName2File.put(ArpgUtility.validateString(moodArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<MoodHandle> moods = internalBuilder.build(file);

        for (int i = 0; i < moods.size(); i++)
            moodManager.addMoodHandle(moods.get(i));
    }

    // On-Demand \\

    void request(String moodName) {

        File file = moodName2File.get(moodName);

        if (file == null)
            throwException("On-demand mood load failed — no file found for: \"" + moodName + "\"");

        request(file);
    }
}
