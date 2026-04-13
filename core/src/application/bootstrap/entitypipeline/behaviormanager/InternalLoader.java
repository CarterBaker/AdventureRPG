package application.bootstrap.entitypipeline.behaviormanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.entitypipeline.behavior.BehaviorHandle;
import engine.root.LoaderPackage;
import engine.settings.EngineSetting;
import engine.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the behavior JSON directory and loads all behavior definitions into
     * BehaviorManager. Supports on-demand loading for behaviors not yet in the
     * palette at runtime.
     */

    // Internal
    private File root;
    private BehaviorManager behaviorManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> behaviorName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.BEHAVIOR_JSON_PATH);
        this.behaviorName2File = new Object2ObjectOpenHashMap<>();

        if (!root.exists() || !root.isDirectory())
            throwException("Behavior directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String behaviorName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        behaviorName2File.put(behaviorName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk behavior directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
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