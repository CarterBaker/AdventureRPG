package application.bootstrap.entitypipeline.featuremanager;

import java.io.File;

import application.bootstrap.entitypipeline.feature.FeatureHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class FeatureLoader extends LoaderPackage {

    /*
     * Scans the feature JSON directory and loads every appearance option
     * into FeatureManager. Supports on-demand loading for features not yet
     * in the palette — entity templates resolve their default features
     * through that path while they load.
     */

    // Internal
    private File root;
    private FeatureManager featureManager;
    private FeatureBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> featureName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.FEATURE_JSON_PATH);
        this.featureName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Feature JSON directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String featureName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            featureName2File.put(featureName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(FeatureBuilder.class);
    }

    @Override
    protected void get() {
        this.featureManager = get(FeatureManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String featureName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        FeatureHandle handle = internalBuilder.build(file, featureName);

        if (handle == null)
            throwException("Failed to build feature from: " + file.getAbsolutePath());

        featureManager.addFeature(handle);
    }

    // On-Demand \\

    void request(String featureName) {

        File file = featureName2File.get(featureName);

        if (file == null)
            throwException("On-demand feature load failed — not found in scan registry: \"" + featureName + "\"");

        request(file);
    }

    boolean hasFeatureFile(String featureName) {
        return featureName2File.containsKey(featureName);
    }
}
