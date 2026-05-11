package application.bootstrap.physicspipeline.raycastmanager;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.physicspipeline.util.ScreenRayStruct;
import application.kernel.windowpipeline.windowmanager.WindowManager;
import engine.root.BranchPackage;

class ScreenCastBranch extends BranchPackage {

    /*
     * Assembles the current frame's ScreenRayStruct from raw InputSystem state
     * and the active WindowManager window. Called by RaycastManager each frame.
     * Always writes mouse position and returns true while an active window exists.
     * Click detection is the caller's responsibility — this branch only tracks
     * cursor position.
     */

    // Internal
    private InputSystem inputSystem;
    private WindowManager windowManager;

    // Internal \\

    @Override
    protected void get() {
        this.inputSystem = get(InputSystem.class);
        this.windowManager = get(WindowManager.class);
    }

    // Cast \\

    boolean cast(ScreenRayStruct out) {

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