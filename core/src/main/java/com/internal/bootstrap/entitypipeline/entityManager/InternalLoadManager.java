package com.internal.bootstrap.entitypipeline.entitymanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoadManager extends LoaderPackage {

    // Internal
    private File root;
    private EntityManager entityManager;
    private InternalBuildSystem internalBuildSystem;
    private int templateDataCount;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.ENTITY_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        if (!root.exists() || !root.isDirectory())
            throwException("Entity template JSON directory not found: " + root.getAbsolutePath());

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> EngineSetting.JSON_FILE_EXTENSIONS.contains(FileUtility.getExtension(f)))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("EntityManager failed to walk directory: ", e);
        }
    }

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.templateDataCount = 0;
    }

    @Override
    protected void get() {
        this.entityManager = get(EntityManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        String templateName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        try {
            int templateID = templateDataCount++;
            EntityData templateData = internalBuildSystem.build(root, file, templateID);
            if (templateData != null)
                entityManager.addEntityTemplate(templateName, templateID, templateData);
        } catch (RuntimeException e) {
            throwException("Failed to build entity template from file: " + file.getAbsolutePath(), e);
        }
    }

    // On-Demand Loading \\

    void request(String templateName) {

        File file = resourceName2File.get(templateName);

        if (file == null)
            throwException(
                    "On-demand entity load failed — resource not found in scan registry: \"" + templateName + "\"");

        request(file);
    }
}