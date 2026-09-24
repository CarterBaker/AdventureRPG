package engine.lwjgl3;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;

import engine.input.Input;
import engine.root.EngineContext;
import engine.root.EnginePackage;
import engine.root.EngineSetting;
import engine.root.EngineUtility;

import java.nio.IntBuffer;
import java.util.function.BooleanSupplier;

public class Lwjgl3Application {

    /*
     * Entry point for the LWJGL3 backend. Owns the main GLFW window and primary
     * GL context. The loop advances the engine tick and pumps input — all context
     * switching, drawing, and buffer swapping are driven by RenderManager uniformly
     * across every window, main and secondary alike.
     *
     * The main window is created hidden and fitted inside the work area of the
     * monitor it was saved on before it is shown, so a stale, oversized, or
     * off-screen saved placement never leaves its title bar out of reach.
     */

    // Internal
    private final long mainHandle;
    private final EnginePackage engine;
    private final Lwjgl3Display display;
    private final Lwjgl3Input input;
    private final int glMajor;
    private final int glMinor;
    private final int swapInterval;

    // Secondary Windows
    private final LongArrayList secondaryHandles;
    private final Long2ObjectOpenHashMap<Lwjgl3Input> handle2Input;

    // State
    private boolean running;

    // Scratch buffers — reused to avoid per-call allocation
    private final IntBuffer posScratchX = BufferUtils.createIntBuffer(1);
    private final IntBuffer posScratchY = BufferUtils.createIntBuffer(1);
    private final IntBuffer sizeScratchW = BufferUtils.createIntBuffer(1);
    private final IntBuffer sizeScratchH = BufferUtils.createIntBuffer(1);
    private final IntBuffer areaScratchX = BufferUtils.createIntBuffer(1);
    private final IntBuffer areaScratchY = BufferUtils.createIntBuffer(1);
    private final IntBuffer areaScratchW = BufferUtils.createIntBuffer(1);
    private final IntBuffer areaScratchH = BufferUtils.createIntBuffer(1);
    private final IntBuffer frameScratchLeft = BufferUtils.createIntBuffer(1);
    private final IntBuffer frameScratchTop = BufferUtils.createIntBuffer(1);
    private final IntBuffer frameScratchRight = BufferUtils.createIntBuffer(1);
    private final IntBuffer frameScratchBottom = BufferUtils.createIntBuffer(1);

    public Lwjgl3Application(
            EnginePackage engine,
            Lwjgl3Configuration config,
            Lwjgl3WindowPlatform platform) {

        this.secondaryHandles = new LongArrayList();
        this.handle2Input = new Long2ObjectOpenHashMap<>();
        this.running = true;

        if (!GLFW.glfwInit())
            EngineUtility.throwException("Unable to initialize GLFW");

        this.glMajor = config.getGlMajor();
        this.glMinor = config.getGlMinor();
        this.swapInterval = config.isVsync() ? 1 : 0;
        applyWindowHints(glMajor, glMinor);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);

