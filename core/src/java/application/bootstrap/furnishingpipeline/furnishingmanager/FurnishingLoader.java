package application.bootstrap.furnishingpipeline.furnishingmanager;

import java.io.File;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class FurnishingLoader extends LoaderPackage {

    /*
     * Scans the furnishing ARPG directory and loads every furnishing table
     * into FurnishingManager, one table per file, named by its path below the
     * directory. Supports on-demand loading by furnishing name, and
     * requestAll() so FurnishingManager completes its palette before anything
     * is furnished.
     */

    // Internal
    private File root;
    private FurnishingManager furnishingManager;
    private FurnishingBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> furnishingName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(FurnishingBuilder.class);
    }

    @Override
    protected void get() {
        this.furnishingManager = get(FurnishingManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.FURNISHING_PATH);
        this.furnishingName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Furnishing root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String furnishingName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            furnishingName2File.put(furnishingName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String furnishingName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        FurnishingHandle furnishingHandle = internalBuilder.build(file, furnishingName);

        furnishingManager.addFurnishingHandle(furnishingHandle);
    }

    // On-Demand \\

    void request(String furnishingName) {

        if (furnishingManager.hasFurnishing(furnishingName))
            return;

        File file = furnishingName2File.get(furnishingName);

        if (file == null)
            throwException("On-demand furnishing load failed — no file found for: \"" + furnishingName + "\"");

        request(file);
    }
}
