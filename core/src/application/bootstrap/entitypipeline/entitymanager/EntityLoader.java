package application.bootstrap.entitypipeline.entitymanager;

import java.io.File;

import application.bootstrap.entitypipeline.entity.EntityHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class EntityLoader extends LoaderPackage {

    /*
     * Scans the entity template JSON directory and loads all definitions into
     * EntityManager. IDs are derived from template names via RegistryUtility.
     * Supports on-demand loading for templates not yet in the palette at runtime.
     */

    // Internal
    private File root;
    private EntityManager entityManager;
    private EntityBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> templateName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ENTITY_JSON_PATH);
        this.templateName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Entity template directory not found: " + root.getAbsolutePath());

        for (File file : FileUtility.collectFiles(root, EngineSetting.JSON_FILE_EXTENSIONS)) {
            String templateName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            templateName2File.put(templateName, file);
            queueFile(file);
        }
    }

    @Override
    protected void create() {
        create(AppearanceBuilder.class);
        this.internalBuilder = create(EntityBuilder.class);
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