package program.bootstrap.shaderpipeline.passmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import program.bootstrap.shaderpipeline.pass.PassHandle;
import program.core.engine.LoaderPackage;
import program.core.settings.EngineSetting;
import program.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Drives the pass bootstrap sequence: directory walked in scan(), one pass
 * assembled per load() call, self-releases when the queue empties.
 */
class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private PassManager passManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> passName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.PASS_JSON_PATH);
        this.passName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Pass directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        passName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk pass directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
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