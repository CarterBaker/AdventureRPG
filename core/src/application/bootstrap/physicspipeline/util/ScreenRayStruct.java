package application.bootstrap.physicspipeline.util;

import application.core.engine.StructPackage;

public class ScreenRayStruct extends StructPackage {

    /*
     * Captures a screen-space click event from a specific window. Produced
     * by any system that handles mouse input and needs to communicate a
     * screen-space ray to downstream systems like RaycastManager or
     * ButtonRayBranch. windowID identifies which window the click occurred
     * in so menu systems can ignore clicks from other windows.
     */

    // Identity
    private int windowID;

    // Position
    private float screenX;
    private float screenY;
    private float screenW;
    private float screenH;

    // Constructor \\

    public ScreenRayStruct() {
    }

    // Init \\

    public void init(
            int windowID,
            float screenX,
            float screenY,
            float screenW,
            float screenH) {

        // Identity
        this.windowID = windowID;

        // Position
        this.screenX = screenX;
        this.screenY = screenY;
        this.screenW = screenW;
        this.screenH = screenH;
    }

    // Accessible \\

    public int getWindowID() {
        return windowID;
    }

    public float getScreenX() {
        return screenX;
    }

    public float getScreenY() {
        return screenY;
    }

    public float getScreenW() {
        return screenW;
    }

    public float getScreenH() {
        return screenH;
    }
}