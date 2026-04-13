package application.bootstrap.shaderpipeline.materialmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import application.core.engine.LoaderPackage;
import application.core.settings.EngineSetting;
import application.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the material directory, populates the file queue, and drives
     * InternalBuilder one file per load() call. Self-destructs when the
     * queue is exhausted.
     */

    // Internal
    private File root;
    private MaterialManager materialManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> materialName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.MATERIAL_JSON_PATH);
        this.materialName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root,
                "Material JSON directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String materialName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        materialName2File.put(materialName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk material directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.materialManager = get(MaterialManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String materialName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {
            internalBuilder.build(file, materialName);
        } catch (RuntimeException e) {
            throwException("Failed to process material file: " + file.getAbsolutePath(), e);
        }
    }

    // On-Demand \\

    void request(String materialName) {

        File file = materialName2File.get(materialName);

        if (file == null)
            throwException("On-demand material load failed — not found in scan registry: \"" + materialName + "\"");

        request(file);
    }
}