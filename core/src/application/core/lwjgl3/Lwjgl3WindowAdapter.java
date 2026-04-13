package application.core.lwjgl3;

public class Lwjgl3WindowAdapter {

    /*
     * No-op base adapter for GLFW window lifecycle events.
     * Override only the callbacks needed.
     */

    public boolean closeRequested() {
        return true;
    }
}