package application.bootstrap.progressionpipeline.damagetypemanager;

import java.io.File;

import application.bootstrap.progressionpipeline.damagetype.DamageTypeHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class DamageTypeLoader extends LoaderPackage {

    /*
     * Scans the damage type ARPG directory and loads every damage type into
     * DamageTypeManager. Each damage type's name is mapped to its file during
     * scan, so a name asked for before the bootstrap batch reaches it loads
     * its file on demand.
     */

    // Internal
    private File root;
    private DamageTypeManager damageTypeManager;
    private DamageTypeBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> damageTypeName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.DAMAGE_TYPE_PATH);
        this.damageTypeName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Damage type directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(DamageTypeBuilder.class);
    }

    @Override
    protected void get() {
        this.damageTypeManager = get(DamageTypeManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct damageTypeArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "damage_types");

        for (int i = 0; i < damageTypeArray.size(); i++) {
            ArpgObjectStruct damageTypeArpg = damageTypeArray.get(i).getAsObject();
            damageTypeName2File.put(ArpgUtility.validateString(damageTypeArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<DamageTypeHandle> damageTypes = internalBuilder.build(file);

        for (int i = 0; i < damageTypes.size(); i++)
            damageTypeManager.addDamageTypeHandle(damageTypes.get(i));
    }

    // On-Demand \\

    void request(String damageTypeName) {

        File file = damageTypeName2File.get(damageTypeName);

        if (file == null)
            throwException("On-demand damage type load failed — no file found for: \"" + damageTypeName + "\"");

        request(file);
    }
}
