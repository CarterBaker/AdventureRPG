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
     * LWJGL3 backend entry point. Owns the main GLFW window and primary GL
     * context and runs the loop that pumps input and ticks the engine;
     * rendering and swapping are driven by RenderManager for every window
     * alike. The main window is fitted to its saved monitor's work area before
     * it is shown, and fullscreen and vsync switch at runtime.
     */

    // Internal
    private final long mainHandle;
    private final EnginePackage engine;
    private final Lwjgl3Display display;
    private final Lwjgl3Input input;
    private final int glMajor;
    private final int glMinor;
    private int swapInterval;

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
        int createWidth = config.isFullscreen() ? config.getFullscreenWidth() : config.width;
        int createHeight = config.isFullscreen() ? config.getFullscreenHeight() : config.height;
        this.mainHandle = GLFW.glfwCreateWindow(createWidth, createHeight, config.title, monitor, 0L);
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);

        if (mainHandle == 0L) {
            GLFW.glfwTerminate();
            EngineUtility.throwException("Failed to create GLFW window");
        }

        if (!config.isFullscreen())
            placeMainWindow(config.width, config.height, config.getWindowX(), config.getWindowY());

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
        seedWindowedBounds(config);

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
        GLFW.glfwSetWindowMaximizeCallback(mainHandle, (w, maximized) -> onWindowMaximized(maximized));
    }

    private void onWindowMaximized(boolean maximized) {
        if (!display.isFullscreen())
            display.setMaximized(maximized);
    }

    private void loop() {

        long last = System.nanoTime();

        while (running && !GLFW.glfwWindowShouldClose(mainHandle)) {

            long now = System.nanoTime();
            float delta = (float) (now - last) / EngineSetting.NANOS_PER_SECOND;
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

    private void placeMainWindow(int windowWidth, int windowHeight, int windowX, int windowY) {

        long monitor = resolvePlacementMonitor(windowX, windowY, windowWidth, windowHeight);

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
        int width = Math.clamp(windowWidth, EngineSetting.MIN_WINDOW_DIMENSION, maxWidth);
        int height = Math.clamp(windowHeight, EngineSetting.MIN_WINDOW_DIMENSION, maxHeight);

        int minX = areaX + frameLeft;
        int minY = areaY + frameTop;
        int maxX = Math.max(minX, areaX + areaWidth - frameRight - width);
        int maxY = Math.max(minY, areaY + areaHeight - frameBottom - height);

        int x = hasWindowPosition(windowX, windowY)
                ? Math.clamp(windowX, minX, maxX)
                : minX + (maxX - minX) / 2;
        int y = hasWindowPosition(windowX, windowY)
                ? Math.clamp(windowY, minY, maxY)
                : minY + (maxY - minY) / 2;

        GLFW.glfwSetWindowSize(mainHandle, width, height);
        GLFW.glfwSetWindowPos(mainHandle, x, y);
    }

    private long resolvePlacementMonitor(int windowX, int windowY, int windowWidth, int windowHeight) {

        long primaryMonitor = GLFW.glfwGetPrimaryMonitor();
        PointerBuffer monitors = GLFW.glfwGetMonitors();

        if (!hasWindowPosition(windowX, windowY) || monitors == null)
            return primaryMonitor;

        long bestMonitor = primaryMonitor;
        long bestOverlap = 0L;

        for (int i = 0; i < monitors.limit(); i++) {

            long candidate = monitors.get(i);

            if (!readMonitorWorkarea(candidate))
                continue;

            long overlap = (long) overlapSpan(windowX, windowWidth, areaScratchX.get(0), areaScratchW.get(0))
                    * overlapSpan(windowY, windowHeight, areaScratchY.get(0), areaScratchH.get(0));

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

    private void seedWindowedBounds(Lwjgl3Configuration config) {

        if (!config.isFullscreen()) {
            captureWindowedBounds();
            return;
        }

        display.setWindowBounds(config.getWindowX(), config.getWindowY(), config.width, config.height);
        display.setMaximized(config.isMaximized());
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

    private static boolean hasWindowPosition(int windowX, int windowY) {
        return windowX != EngineSetting.WINDOW_POSITION_UNSET
                && windowY != EngineSetting.WINDOW_POSITION_UNSET;
    }

    private static int overlapSpan(int start, int length, int areaStart, int areaLength) {
        return Math.max(0, Math.min(start + length, areaStart + areaLength) - Math.max(start, areaStart));
    }

    // Display Mode \\

    void setFullscreen(boolean fullscreen) {

        if (display.isFullscreen() == fullscreen)
            return;

        if (fullscreen)
            enterFullscreen();
        else
            exitFullscreen();
    }

    private void enterFullscreen() {

        long monitor = resolvePlacementMonitor(
                display.getPosX(),
                display.getPosY(),
                display.getWindowWidth(),
                display.getWindowHeight());
        GLFWVidMode mode = monitor != 0L ? GLFW.glfwGetVideoMode(monitor) : null;

        if (mode == null)
            return;

        display.setFullscreen(true);
        GLFW.glfwSetWindowMonitor(mainHandle, monitor, 0, 0, mode.width(), mode.height(), mode.refreshRate());
    }

    private void exitFullscreen() {

        int windowX = display.getPosX();
        int windowY = display.getPosY();
        int width = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, display.getWindowWidth());
        int height = Math.max(EngineSetting.MIN_WINDOW_DIMENSION, display.getWindowHeight());
        boolean positioned = hasWindowPosition(windowX, windowY);

        GLFW.glfwSetWindowMonitor(
                mainHandle,
                0L,
                positioned ? windowX : 0,
                positioned ? windowY : 0,
                width,
                height,
                GLFW.GLFW_DONT_CARE);
        display.setFullscreen(false);
        placeMainWindow(width, height, windowX, windowY);

        if (display.isMaximized())
            GLFW.glfwMaximizeWindow(mainHandle);
    }

    void setSwapInterval(int swapInterval) {
        this.swapInterval = swapInterval;
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
