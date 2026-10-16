package application.bootstrap.progressionpipeline.professionmanager;

import java.io.File;

import application.bootstrap.progressionpipeline.profession.ProfessionHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class ProfessionLoader extends LoaderPackage {

    /*
     * Scans the profession ARPG directory and loads every profession into
     * ProfessionManager. Each profession's name is mapped to its file during
     * scan, so a name asked for before the bootstrap batch reaches it loads
     * its file on demand.
     */

    // Internal
    private File root;
    private ProfessionManager professionManager;
    private ProfessionBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> professionName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.PROFESSION_PATH);
        this.professionName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Profession directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(ProfessionBuilder.class);
    }

    @Override
    protected void get() {
        this.professionManager = get(ProfessionManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct professionArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "professions");

        for (int i = 0; i < professionArray.size(); i++) {
            ArpgObjectStruct professionArpg = professionArray.get(i).getAsObject();
            professionName2File.put(ArpgUtility.validateString(professionArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<ProfessionHandle> professions = internalBuilder.build(file);

        for (int i = 0; i < professions.size(); i++)
            professionManager.addProfessionHandle(professions.get(i));
    }

    // On-Demand \\

    void request(String professionName) {

        File file = professionName2File.get(professionName);

        if (file == null)
            throwException("On-demand profession load failed — no file found for: \"" + professionName + "\"");

        request(file);
    }
}
