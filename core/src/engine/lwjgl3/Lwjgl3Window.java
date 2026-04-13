package engine.lwjgl3;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryStack;

public class Lwjgl3Window {

    /*
     * Handle to an OS window. Position is queried once at construction and kept
     * current via a GLFW pos callback — zero per-frame allocation.
     */

    // Identity
    private final long handle;
    private final Lwjgl3Input input;

    // State
    private int posX;
    private int posY;

    Lwjgl3Window(long handle, Lwjgl3Input input) {

        this.handle = handle;
        this.input = input;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            var x = stack.mallocInt(1);
            var y = stack.mallocInt(1);
            GLFW.glfwGetWindowPos(handle, x, y);
            posX = x.get(0);
            posY = y.get(0);
        }

        GLFW.glfwSetWindowPosCallback(handle, (w, x, y) -> {
            posX = x;
            posY = y;
        });
    }

    // Accessible \\

    public long getHandle() {
        return handle;
    }

    public Lwjgl3Input getInput() {
        return input;
    }

    public int getPositionX() {
        return posX;
    }

    public int getPositionY() {
        return posY;
    }
}