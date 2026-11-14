package application.bootstrap.worldpipeline.coveringmanager;

import java.io.File;

import application.bootstrap.worldpipeline.covering.CoveringHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class CoveringLoader extends LoaderPackage {

    /*
     * Scans the covering ARPG directory and loads every covering into
     * CoveringManager. Supports on-demand loading by covering name, and
     * requestAll() so CoveringManager can complete its palette, and the
     * table the surface shader reads, before the first chunk generates. The
     * path below the directory, without its extension, is the covering's
     * name.
     */

    // Internal
    private File root;
    private CoveringManager coveringManager;
    private CoveringBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> coveringName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(CoveringBuilder.class);
    }

    @Override
    protected void get() {
        this.coveringManager = get(CoveringManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.COVERING_PATH);
        this.coveringName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Covering root directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String coveringName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            coveringName2File.put(coveringName, file);
            queueFile(file);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String coveringName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        CoveringHandle coveringHandle = internalBuilder.build(file, coveringName);

        if (coveringHandle == null)
            throwException("Failed to build covering from: " + file.getAbsolutePath());

        coveringManager.addCoveringHandle(coveringHandle);
    }

    // On-Demand \\

    void request(String coveringName) {

        if (coveringManager.hasCovering(coveringName))
            return;

        File file = coveringName2File.get(coveringName);

        if (file == null)
            throwException("On-demand covering load failed — no file found for: \"" + coveringName + "\"");

        request(file);
    }
}
