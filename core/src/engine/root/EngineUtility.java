package engine.root;

import java.util.concurrent.Future;

import application.kernel.threadpipeline.syncconsumer.AsyncStructConsumer;
import application.kernel.threadpipeline.syncconsumer.AsyncStructConsumerMulti;
import application.kernel.threadpipeline.syncconsumer.BiSyncAsyncConsumer;
import application.kernel.threadpipeline.syncconsumer.SyncStructConsumer;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.threadpipeline.threadmanager.InternalThreadManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.util.display.camera.CameraData;
import engine.util.display.camera.CameraInstance;
import engine.util.display.camera.OrthographicCameraData;
import engine.util.display.camera.OrthographicCameraInstance;

public abstract class EngineUtility {

    /*
     * EngineUtility provides engine-level diagnostics, exception handling,
     * and convenience access to static utility classes.
     *
     * Core kernel references are assigned during their respective awake()
     * phases, making thread submission and camera creation available globally
     * without passing engine references through call chains.
     */

    // Internal \\

    protected static final UtilityPackage UTILITY = new UtilityPackage() {
    };

    static EnginePackage engine;
    static InternalThreadManager threadManager;
    static WindowManager windowManager;

    public static void assignThreadManager(InternalThreadManager input) {

        if (threadManager != null)
            throwException("Illegal reassignment of thread manager attempted during runtime");

        threadManager = input;
    }

    public static void assignWindowManager(WindowManager input) {

        if (windowManager != null)
            throwException("Illegal reassignment of window manager attempted during runtime");

        windowManager = input;
    }

    public static WindowManager getWindowManager() {
        return windowManager;
    }

    // Thread \\

    static ThreadHandle getThreadHandle(String threadName) {
        return threadManager.getThreadHandleFromThreadName(threadName);
    }

    static Future<?> executeAsync(ThreadHandle handle, Runnable task) {
        return threadManager.executeAsync(handle, task);
    }

    static <T extends AsyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T asyncStruct,
            AsyncStructConsumer<T> consumer) {
        return threadManager.executeAsync(handle, asyncStruct, consumer);
    }

    static Future<?> executeAsync(
            ThreadHandle handle,
            AsyncStructConsumerMulti consumer,
            AsyncContainerPackage... asyncStructs) {
        return threadManager.executeAsync(handle, consumer, asyncStructs);
    }

    static <T extends SyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T syncStruct,
            SyncStructConsumer<T> consumer) {
        return threadManager.executeAsync(handle, syncStruct, consumer);
    }

    static <T extends AsyncContainerPackage, S extends SyncContainerPackage> Future<?> executeAsync(
            ThreadHandle handle,
            T asyncStruct,
            S syncStruct,
            BiSyncAsyncConsumer<T, S> consumer) {
        return threadManager.executeAsync(handle, asyncStruct, syncStruct, consumer);
    }

    // Camera \\

    static CameraInstance createCamera(float fov, float width, float height) {
        CameraInstance instance = engine.createInstance(CameraInstance.class);
        instance.constructor(new CameraData(fov, width, height));
        return instance;
    }

    static OrthographicCameraInstance createOrthographicCamera(float width, float height) {
        OrthographicCameraInstance instance = engine.createInstance(OrthographicCameraInstance.class);
        instance.constructor(new OrthographicCameraData(width, height));
        return instance;
    }

    // Debug \\

    public static void debug() {
        UTILITY.debug();
    }

    public static void debug(Object input) {
        UTILITY.debug(input);
    }

    public static void timeStampDebug(Object input) {
        UTILITY.timeStampDebug(input);
    }

    // Log \\

    public static void log(Object input) {
        UTILITY.log(input);
    }

    public static void errorLog(Object input) {
        UTILITY.errorLog(input);
    }

    public static void timeStampLog(Object input) {
        UTILITY.timeStampLog(input);
    }

    // Exception Handling \\

    public static <T> T throwException() {
        return UTILITY.throwException();
    }

    public static <T> T throwException(String message) {
        return UTILITY.throwException(message);
    }

    public static <T> T throwException(Throwable cause) {
        return UTILITY.throwException(cause);
    }

    public static <T> T throwException(String message, Throwable cause) {
        return UTILITY.throwException(message, cause);
    }

    public static <T> T throwException(Object input) {
        return UTILITY.throwException(input);
    }

    public static <T> T throwException(Object input, Throwable cause) {
        return UTILITY.throwException(input, cause);
    }
}
