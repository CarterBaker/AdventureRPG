package program.core.backends.lwjgl3;

public class Lwjgl3WindowConfiguration {

    /*
     * Shared base configuration for any GLFW window. Holds title and initial
     * dimensions. Extended by Lwjgl3ApplicationConfiguration for boot-time options.
     */

    // Identity
    String title = "Window";

    // Dimensions
    int width = 1280;
    int height = 720;

    // Accessible \\

    public void setTitle(String title) {
        this.title = title;
    }

    public void setWindowedMode(int width, int height) {
        this.width = width;
        this.height = height;
    }
}