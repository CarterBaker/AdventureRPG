package com.internal.bootstrap.entitypipeline.entitymanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import com.internal.bootstrap.entitypipeline.entity.EntityHandle;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoader extends LoaderPackage {

    // Internal
    private File root;
    private EntityManager entityManager;
    private InternalBuilder internalBuilder;
    private int templateCount;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> templateName2File;

    // Base \\

    @Override
    protected void create() {

        // Internal
        this.root = new File(EngineSetting.ENTITY_JSON_PATH);
        this.templateCount = 0;

        // File Registry
        this.templateName2File = new Object2ObjectOpenHashMap<>();
        this.internalBuilder = create(InternalBuilder.class);
    }

    @Override
    protected void get() {

        // Internal
        this.entityManager = get(EntityManager.class);
    }

    @Override
    protected void scan() {

        if (!root.exists() || !root.isDirectory())
            throwException("Entity template JSON directory not found: " + root.getAbsolutePath());

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
        }

        catch (IOException e) {
            throwException("EntityLoader failed to walk directory: ", e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        String templateName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        int templateID = templateCount++;
        EntityHandle entityHandle = internalBuilder.build(file);

        if (entityHandle == null)
            throwException("Failed to build entity template from: " + file.getAbsolutePath());

        entityManager.addEntityTemplate(templateName, templateID, entityHandle);
    }

    // On-Demand \\

    void request(String templateName) {

        File file = templateName2File.get(templateName);

        if (file == null)
            throwException("On-demand entity load failed — not found in scan registry: \"" + templateName + "\"");

        request(file);
    }
}