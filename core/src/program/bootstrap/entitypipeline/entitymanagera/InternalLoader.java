package program.bootstrap.entitypipeline.entitymanagera;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import program.bootstrap.entitypipeline.entity.EntityHandle;
import program.core.engine.LoaderPackage;
import program.core.settings.EngineSetting;
import program.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    /*
     * Scans the entity template JSON directory and loads all definitions into
     * EntityManager. IDs are derived from template names via RegistryUtility.
     * Supports on-demand loading for templates not yet in the palette at runtime.
     */

    // Internal
    private File root;
    private EntityManager entityManager;
    private InternalBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> templateName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ENTITY_JSON_PATH);
        this.templateName2File = new Object2ObjectOpenHashMap<>();

        if (!root.exists() || !root.isDirectory())
            throwException("Entity template directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String templateName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        templateName2File.put(templateName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("Failed to walk entity directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {
        this.entityManager = get(EntityManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {

        String templateName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        EntityHandle entityHandle = internalBuilder.build(file);

        if (entityHandle == null)
            throwException("Failed to build entity template from: " + file.getAbsolutePath());

        entityManager.addEntityTemplate(templateName, entityHandle);
    }

    // On-Demand \\

    void request(String templateName) {

        File file = templateName2File.get(templateName);

        if (file == null)
            throwException("On-demand entity load failed — not found in scan registry: \"" + templateName + "\"");

        request(file);
    }
}