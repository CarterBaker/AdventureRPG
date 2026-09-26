package engine.root;

import java.util.concurrent.Future;

import application.kernel.frameratepipeline.frameratemanager.FrameRateManager;
import application.kernel.inputpipeline.inputmanager.InputManager;
import application.kernel.threadpipeline.thread.ThreadHandle;
import application.kernel.threadpipeline.threadmanager.ThreadManager;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.assets.camera.CameraData;
import engine.assets.camera.CameraInstance;
import engine.assets.camera.OrthographicCameraData;
import engine.assets.camera.OrthographicCameraInstance;

public abstract class EngineUtility {

    /*
     * Static base for every *Utility class. Provides engine-wide logging and
     * throwException(), and holds the kernel managers assigned during their
     * awake() so thread submission and camera creation are reachable without
     * passing the engine around.
     */

    // Internal \\

    protected static final UtilityPackage UTILITY = new UtilityPackage() {
    };

    static EnginePackage engine;
    static InputManager inputManager;
    static ThreadManager threadManager;
    static WindowManager windowManager;
    static FrameRateManager frameRateManager;

    public static void assignInputManager(InputManager input) {

        if (inputManager != null)
            throwException("Illegal reassignment of input manager attempted during runtime");

        inputManager = input;
    }

    public static void assignThreadManager(ThreadManager input) {

        if (threadManager != null)
            throwException("Illegal reassignment of thread manager attempted during runtime");

        threadManager = input;
    }

    public static void assignWindowManager(WindowManager input) {

        if (windowManager != null)
            throwException("Illegal reassignment of window manager attempted during runtime");

        windowManager = input;
    }

    public static void assignFrameRateManager(FrameRateManager input) {

        if (frameRateManager != null)
            throwException("Illegal reassignment of frame rate manager attempted during runtime");

        frameRateManager = input;
    }

    // Thread \\

    static ThreadHandle getThreadHandle(String threadName) {
        return threadManager.getThreadHandleFromThreadName(threadName);
    }

    static Future<?> executeAsync(ThreadHandle handle, Runnable task) {
        return threadManager.executeAsync(handle, task);
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