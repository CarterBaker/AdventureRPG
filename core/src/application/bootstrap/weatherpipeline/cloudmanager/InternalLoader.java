package application.bootstrap.weatherpipeline.cloudmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.bootstrap.weatherpipeline.cloud.CloudHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the cloud JSON directory and loads all cloud archetype
     * definitions into CloudManager. Supports on-demand loading for clouds
     * not yet in the palette at runtime.
     */

    // Internal
    private File root;
    private CloudManager cloudManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.cloudManager = get(CloudManager.class);
    }

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.CLOUD_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Cloud root directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk cloud directory: " + root.getAbsolutePath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        CloudHandle cloudHandle = internalBuilder.build(file, root);

        if (cloudHandle != null)
            cloudManager.addCloud(cloudHandle);
    }

    // On-Demand \\

    void request(String cloudName) {

        File file = resourceName2File.get(cloudName);

        if (file == null)
            throwException("On-demand cloud load failed — no file found for: \"" + cloudName + "\"");

        request(file);
    }
}