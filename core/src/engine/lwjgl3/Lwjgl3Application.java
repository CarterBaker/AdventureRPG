package engine.lwjgl3;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

import engine.input.Input;
import engine.root.EngineContext;
import engine.root.EnginePackage;
import engine.root.EngineUtility;

import java.util.function.BooleanSupplier;

public class Lwjgl3Application {

    /*
     * Entry point for the LWJGL3 backend. Owns the main GLFW window and primary
     * GL context. The loop advances the engine tick and pumps input — all context
     * switching, drawing, and buffer swapping are driven by RenderManager uniformly
     * across every window, main and secondary alike.
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

        long monitor = config.isFullscreen() ? GLFW.glfwGetPrimaryMonitor() : 0L;
        this.mainHandle = GLFW.glfwCreateWindow(config.width, config.height, config.title, monitor, 0L);

        if (mainHandle == 0L) {
            GLFW.glfwTerminate();
            EngineUtility.throwException("Failed to create GLFW window");
        }

        if (config.getWindowX() >= 0 && config.getWindowY() >= 0)
            GLFW.glfwSetWindowPos(mainHandle, config.getWindowX(), config.getWindowY());

        GLFW.glfwMakeContextCurrent(mainHandle);
        GLFW.glfwSwapInterval(swapInterval);
        GL.createCapabilities();

        this.input = new Lwjgl3Input(mainHandle);
        this.display = new Lwjgl3Display(config.width, config.height, config.isFullscreen());
        this.engine = engine;

        display.setMainHandle(mainHandle);

        Lwjgl3GL gl20 = new Lwjgl3GL();
        EngineContext.display = display;
        EngineContext.input = input;
        EngineContext.gl20 = gl20;
        EngineContext.gl30 = gl20;

        registerCallbacks(mainHandle, input, config.getCloseCallback());
        GLFW.glfwSetWindowPosCallback(mainHandle, (w, x, y) -> {
            display.setPosX(x);
            display.setPosY(y);
        });

        platform.setApplication(this);

        loop();

        for (int i = 0; i < secondaryHandles.size(); i++)
            GLFW.glfwDestroyWindow(secondaryHandles.getLong(i));

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

    // Accessible \\

    public long newWindow(String title, int width, int height) {

        applyWindowHints(glMajor, glMinor);
        long handle = GLFW.glfwCreateWindow(width, height, title, 0L, mainHandle);

        if (handle == 0L)
            EngineUtility.throwException("Failed to create secondary window: " + title);

        Lwjgl3Input windowInput = new Lwjgl3Input(handle);
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
}