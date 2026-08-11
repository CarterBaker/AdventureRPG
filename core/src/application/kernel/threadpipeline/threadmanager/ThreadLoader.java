package application.kernel.threadpipeline.threadmanager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.io.FileUtility;
import engine.util.io.JsonUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class ThreadLoader extends LoaderPackage {

    /*
     * Loads named thread pool definitions from JSON. "size" may be a fixed
     * integer or the literal string "auto", which resolves against
     * Runtime.getRuntime().availableProcessors() at load time — see
     * resolveThreadSize(). A copy-pasted fixed integer is either wasteful on
     * a big machine or actively harmful on a small one: for CPU-bound work
     * like chunk geometry building, threads past the core count add nothing
     * but context-switch overhead, and — combined with this engine's
     * tryAcquire-based chunk locking — a sharp rise in lock-contention
     * retries as concurrency increases. An optional "maxInFlight" caps how
     * many tasks may be queued-or-running on the pool at once; if omitted,
     * it defaults to a small multiple of the resolved thread count so the
     * pipeline can overlap without letting the executor's internal queue
     * grow without bound — see ThreadHandle.hasCapacity().
     */

    // Internal
    private File root;
    private ThreadManager internalThreadManager;
    private ThreadBuilder internalBuilder;

    // File Registry
    private Object2ObjectOpenHashMap<String, File> resourceName2File;
    private Object2ObjectOpenHashMap<String, String> threadName2ResourceName;

    // Base \\

    @Override
    protected void scan() {

        this.root = new File(EngineSetting.THREAD_CATALOG_PATH);
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
        this.internalBuilder = create(ThreadBuilder.class);
    }

    @Override
    protected void get() {
        this.internalThreadManager = get(ThreadManager.class);
    }

    // Pre-Registration \\

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
            int threadSize = resolveThreadSize(threadDef, threadName);
            int inFlightCapacity = resolveInFlightCapacity(threadDef, threadSize);
            ThreadHandle handle = internalBuilder.build(threadName, threadSize, inFlightCapacity);
            internalThreadManager.addThreadHandle(threadName, handle);
        }
    }

    // Sizing \\

    private int resolveThreadSize(JsonObject threadDef, String threadName) {

        if (!threadDef.has("size"))
            throwException("Thread '" + threadName + "' is missing required \"size\" field.");

        JsonElement sizeEl = threadDef.get("size");
        int resolved;

        if (sizeEl.isJsonPrimitive() && sizeEl.getAsJsonPrimitive().isString()) {

            String mode = sizeEl.getAsString();

            if (!mode.equalsIgnoreCase("auto"))
                throwException("Thread '" + threadName + "' has unrecognized size mode \"" + mode
                        + "\" — only \"auto\" or a positive integer are valid.");

            int available = Runtime.getRuntime().availableProcessors();
            resolved = Math.max(
                    EngineSetting.MIN_AUTO_THREAD_POOL_SIZE,
                    available - EngineSetting.AUTO_THREAD_POOL_RESERVED_CORES);
        } else {

            resolved = sizeEl.getAsInt();

            if (resolved <= 0)
                throwException("Thread '" + threadName + "' has invalid size: " + resolved);
        }

        if (resolved > EngineSetting.MAX_THREAD_POOL_SIZE) {
            errorLog("[ThreadManager] Thread '" + threadName + "' requested " + resolved
                    + " threads, exceeding MAX_THREAD_POOL_SIZE (" + EngineSetting.MAX_THREAD_POOL_SIZE
                    + "). More OS threads than the CPU can run concurrently adds no throughput for "
                    + "compute-bound work — only context-switch and lock-contention overhead. Clamping.");
            resolved = EngineSetting.MAX_THREAD_POOL_SIZE;
        }

        return resolved;
    }

    private int resolveInFlightCapacity(JsonObject threadDef, int threadSize) {

        if (threadDef.has("maxInFlight"))
            return threadDef.get("maxInFlight").getAsInt();

        return threadSize * EngineSetting.DEFAULT_IN_FLIGHT_MULTIPLIER;
    }

    // On-Demand \\

    void request(String threadName) {
        String resourceName = threadName2ResourceName.get(threadName);
        if (resourceName == null)
            throwException(
                    "[InternalLoadManager] On-demand thread load failed — no file found for thread: \""
                            + threadName + "\"");
        request(resourceName2File.get(resourceName));
    }
}