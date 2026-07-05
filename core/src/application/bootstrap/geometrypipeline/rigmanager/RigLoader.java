package application.bootstrap.geometrypipeline.rigmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.geometrypipeline.rig.RigHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class RigLoader extends LoaderPackage {

    /*
     * Scans the rig JSON directory and loads every rig template into
     * RigManager. Supports on-demand loading for rigs not yet in the
     * palette at runtime.
     */

    // Internal
    private File root;
    private RigManager rigManager;
    private RigBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> rigName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.RIG_JSON_PATH);
        this.rigName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Rig JSON directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String rigName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        rigName2File.put(rigName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk rig directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(RigBuilder.class);
    }

    @Override
    protected void get() {
        this.rigManager = get(RigManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String rigName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        RigHandle handle = internalBuilder.build(file, rigName);

        if (handle == null)
            throwException("Failed to build rig from: " + file.getAbsolutePath());

        rigManager.addRig(rigName, handle);
    }

    // On-Demand \\

    void request(String rigName) {

        File file = rigName2File.get(rigName);

        if (file == null)
            throwException("On-demand rig load failed — not found in scan registry: \"" + rigName + "\"");

        request(file);
    }
}