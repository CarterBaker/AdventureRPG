package application.bootstrap.dialoguepipeline.relationmanager;

import java.io.File;

import application.bootstrap.dialoguepipeline.relation.RelationHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class RelationLoader extends LoaderPackage {

    /*
     * Scans the relation ARPG directory and loads every relation into
     * RelationManager. Each relation's name is mapped to its file during scan, so
     * a name asked for before the bootstrap batch reaches it loads its file on
     * demand.
     */

    // Internal
    private File root;
    private RelationManager relationManager;
    private RelationBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> relationName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.RELATION_PATH);
        this.relationName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Relation directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            mapNames(file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(RelationBuilder.class);
    }

    @Override
    protected void get() {
        this.relationManager = get(RelationManager.class);
    }

    // Scan \\

    private void mapNames(File file) {

        ArpgArrayStruct relationArray = ArpgUtility.validateArray(ArpgUtility.loadObject(file), "relations");

        for (int i = 0; i < relationArray.size(); i++) {
            ArpgObjectStruct relationArpg = relationArray.get(i).getAsObject();
            relationName2File.put(ArpgUtility.validateString(relationArpg, "name"), file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        ObjectArrayList<RelationHandle> relations = internalBuilder.build(file);

        for (int i = 0; i < relations.size(); i++)
            relationManager.addRelationHandle(relations.get(i));
    }

    // On-Demand \\

    void request(String relationName) {

        File file = relationName2File.get(relationName);

        if (file == null)
            throwException("On-demand relation load failed — no file found for: \"" + relationName + "\"");

        request(file);
    }
}
