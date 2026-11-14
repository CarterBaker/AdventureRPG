package application.bootstrap.worldpipeline.architecturemanager;

import java.io.File;

import application.bootstrap.worldpipeline.architecture.ArchitectureHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class ArchitectureLoader extends LoaderPackage {

    /*
     * Scans the architecture ARPG directory and loads every architecture into
     * ArchitectureManager, one per file, named by its path below the
     * directory. Supports on-demand loading by name, and requestAll()
     * so ArchitectureManager completes its palette before the first chunk
     * generates.
     */

    // Internal
    private File root;
    private ArchitectureManager architectureManager;
    private ArchitectureBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> architectureName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(ArchitectureBuilder.class);
    }

    @Override
    protected void get() {
        this.architectureManager = get(ArchitectureManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ARCHITECTURE_PATH);
        this.architectureName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Structure list root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String architectureName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            architectureName2File.put(architectureName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String architectureName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        ArchitectureHandle architectureHandle = internalBuilder.build(file, architectureName);

        architectureManager.addArchitectureHandle(architectureHandle);
    }

    // On-Demand \\

    void request(String architectureName) {

        if (architectureManager.hasArchitecture(architectureName))
            return;

        File file = architectureName2File.get(architectureName);

        if (file == null)
            throwException("On-demand architecture load failed — no file found for: \"" + architectureName + "\"");

        request(file);
    }
}
