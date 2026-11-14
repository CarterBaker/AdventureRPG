package application.kernel.threadpipeline.threadmanager;

import java.io.File;

import application.kernel.threadpipeline.thread.ThreadHandle;
import engine.root.EngineSetting;
import engine.root.LoaderPackage;
import engine.util.arpg.ArpgArrayStruct;
import engine.util.arpg.ArpgElementStruct;
import engine.util.arpg.ArpgObjectStruct;
import engine.util.arpg.ArpgUtility;
import engine.util.io.FileUtility;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

class ThreadLoader extends LoaderPackage {

    /*
     * Loads named thread pool definitions from ARPG. "size" is a fixed count or
     * "auto", resolved against the processor count, and the optional
     * "maxInFlight" caps queued and running tasks, defaulting to a small
     * multiple of the thread count.
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

        for (File file : FileUtility.collectFiles(root, EngineSetting.ARPG_FILE_EXTENSIONS)) {
            String resourceName = FileUtility.getPathWithFileNameWithoutExtension(root, file);
            resourceName2File.put(resourceName, file);
            preRegisterThreadNames(file, resourceName);
            queueFile(file);
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
            ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
            ArpgArrayStruct threads = arpg.getAsArray("threads");
            if (threads == null)
                return;
            for (int i = 0; i < threads.size(); i++) {
                ArpgObjectStruct threadDef = threads.get(i).getAsObject();
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

        ArpgObjectStruct arpg = ArpgUtility.loadObject(file);
        if (!arpg.has("threads"))
            return;

        ArpgArrayStruct threads = ArpgUtility.validateArray(arpg, "threads");

        for (int i = 0; i < threads.size(); i++) {
            ArpgObjectStruct threadDef = threads.get(i).getAsObject();
            String threadName = ArpgUtility.validateString(threadDef, "name");
            int threadSize = resolveThreadSize(threadDef, threadName);
            int inFlightCapacity = resolveInFlightCapacity(threadDef, threadSize);
            ThreadHandle handle = internalBuilder.build(threadName, threadSize, inFlightCapacity);
            internalThreadManager.addThreadHandle(threadName, handle);
        }
    }

    // Sizing \\

    private int resolveThreadSize(ArpgObjectStruct threadDef, String threadName) {

        if (!threadDef.has("size"))
            throwException("Thread '" + threadName + "' is missing required \"size\" field.");

        ArpgElementStruct sizeEl = threadDef.get("size");
        int resolved;

        if (sizeEl.isValue() && sizeEl.getAsValue().isString()) {

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

    private int resolveInFlightCapacity(ArpgObjectStruct threadDef, int threadSize) {

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