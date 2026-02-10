package com.internal.bootstrap.entitypipeline.entityManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoadManager extends ManagerPackage {

    // Internal
    private File root;
    private EntityManager entityManager;
    private InternalBuildSystem internalBuildSystem;

    private int templateDataCount;

    // File Registry
    private Map<String, File> resourceName2File;

    // Base \\

    @Override
    protected void create() {
        this.root = new File(EngineSetting.ENTITY_JSON_PATH);
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.templateDataCount = 0;
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
    }

    @Override
    protected void get() {
        this.entityManager = get(EntityManager.class);
    }

    @Override
    protected void release() {
        this.internalBuildSystem = release(InternalBuildSystem.class);
    }

    // Template Management \\

    void loadTemplateData() {
        List<File> templateFiles = collectTemplateFiles();
        buildFileRegistry(templateFiles);
        processTemplateFiles(templateFiles);
    }

    // File Collection \\

    private List<File> collectTemplateFiles() {
        validateRootDirectory();

        Path basePath = root.toPath();

        try (var stream = Files.walk(basePath)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(this::isValidJsonFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throwException(
                    "Failed to list entity template files in directory: " + root.getAbsolutePath(), e);
        }

        return null;
    }

    private void validateRootDirectory() {
        if (!root.exists() || !root.isDirectory())
            throwException("Entity template JSON directory not found: " + root.getAbsolutePath());
    }

    private boolean isValidJsonFile(File file) {
        String extension = FileUtility.getExtension(file);
        return EngineSetting.JSON_FILE_EXTENSIONS.contains(extension);
    }

    // File Registry \\

    private void buildFileRegistry(List<File> templateFiles) {
        for (File file : templateFiles) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
        }
    }

    public File getFileByResourceName(String resourceName) {
        return resourceName2File.get(resourceName);
    }

    // File Processing \\

    private void processTemplateFiles(List<File> templateFiles) {
        for (File file : templateFiles)
            processTemplateFile(file);
    }

    public void processTemplateFile(File file) {
        String templateName = FileUtility.getPathWithFileNameWithoutExtension(root, file);

        try {
            int templateID = templateDataCount++;
            EntityData templateData = internalBuildSystem.buildTemplateData(root, file, templateID, this);

            if (templateData != null)
                createTemplateData(templateName, templateID, templateData);
        } catch (RuntimeException ex) {
            throwException(
                    "Failed to build entity template from file: " + file.getAbsolutePath(), ex);
        }
    }

    private void createTemplateData(String templateName, int templateID, EntityData templateData) {
        entityManager.addEntityTemplate(templateName, templateID, templateData);
    }
}