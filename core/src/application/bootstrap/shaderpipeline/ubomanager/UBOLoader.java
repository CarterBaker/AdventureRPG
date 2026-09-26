package application.bootstrap.shaderpipeline.ubomanager;

import java.io.File;

import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class UBOLoader extends LoaderPackage {

    /*
     * Drives the UBO bootstrap sequence: walks the JSON directory in scan(),
     * assembles one UBO per load() call, and self-releases when the queue empties.
     */

    // Internal
    private File root;
    private UBOManager uboManager;
    private UBOBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> uboName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.UBO_JSON_PATH);
        this.uboName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "UBO directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            uboName2File.put(resourceName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(UBOBuilder.class);
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        uboManager.buildBuffer(internalBuilder.parse(file));
    }

    // On-Demand \\

    void request(String blockName) {

        File file = uboName2File.get(blockName);

        if (file == null)
            throwException("On-demand UBO load failed — not found in scan registry: \"" + blockName + "\"");

        request(file);
    }
}