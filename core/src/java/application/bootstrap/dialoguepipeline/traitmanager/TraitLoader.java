package application.bootstrap.dialoguepipeline.traitmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.trait.TraitHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class TraitLoader extends LoaderPackage {

    /*
     * Scans the trait ARPG directory and loads every trait into TraitManager. Each
     * trait's name is mapped to its file during scan, so a name asked for before
     * the bootstrap batch reaches it loads its file on demand.
     */

    // Internal
    private File root;
    private TraitManager traitManager;
    private TraitBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> traitName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.TRAIT_PATH);
        this.traitName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Trait directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(TraitBuilder.class);
    }

    @Override
    protected void get() {
        this.traitManager = get(TraitManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct traitArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "traits");

        for (int i = 0; i < traitArray.size(); i++) {
            ArpgObjectStruct traitArpg = traitArray.get(i).getAsObject();
            traitName2File.put(ArpgUtility.validateString(traitArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<TraitHandle> traits = internalBuilder.build(file);

        for (int i = 0; i < traits.size(); i++)
            traitManager.addTraitHandle(traits.get(i));
    }

    // On-Demand \\

    void request(String traitName) {

        File file = traitName2File.get(traitName);

        if (file == null)
            throwException("On-demand trait load failed — no file found for: \"" + traitName + "\"");

        request(file);
    }
}
