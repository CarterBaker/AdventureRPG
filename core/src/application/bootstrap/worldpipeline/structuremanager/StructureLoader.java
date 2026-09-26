package application.bootstrap.worldpipeline.structuremanager;

import java.io.File;

import application.bootstrap.worldpipeline.structure.StructureHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class StructureLoader extends LoaderPackage {

    /*
     * Scans the structure JSON directory and loads every structure definition
     * into StructureManager. Supports on-demand loading by structure name, and
     * requestAll() so StructureManager can complete its palette before the
     * first chunk generates.
     */

    // Internal
    private File root;
    private StructureManager structureManager;
    private StructureBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> structureName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(StructureBuilder.class);
    }

    @Override
    protected void get() {
        this.structureManager = get(StructureManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.STRUCTURE_JSON_PATH);
        this.structureName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Structure root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String structureName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            structureName2File.put(structureName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String structureName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        StructureHandle structureHandle = internalBuilder.build(file, structureName);

        if (structureHandle == null)
            throwException("Failed to build structure from: " + file.getAbsolutePath());

        structureManager.addStructureHandle(structureHandle);
    }

    // On-Demand \\

    void request(String structureName) {

        if (structureManager.hasStructure(structureName))
            return;

        File file = structureName2File.get(structureName);

        if (file == null)
            throwException("On-demand structure load failed — no file found for: \"" + structureName + "\"");

        request(file);
    }
}
