package com.internal.bootstrap.entitypipeline.behaviormanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.internal.bootstrap.entitypipeline.behavior.BehaviorHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private BehaviorManager behaviorManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> behaviorName2File;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.BEHAVIOR_JSON_PATH);

        // File Registry
        this.behaviorName2File = new Object2ObjectOpenHashMap<>();

        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {

        // Internal
        this.behaviorManager = get(BehaviorManager.class);
    }

    @Override
    protected void scan() {

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
        }

        catch (IOException e) {
            throwException("BehaviorLoader failed to walk directory: ", e);
        }
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
            throwException("[BehaviorLoader] On-demand load failed — not found in scan registry: \""
                    + behaviorName + "\"");

        request(file);
    }
}