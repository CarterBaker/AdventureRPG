package application.bootstrap.progressionpipeline.gendermanager;

import java.io.File;

import application.bootstrap.progressionpipeline.gender.GenderHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class GenderLoader extends LoaderPackage {

    /*
     * Scans the gender ARPG directory and loads every gender into
     * GenderManager. Each gender's name is mapped to its file during scan, so
     * a name asked for before the bootstrap batch reaches it loads its file on
     * demand.
     */

    // Internal
    private File root;
    private GenderManager genderManager;
    private GenderBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> genderName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.GENDER_PATH);
        this.genderName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Gender directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(GenderBuilder.class);
    }

    @Override
    protected void get() {
        this.genderManager = get(GenderManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct genderArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "genders");

        for (int i = 0; i < genderArray.size(); i++) {
            ArpgObjectStruct genderArpg = genderArray.get(i).getAsObject();
            genderName2File.put(ArpgUtility.validateString(genderArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<GenderHandle> genders = internalBuilder.build(file);

        for (int i = 0; i < genders.size(); i++)
            genderManager.addGenderHandle(genders.get(i));
    }

    // On-Demand \\

    void request(String genderName) {

        File file = genderName2File.get(genderName);

        if (file == null)
            throwException("On-demand gender load failed — no file found for: \"" + genderName + "\"");

        request(file);
    }
}
