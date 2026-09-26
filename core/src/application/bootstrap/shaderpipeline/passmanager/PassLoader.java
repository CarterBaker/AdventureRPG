package application.bootstrap.shaderpipeline.passmanager;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class PassLoader extends LoaderPackage {

    /*
     * Drives the pass bootstrap sequence: directory walked in scan(), one pass
     * assembled per load() call, self-releases when the queue empties.
     */

    // Internal
    private File root;
    private PassManager passManager;
    private PassBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> passName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.PASS_JSON_PATH);
        this.passName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Pass directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            passName2File.put(resourceName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(PassBuilder.class);
    }

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        String passName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        passManager.addPassHandle(internalBuilder.build(file, passName));
    }

    // On-Demand \\

    void request(String passName) {

        File file = passName2File.get(passName);

        if (file == null)
            throwException("On-demand pass load failed — not found in scan registry: \"" + passName + "\"");

        request(file);
    }
}