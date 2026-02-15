package com.internal.core.kernel.threadmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.engine.ThreadHandle;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

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

        List<File> jsonFiles = collectJsonFiles();

        for (File file : jsonFiles)
            processJsonFile(file);
    }

    // File Collection \\

    private List<File> collectJsonFiles() {

        validateRootDirectory();

        Path basePath = root.toPath();

        try (var stream = Files.walk(basePath)) {

            return stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(this::isValidJsonFile)
                    .collect(Collectors.toList());
        }

        catch (IOException e) {
            throwException("Failed to collect thread definition files from: " + root.getAbsolutePath(), e);
        }

        return null;
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
        int threadSize = threadDef.get("size").getAsInt();

        if (threadSize <= 0)
            return;

        ThreadHandle threadHandle = internalBuildSystem.buildThreadHandle(threadName, threadSize);

        if (threadHandle != null)
            createThreadData(threadName, threadHandle);
    }

    private void createThreadData(String threadName, ThreadHandle threadHandle) {
        internalThreadManager.addThreadHandle(threadName, threadHandle);
    }
}
