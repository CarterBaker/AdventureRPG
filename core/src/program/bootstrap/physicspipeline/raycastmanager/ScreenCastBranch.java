package program.bootstrap.physicspipeline.raycastmanager;

import program.bootstrap.inputpipeline.inputsystem.InputSystem;
import program.bootstrap.physicspipeline.util.ScreenRayStruct;
import program.core.engine.BranchPackage;
import program.core.kernel.windowmanager.WindowManager;

class ScreenCastBranch extends BranchPackage {

    /*
     * Assembles the current frame's ScreenRayStruct from raw InputSystem state
     * and the active WindowManager window. Called by RaycastManager each frame.
     * If no click is detected the struct is cleared via the hasScreenRay flag —
     * the struct itself is never nulled, just ignored by callers via
     * hasScreenRay().
     */

    // Internal
    private InputSystem inputSystem;
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void get() {

        // Internal
        this.inputSystem = get(InputSystem.class);
        this.windowManager = get(WindowManager.class);
    }

    // Cast \\

    boolean cast(ScreenRayStruct out) {

        if (!inputSystem.isRawLeftClick())
            return false;

        int windowID = windowManager.getActiveWindow().getWindowID();
        float screenW = windowManager.getActiveWindow().getWidth();
        float screenH = windowManager.getActiveWindow().getHeight();

        out.init(
                windowID,
                inputSystem.getMouseX(),
                inputSystem.getMouseY(),
                screenW,
                screenH);

        return true;
    }
}