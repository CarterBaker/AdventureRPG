package program.bootstrap.shaderpipeline.ubomanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import program.core.engine.LoaderPackage;
import program.core.settings.EngineSetting;
import program.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Drives the UBO bootstrap sequence: walks the JSON directory in scan(),
 * assembles one UBO per load() call, and self-releases when the queue empties.
 */
class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private UBOManager uboManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> uboName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.UBO_JSON_PATH);
        this.uboName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "UBO directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        uboName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("UBO directory walk failed: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
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