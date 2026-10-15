package application.bootstrap.combatpipeline.movesetmanager;

import java.io.File;

import application.bootstrap.combatpipeline.moveset.MovesetHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class MovesetLoader extends LoaderPackage {

    /*
     * Scans the moveset ARPG directory and loads every moveset into
     * MovesetManager. Supports on-demand loading for movesets not yet in the
     * palette — item definitions resolve the moveset they name through that
     * path while they load.
     */

    // Internal
    private File root;
    private MovesetManager movesetManager;
    private MovesetBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> movesetName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MOVESET_PATH);
        this.movesetName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Moveset ARPG directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String movesetName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            movesetName2File.put(movesetName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(MovesetBuilder.class);
    }

    @Override
    protected void get() {
        this.movesetManager = get(MovesetManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String movesetName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        MovesetHandle handle = internalBuilder.build(file, movesetName);

        if (handle == null)
            throwException("Failed to build moveset from: " + file.getAbsolutePath());

        movesetManager.addMoveset(handle);
    }

    // On-Demand \\

    void request(String movesetName) {

        File file = movesetName2File.get(movesetName);

        if (file == null)
            throwException("On-demand moveset load failed — not found in scan registry: \"" + movesetName + "\"");

        request(file);
    }
}
