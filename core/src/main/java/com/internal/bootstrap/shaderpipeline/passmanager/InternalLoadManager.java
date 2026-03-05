package com.internal.bootstrap.shaderpipeline.passmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

/*
 * Drives the pass bootstrap sequence: file discovery in scan(), one pass
 * assembled per load() call, self-releases when queue empties.
 */
class InternalLoadManager extends LoaderPackage {

    // Internal
    private File root;
    private PassManager passManager;
    private InternalBuildSystem internalBuildSystem;
    private int passCount;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.PASS_JSON_PATH);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "Pass directory not found: " + root.getAbsolutePath());

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
            throwException("Failed to walk pass directory: " + root.getAbsolutePath(), e);
        }
    }

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
        this.passCount = 0;
    }

    @Override
    protected void get() {
        this.passManager = get(PassManager.class);
    }

    // Load \\

    @Override
    protected void load(File file) {
        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
        PassHandle pass = internalBuildSystem.build(file, resourceName, passCount++);
        passManager.addPass(pass);
    }

    // On-Demand Loading \\

    void request(String passName) {

        File file = resourceName2File.get(passName);

        if (file == null)
            throwException(
                    "On-demand pass load failed — resource not found in scan registry: \"" + passName + "\"");

        request(file);
    }
}