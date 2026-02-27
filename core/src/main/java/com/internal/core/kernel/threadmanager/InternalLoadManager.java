package com.internal.core.kernel.threadmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

class InternalLoadManager extends ManagerPackage {

    // Internal
    private File root;
    private InternalThreadManager internalThreadManager;
    private InternalBuildSystem internalBuildSystem;

    // Base \\

    @Override
    protected void create() {
        this.root = new File(EngineSetting.THREAD_DEFINITIONS);
    }

    @Override
    protected void get() {
        this.internalThreadManager = get(InternalThreadManager.class);
        this.internalBuildSystem = get(InternalBuildSystem.class);
    }

    // Thread Data Management \\

    void loadThreadData() {
        ObjectArrayList<File> jsonFiles = collectJsonFiles();
        for (int i = 0; i < jsonFiles.size(); i++)
            processJsonFile(jsonFiles.get(i));
    }

    // File Collection \\

    private ObjectArrayList<File> collectJsonFiles() {
        validateRootDirectory();
        ObjectArrayList<File> result = new ObjectArrayList<>();
        try (var stream = Files.walk(root.toPath())) {
            stream.filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(this::isValidJsonFile)
                    .forEach(result::add);
            return result;
        } catch (IOException e) {
            return throwException("Failed to collect thread definition files from: " + root.getAbsolutePath(), e);
        }
    }

    private void validateRootDirectory() {
        if (!root.exists() || !root.isDirectory())
            throwException("Thread definitions directory not found: " + root.getAbsolutePath());
    }

    private boolean isValidJsonFile(File file) {
        return FileUtility.hasExtension(file, EngineSetting.JSON_FILE_EXTENSIONS);
    }

    // File Processing \\

    private void processJsonFile(File file) {
        JsonObject json = JsonUtility.loadJsonObject(file);
        if (!json.has("threads"))
            return;

        JsonArray threads = JsonUtility.validateArray(json, "threads");
        for (int i = 0; i < threads.size(); i++)
            processThreadDefinition(threads.get(i).getAsJsonObject());
    }

    private void processThreadDefinition(JsonObject threadDef) {
        String threadName = JsonUtility.validateString(threadDef, "name");
        int threadSize = JsonUtility.validateInt(threadDef, "size");

        if (threadSize <= 0)
            throwException("Thread '" + threadName + "' has invalid size: " + threadSize);

        ThreadHandle threadHandle = internalBuildSystem.buildThreadHandle(threadName, threadSize);
        internalThreadManager.addThreadHandle(threadName, threadHandle);
    }
}