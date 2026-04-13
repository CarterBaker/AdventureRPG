package program.core.lwjgl3;

import program.core.engine.StructPackage;

public class DisplayModeStruct extends StructPackage {

    /*
     * Immutable snapshot of a monitor's active video mode at the time of query.
     * Constructed once from GLFW and never mutated.
     */

    // Identity
    private final int width;
    private final int height;
    private final int refreshRate;

    DisplayModeStruct(int width, int height, int refreshRate) {
        this.width = width;
        this.height = height;
        this.refreshRate = refreshRate;
    }

    // Accessible \\

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRefreshRate() {
        return refreshRate;
    }
}