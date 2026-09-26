package application.bootstrap.entitypipeline.behaviormanager;

import java.io.File;

import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class BehaviorLoader extends LoaderPackage {

    /*
     * Scans the behavior JSON directory and loads all behavior definitions into
     * BehaviorManager. Supports on-demand loading for behaviors not yet in the
     * palette at runtime.
     */

    // Internal
    private File root;
    private BehaviorManager behaviorManager;
    private BehaviorBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> behaviorName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BEHAVIOR_JSON_PATH);
        this.behaviorName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Behavior directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String behaviorName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            behaviorName2File.put(behaviorName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(BehaviorBuilder.class);
    }

    @Override
    protected void get() {
        this.behaviorManager = get(BehaviorManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String behaviorName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        BehaviorHandle handle = internalBuilder.build(file, behaviorName);

        if (handle == null)
            throwException("Failed to build behavior from: " + file.getAbsolutePath());

        behaviorManager.addBehavior(handle);
    }

    // On-Demand \\

    void request(String behaviorName) {

        File file = behaviorName2File.get(behaviorName);

        if (file == null)
            throwException("On-demand behavior load failed — not found in scan registry: \"" + behaviorName + "\"");

        request(file);
    }
}