        long monitor = config.isFullscreen() ? GLFW.glfwGetPrimaryMonitor() : 0L;
        this.mainHandle = GLFW.glfwCreateWindow(config.width, config.height, config.title, monitor, 0L);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);

        if (mainHandle == 0L) {
            GLFW.glfwTerminate();
            EngineUtility.throwException("Failed to create GLFW window");
        }

        if (!config.isFullscreen())
            placeMainWindow(config);

        GLFW.glfwMakeContextCurrent(mainHandle);
        GLFW.glfwSwapInterval(swapInterval);
        GL.createCapabilities();

        sizeScratchW.clear();
        sizeScratchH.clear();
        GLFW.glfwGetFramebufferSize(mainHandle, sizeScratchW, sizeScratchH);

        this.input = new Lwjgl3Input(mainHandle);
        this.input.initCursors();
        this.display = new Lwjgl3Display(sizeScratchW.get(0), sizeScratchH.get(0), config.isFullscreen());
        this.engine = engine;

        display.setMainHandle(mainHandle);
        captureWindowedBounds();

        Lwjgl3GL gl = new Lwjgl3GL();
        EngineContext.display = display;
        EngineContext.input = input;
        EngineContext.gl20 = gl;
        EngineContext.gl30 = gl;
        EngineContext.gl40 = gl;

        registerCallbacks(mainHandle, input, config.getCloseCallback());
        registerPlacementCallbacks();
        GLFW.glfwShowWindow(mainHandle);

        if (config.isMaximized() && !config.isFullscreen())
            GLFW.glfwMaximizeWindow(mainHandle);

        platform.setApplication(this);

        loop();

        for (int i = 0; i < secondaryHandles.size(); i++)
            GLFW.glfwDestroyWindow(secondaryHandles.getLong(i));

        for (Lwjgl3Input value : handle2Input.values())
            value.destroyCursors();
        input.destroyCursors();

        GLFW.glfwDestroyWindow(mainHandle);
        GLFW.glfwTerminate();
    }

    // Internal \\

    private static void applyWindowHints(int glMajor, int glMinor) {
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, glMajor);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, glMinor);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
    }

    private void registerCallbacks(long handle, Lwjgl3Input inp, BooleanSupplier closeCallback) {

        GLFW.glfwSetFramebufferSizeCallback(handle, (w, width, height) -> {
            if (handle == mainHandle)
                display.setSize(width, height);
        });
        GLFW.glfwSetCursorPosCallback(handle, (w, x, y) -> inp.onCursor(x, y));
        GLFW.glfwSetMouseButtonCallback(handle, (w, b, a, m) -> inp.onMouseButton(b, a));
        GLFW.glfwSetScrollCallback(handle, (w, dx, dy) -> inp.onScroll(dx, dy));
        GLFW.glfwSetKeyCallback(handle, (w, k, s, a, m) -> inp.onKey(k, a));
        GLFW.glfwSetCharCallback(handle, (w, cp) -> inp.onChar(cp));

        if (closeCallback != null)
            GLFW.glfwSetWindowCloseCallback(handle, w -> {
                if (!closeCallback.getAsBoolean())
                    GLFW.glfwSetWindowShouldClose(w, false);
            });
    }

    private void registerPlacementCallbacks() {
        GLFW.glfwSetWindowPosCallback(mainHandle, (w, x, y) -> onWindowMoved(w));
        GLFW.glfwSetWindowSizeCallback(mainHandle, (w, width, height) -> captureWindowedBounds());
        GLFW.glfwSetWindowMaximizeCallback(mainHandle, (w, maximized) -> display.setMaximized(maximized));
    }

    private void loop() {

        long last = System.nanoTime();

        while (running && !GLFW.glfwWindowShouldClose(mainHandle)) {

            long now = System.nanoTime();
            float delta = (now - last) / 1_000_000_000f;
            display.setDelta(delta);
            last = now;

            input.endFrame();

            for (int i = 0; i < secondaryHandles.size(); i++)
                handle2Input.get(secondaryHandles.getLong(i)).endFrame();

            GLFW.glfwPollEvents();
            engine.execute(delta);
        }

        engine.shutdown();
    }

    // Placement \\

    private void placeMainWindow(Lwjgl3Configuration config) {

        long monitor = resolvePlacementMonitor(config);

        if (monitor == 0L || !readMonitorWorkarea(monitor))
            return;

        int areaX = areaScratchX.get(0);
        int areaY = areaScratchY.get(0);
        int areaWidth = areaScratchW.get(0);
        int areaHeight = areaScratchH.get(0);

        frameScratchLeft.clear();
        frameScratchTop.clear();
        frameScratchRight.clear();
        frameScratchBottom.clear();
        GLFW.glfwGetWindowFrameSize(mainHandle, frameScratchLeft, frameScratchTop, frameScratchRight,
                frameScratchBottom);

        int frameLeft = frameScratchLeft.get(0);
        int frameTop = frameScratchTop.get(0);
        int frameRight = frameScratchRight.get(0);
        int frameBottom = frameScratchBottom.get(0);

        int maxWidth = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, areaWidth - frameLeft - frameRight);
        int maxHeight = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, areaHeight - frameTop - frameBottom);
        int width = Math.clamp(config.width, EngineSetting.MIN_WINDOW_DIMENSION, maxWidth);
        int height = Math.clamp(config.height, EngineSetting.MIN_WINDOW_DIMENSION, maxHeight);

        int minX = areaX + frameLeft;
        int minY = areaY + frameTop;
        int maxX = Math.max(minX, areaX + areaWidth - frameRight - width);
        int maxY = Math.max(minY, areaY + areaHeight - frameBottom - height);

        int x = hasWindowPosition(config)
                ? Math.clamp(config.getWindowX(), minX, maxX)
                : minX + (maxX - minX) / 2;
        int y = hasWindowPosition(config)
                ? Math.clamp(config.getWindowY(), minY, maxY)
                : minY + (maxY - minY) / 2;

        GLFW.glfwSetWindowSize(mainHandle, width, height);
        GLFW.glfwSetWindowPos(mainHandle, x, y);
    }

    private long resolvePlacementMonitor(Lwjgl3Configuration config) {

        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        PointerBuffer monitors = GLFW.glfwGetMonitors();

        if (!hasWindowPosition(config) || monitors == null)
            return primaryMonitor;

        long bestMonitor = primaryMonitor;
        long bestOverlap = 0L;

        for (int i = 0; i < monitors.limit(); i++) {

            long candidate = monitors.get(i);

            if (!readMonitorWorkarea(candidate))
                continue;

            long overlap = (long) overlapSpan(config.getWindowX(), config.width, areaScratchX.get(0),
                    areaScratchW.get(0))
                    * overlapSpan(config.getWindowY(), config.height, areaScratchY.get(0), areaScratchH.get(0));

            if (overlap > bestOverlap) {
                bestOverlap = overlap;
                bestMonitor = candidate;
            }
        }

        return bestMonitor;
    }

    private boolean readMonitorWorkarea(long monitor) {

        areaScratchX.clear();
        areaScratchY.clear();
        areaScratchW.clear();
        areaScratchH.clear();
        GLFW.glfwGetMonitorWorkarea(monitor, areaScratchX, areaScratchY, areaScratchW, areaScratchH);

        if (areaScratchW.get(0) > 0 && areaScratchH.get(0) > 0)
            return true;

        GLFWVidMode mode = GLFW.glfwGetVideoMode(monitor);

        if (mode == null)
            return false;

        GLFW.glfwGetMonitorPos(monitor, areaScratchX, areaScratchY);
        areaScratchW.put(0, mode.width());
        areaScratchH.put(0, mode.height());
        return true;
    }

    private void captureWindowedBounds() {

        if (GLFW.glfwGetWindowMonitor(mainHandle) != 0L
                || GLFW.glfwGetWindowAttrib(mainHandle, GLFW.GLFW_ICONIFIED) == GLFW.GLFW_TRUE
                || GLFW.glfwGetWindowAttrib(mainHandle, GLFW.GLFW_MAXIMIZED) == GLFW.GLFW_TRUE)
            return;

        posScratchX.clear();
        posScratchY.clear();
        sizeScratchW.clear();
        sizeScratchH.clear();
        GLFW.glfwGetWindowPos(mainHandle, posScratchX, posScratchY);
        GLFW.glfwGetWindowSize(mainHandle, sizeScratchW, sizeScratchH);

        if (sizeScratchW.get(0) <= 0 || sizeScratchH.get(0) <= 0)
            return;

        display.setWindowBounds(posScratchX.get(0), posScratchY.get(0), sizeScratchW.get(0), sizeScratchH.get(0));
    }

    private static boolean hasWindowPosition(Lwjgl3Configuration config) {
        return config.getWindowX() != EngineSetting.WINDOW_POSITION_UNSET
                && config.getWindowY() != EngineSetting.WINDOW_POSITION_UNSET;
    }

    private static int overlapSpan(int start, int length, int areaStart, int areaLength) {
        return Math.max(0, Math.min(start + length, areaStart + areaLength) - Math.max(start, areaStart));
    }

    // Accessible \\

    public long newWindow(String title, int width, int height) {

        applyWindowHints(glMajor, glMinor);
        long handle = GLFW.glfwCreateWindow(width, height, title, 0L, mainHandle);

        if (handle == 0L)
            EngineUtility.throwException("Failed to create secondary window: " + title);

        Lwjgl3Input windowInput = new Lwjgl3Input(handle);
        windowInput.initCursors();
        registerCallbacks(handle, windowInput, null);
        secondaryHandles.add(handle);
        handle2Input.put(handle, windowInput);

        return handle;
    }

    public void removeSecondaryWindow(long handle) {

        for (int i = 0; i < secondaryHandles.size(); i++) {

            if (secondaryHandles.getLong(i) != handle)
                continue;

            secondaryHandles.removeLong(i);
            handle2Input.remove(handle);
            return;
        }
    }

    void onWindowMoved(long handle) {
        if (handle == mainHandle)
            captureWindowedBounds();
    }

    public void exit() {
        running = false;
    }

    public int getSwapInterval() {
        return swapInterval;
    }

    public Input getInputForHandle(long handle) {

        if (handle == mainHandle)
            return input;

        return handle2Input.get(handle);
    }

    public Lwjgl3Input getLwjglInputForHandle(long handle) {

        if (handle == mainHandle)
            return input;

        return handle2Input.get(handle);
    }
}
