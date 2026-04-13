package program.core.lwjgl3;

class Lwjgl3ManagedWindow {

    /*
     * Lightweight pairing of a secondary GLFW window handle with its input
     * instance. Rendering and buffer swapping are driven by RenderManager —
     * no per-window listener exists or is needed.
     */

    // Identity
    private final long handle;
    private final Lwjgl3Input input;

    Lwjgl3ManagedWindow(long handle, Lwjgl3Input input) {
        this.handle = handle;
        this.input = input;
    }

    // Accessible \\

    long getHandle() {
        return handle;
    }

    Lwjgl3Input getInput() {
        return input;
    }
}