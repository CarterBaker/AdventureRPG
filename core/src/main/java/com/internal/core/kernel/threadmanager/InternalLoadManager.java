package com.internal.core.kernel.threadmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.internal.core.engine.LoaderPackage;
import com.internal.core.engine.settings.EngineSetting;
import com.internal.core.util.FileUtility;
import com.internal.core.util.JsonUtility;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class InternalLoadManager extends LoaderPackage {

    // Internal
    private File root;
    private InternalThreadManager internalThreadManager;
    private InternalBuildSystem internalBuildSystem;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;
    private Object2ObjectOpenHashMap<String, String> threadName2ResourceName;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.THREAD_DEFINITIONS);
        this.resourceName2File = new Object2ObjectOpenHashMap<>();
        this.threadName2ResourceName = new Object2ObjectOpenHashMap<>();

        FileUtility.verifyDirectory(root, "[ThreadManager] The root folder could not be verified");

        try (var stream = Files.walk(root.toPath())) {
            stream
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(f -> FileUtility.hasExtension(f, EngineSetting.JSON_FILE_EXTENSIONS))
                    .forEach(file -> {
                        String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
                        resourceName2File.put(resourceName, file);
                        preRegisterThreadNames(file, resourceName);
                        fileQueue.offer(file);
                    });
        } catch (IOException e) {
            throwException("[ThreadManager] Failed to walk thread definitions directory: ", e);
        }
    }

    @Override
    protected void create() {
        this.internalBuildSystem = create(InternalBuildSystem.class);
    }

    @Override
    protected void get() {
        this.internalThreadManager = get(InternalThreadManager.class);
    }

    // Pre-Registration \\

    /*
     * Peeks the JSON during scan to extract thread names and build the reverse
     * lookup before any load() fires. This is what allows on-demand requests
     * to resolve correctly — the name → file mapping must be complete before
     * the batch phase begins.
     */
    private void preRegisterThreadNames(File file, String resourceName) {
        try {
            JsonObject json = JsonUtility.loadJsonObject(file);
            JsonArray threads = json.getAsJsonArray("threads");
            if (threads == null)
                return;
            for (int i = 0; i < threads.size(); i++) {
                JsonObject threadDef = threads.get(i).getAsJsonObject();
                if (!threadDef.has("name"))
                    continue;
                String threadName = threadDef.get("name").getAsString();
                threadName2ResourceName.put(threadName, resourceName);
            }
        } catch (Exception e) {
            throwException("[ThreadManager] Failed to pre-register thread names from: " + file.getPath(), e);
        }
    }

    // Load \\

    @Override
    protected void load(File file) {

        JsonObject json = JsonUtility.loadJsonObject(file);
        if (!json.has("threads"))
            return;

        JsonArray threads = JsonUtility.validateArray(json, "threads");

        for (int i = 0; i < threads.size(); i++) {
            JsonObject threadDef = threads.get(i).getAsJsonObject();
            String threadName = JsonUtility.validateString(threadDef, "name");
            int threadSize = JsonUtility.validateInt(threadDef, "size");
            if (threadSize <= 0)
                throwException("Thread '" + threadName + "' has invalid size: " + threadSize);
            ThreadHandle handle = internalBuildSystem.build(threadName, threadSize);
            internalThreadManager.addThreadHandle(threadName, handle);
        }
    }

    // On-Demand Loading \\

    void request(String threadName) {
        String resourceName = threadName2ResourceName.get(threadName);
        if (resourceName == null)
            throwException(
                    "[InternalLoadManager] On-demand thread load failed — no file found for thread: \""
                            + threadName + "\"");
        request(resourceName2File.get(resourceName));
    }
}