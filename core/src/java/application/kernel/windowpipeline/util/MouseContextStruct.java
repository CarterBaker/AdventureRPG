package application.kernel.windowpipeline.util;

import application.kernel.windowpipeline.window.WindowInstance;
import engine.root.StructPackage;

public class MouseContextStruct extends StructPackage {

    /*
     * Pre-allocated result of the per-frame mouse resolution pass written by
     * WindowManager each frame before any system reads input. logicalWindow is
     * the highest-depth logical window whose composite rect contains the cursor,
     * or the active OS window if none qualifies. localX and localY are in
     * logical-window space. valid is false when no OS window is focused.
     */

    // Resolution
    private WindowInstance logicalWindow;
    private float localX;
    private float localY;
    private boolean valid;

    // Accessible \\

    public WindowInstance getLogicalWindow() {
        return logicalWindow;
    }

    public void setLogicalWindow(WindowInstance logicalWindow) {
        this.logicalWindow = logicalWindow;
    }

    public float getLocalX() {
        return localX;
    }

    public void setLocalX(float localX) {
        this.localX = localX;
    }

    public float getLocalY() {
        return localY;
    }

    public void setLocalY(float localY) {
        this.localY = localY;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }
}