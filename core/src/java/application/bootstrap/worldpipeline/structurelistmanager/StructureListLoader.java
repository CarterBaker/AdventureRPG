package application.bootstrap.worldpipeline.structurelistmanager;

import java.io.File;

import application.bootstrap.worldpipeline.structurelist.StructureListHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class StructureListLoader extends LoaderPackage {

    /*
     * Scans the structure list ARPG directory and loads every list into
     * StructureListManager, one list per file, named by its path below the
     * directory. Supports on-demand loading by list name, and requestAll()
     * so StructureListManager completes its palette before the first chunk
     * generates.
     */

    // Internal
    private File root;
    private StructureListManager structureListManager;
    private StructureListBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> structureListName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(StructureListBuilder.class);
    }

    @Override
    protected void get() {
        this.structureListManager = get(StructureListManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.STRUCTURE_LIST_PATH);
        this.structureListName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Structure list root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String structureListName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            structureListName2File.put(structureListName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String structureListName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        StructureListHandle structureListHandle = internalBuilder.build(file, structureListName);

        structureListManager.addStructureListHandle(structureListHandle);
    }

    // On-Demand \\

    void request(String structureListName) {

        if (structureListManager.hasStructureList(structureListName))
            return;

        File file = structureListName2File.get(structureListName);

        if (file == null)
            throwException("On-demand structure list load failed — no file found for: \"" + structureListName + "\"");

        request(file);
    }
}
