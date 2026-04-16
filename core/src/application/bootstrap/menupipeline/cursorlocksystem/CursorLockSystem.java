package application.bootstrap.menupipeline.cursorlocksystem;

import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import engine.root.SystemPackage;

public class CursorLockSystem extends SystemPackage {

    /*
     * Manages input and raycast lock reference counts. Drives cursor capture
     * state transitions on InputSystem when the input lock count crosses zero.
     */

    // Internal
    private InputSystem inputSystem;

    // Lock Reference Counts
    private int inputLockCount;
    private int raycastLockCount;

    @Override
    protected void get() {
        this.inputSystem = get(InputSystem.class);
    }

    // Input Lock \\

    public void applyInputLock(int delta) {

        int prev = inputLockCount;
        inputLockCount = Math.max(0, inputLockCount + delta);

        if (prev == 0 && inputLockCount > 0)
            inputSystem.captureCursor(false);
        else if (prev > 0 && inputLockCount == 0)
            inputSystem.captureCursor(true);
    }

    // Raycast Lock \\

    public void applyRaycastLock(int delta) {
        raycastLockCount = Math.max(0, raycastLockCount + delta);
    }

    public boolean isRaycastLocked() {
        return raycastLockCount > 0;
    }

    // Accessible \\

    public boolean isInputLocked() {
        return inputLockCount > 0;
    }
}
