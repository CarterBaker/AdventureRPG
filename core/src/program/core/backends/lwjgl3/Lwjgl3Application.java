package program.core.backends.lwjgl3;

import program.core.app.Application;
import program.core.app.ApplicationListener;
import program.core.app.CoreContext;
import program.core.engine.UtilityPackage;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

public class Lwjgl3Application implements Application {

    /*
     * Entry point for the LWJGL3 backend. Owns the main GLFW window and primary
     * GL context. The loop advances the engine tick and pumps input — all context
     * switching, drawing, and buffer swapping are driven by RenderManager uniformly
     * across every window, main and secondary alike.
     */

    // Internal
    private final long mainHandle;
    private final ApplicationListener listener;
    private final Lwjgl3Graphics graphics;
    private final Lwjgl3Input input;

    // Secondary Windows
    private final ObjectArrayList<Lwjgl3ManagedWindow> secondaryWindows;

    // State
    private boolean running;

    public Lwjgl3Application(ApplicationListener listener, Lwjgl3ApplicationConfiguration config) {

        this.secondaryWindows = new ObjectArrayList<>();
        this.running = true;

        if (!GLFW.glfwInit())
            UtilityPackage.throwException("Unable to initialize GLFW");

        applyWindowHints(config.getGlMajor(), config.getGlMinor());

        long monitor = config.isFullscreen() ? GLFW.glfwGetPrimaryMonitor() : 0L;
        this.mainHandle = GLFW.glfwCreateWindow(config.width, config.height, config.title, monitor, 0L);

        if (mainHandle == 0L) {
            GLFW.glfwTerminate();
            UtilityPackage.throwException("Failed to create GLFW window");
        }

        if (config.getWindowX() >= 0 && config.getWindowY() >= 0)
            GLFW.glfwSetWindowPos(mainHandle, config.getWindowX(), config.getWindowY());

        GLFW.glfwMakeContextCurrent(mainHandle);
        GLFW.glfwSwapInterval(config.isVsync() ? 1 : 0);
        GL.createCapabilities();

        this.input = new Lwjgl3Input(mainHandle);
        this.graphics = new Lwjgl3Graphics(config.width, config.height, config.isFullscreen());
        this.listener = listener;

        graphics.setWindow(new Lwjgl3Window(mainHandle, input));

        Lwjgl3GL gl = new Lwjgl3GL();
        CoreContext.app = this;
        CoreContext.graphics = graphics;
        CoreContext.input = input;
        CoreContext.gl = gl;
        CoreContext.gl20 = gl;
        CoreContext.gl30 = gl;

        registerCallbacks(mainHandle, input, config.getWindowListener());

        listener.create();
        loop();
        listener.dispose();

        for (int i = 0; i < secondaryWindows.size(); i++)
            GLFW.glfwDestroyWindow(secondaryWindows.get(i).getHandle());

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

    private void registerCallbacks(long handle, Lwjgl3Input inp, Lwjgl3WindowAdapter adapter) {

        GLFW.glfwSetFramebufferSizeCallback(handle, (w, width, height) -> {
            if (handle == mainHandle)
                graphics.setSize(width, height);
        });
        GLFW.glfwSetCursorPosCallback(handle, (w, x, y) -> inp.onCursor(x, y));
        GLFW.glfwSetMouseButtonCallback(handle, (w, b, a, m) -> inp.onMouseButton(b, a));
        GLFW.glfwSetScrollCallback(handle, (w, dx, dy) -> inp.onScroll(dx, dy));
        GLFW.glfwSetKeyCallback(handle, (w, k, s, a, m) -> inp.onKey(k, a));
        GLFW.glfwSetCharCallback(handle, (w, cp) -> inp.onChar(cp));

        if (adapter != null)
            GLFW.glfwSetWindowCloseCallback(handle, w -> {
                if (!adapter.closeRequested())
                    GLFW.glfwSetWindowShouldClose(w, false);
            });
    }

    private void loop() {

        long last = System.nanoTime();

        while (running && !GLFW.glfwWindowShouldClose(mainHandle)) {
            long now = System.nanoTime();
            graphics.setDelta((now - last) / 1_000_000_000f);
            last = now;

            listener.render();

            GLFW.glfwPollEvents();
            input.endFrame();

            for (int i = 0; i < secondaryWindows.size(); i++)
                secondaryWindows.get(i).getInput().endFrame();
        }
    }

    // Accessible \\

    public Lwjgl3Window newWindow(Lwjgl3WindowConfiguration config) {

        long handle = GLFW.glfwCreateWindow(config.width, config.height, config.title, 0L, mainHandle);

        if (handle == 0L)
            UtilityPackage.throwException("Failed to create secondary window: " + config.title);

        Lwjgl3Input windowInput = new Lwjgl3Input(handle);
        registerCallbacks(handle, windowInput, null);
        secondaryWindows.add(new Lwjgl3ManagedWindow(handle, windowInput));

        return new Lwjgl3Window(handle, windowInput);
    }

    public void removeSecondaryWindow(long handle) {

        for (int i = 0; i < secondaryWindows.size(); i++) {

            if (secondaryWindows.get(i).getHandle() != handle)
                continue;

            secondaryWindows.remove(i);
            return;
        }
    }

    @Override
    public void exit() {
        running = false;
    }
}