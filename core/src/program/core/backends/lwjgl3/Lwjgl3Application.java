package program.core.backends.lwjgl3;

import program.core.app.Application;
import program.core.app.ApplicationListener;
import program.core.app.CoreContext;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;

public class Lwjgl3Application implements Application {
    private static long mainWindow;
    private final ApplicationListener listener;
    private final Lwjgl3Graphics graphics;
    private final Lwjgl3Input input;
    private boolean running = true;

    public Lwjgl3Application(ApplicationListener listener, Lwjgl3ApplicationConfiguration config) {
        this.listener = listener;
        if (!GLFW.glfwInit()) throw new IllegalStateException("Unable to initialize GLFW");
        mainWindow = GLFW.glfwCreateWindow(config.width, config.height, config.title, 0, 0);
        if (mainWindow == 0L) throw new IllegalStateException("Failed to create window");
        GLFW.glfwMakeContextCurrent(mainWindow);
        GL.createCapabilities();

        this.graphics = new Lwjgl3Graphics();
        graphics.setSize(config.width, config.height);
        graphics.setFullscreen(config.isFullscreen());
        graphics.setWindow(new Lwjgl3Window(mainWindow));
        this.input = new Lwjgl3Input();

        CoreContext.app = this;
        CoreContext.graphics = graphics;
        CoreContext.input = input;
        Lwjgl3GL gl = new Lwjgl3GL();
        CoreContext.gl = gl;
        CoreContext.gl20 = gl;
        CoreContext.gl30 = gl;

        GLFW.glfwSetCursorPosCallback(mainWindow, (w,x,y) -> input.onCursor(x,y));
        GLFW.glfwSetMouseButtonCallback(mainWindow, (w,b,a,m) -> input.onMouseButton(b,a));
        GLFW.glfwSetKeyCallback(mainWindow, (w,k,s,a,m) -> input.onKey(k,a));

        listener.create();
        loop(config.getWindowListener());
        listener.dispose();
        GLFW.glfwDestroyWindow(mainWindow);
        GLFW.glfwTerminate();
    }

    private void loop(Lwjgl3WindowAdapter adapter) {
        long last = System.nanoTime();
        while (running && !GLFW.glfwWindowShouldClose(mainWindow)) {
            if (adapter != null && GLFW.glfwWindowShouldClose(mainWindow) && !adapter.closeRequested()) break;
            long now = System.nanoTime();
            graphics.setDelta((now - last) / 1_000_000_000f);
            last = now;

            listener.render();
            GLFW.glfwSwapBuffers(mainWindow);
            GLFW.glfwPollEvents();
            input.endFrame();
        }
    }

    public Lwjgl3Window newWindow(ApplicationListener ignored, Lwjgl3WindowConfiguration config) {
        long handle = GLFW.glfwCreateWindow(config.width, config.height, config.title, 0, mainWindow);
        return new Lwjgl3Window(handle);
    }

    public static long mainWindowHandle() { return mainWindow; }

    @Override
    public void exit() { running = false; }
}
