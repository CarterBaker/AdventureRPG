package application.bootstrap.menupipeline.locksystem;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.kernel.inputpipeline.inputsystem.InputSystem;
import engine.root.SystemPackage;

public class LockSystem extends SystemPackage {

    /*
     * Manages input and raycast lock reference counts. On input lock transitions,
     * drives cursor capture via InputSystem and gameplay input suppression via
     * PlayerManager. Neither system is aware of the other — LockSystem is the
     * sole coordinator.
     */

    // Internal
    private InputSystem inputSystem;
    private PlayerManager playerManager;

    // Lock Reference Counts
    private int inputLockCount;
    private int raycastLockCount;

    @Override
    protected void get() {
        this.inputSystem = get(InputSystem.class);
        this.playerManager = get(PlayerManager.class);
    }

    // Input Lock \\

    public void applyInputLock(int delta) {
        int prev = inputLockCount;
        inputLockCount = Math.max(0, inputLockCount + delta);

        if (prev == 0 && inputLockCount > 0) {
            inputSystem.captureCursor(false);
            playerManager.setInputLocked(true);
        } else if (prev > 0 && inputLockCount == 0) {
            playerManager.setInputLocked(false);
            inputSystem.captureCursor(true);
        }
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