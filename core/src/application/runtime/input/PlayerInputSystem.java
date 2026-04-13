package application.runtime.input;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.inputpipeline.input.InputHandle;
import application.bootstrap.inputpipeline.inputsystem.InputSystem;
import application.bootstrap.menupipeline.menueventsmanager.menus.InventoryBranch;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import engine.root.SystemPackage;
import engine.util.camera.CameraInstance;
import engine.util.mathematics.vectors.Vector3;

public class PlayerInputSystem extends SystemPackage {

    /*
     * Translates raw InputSystem state into player actions each frame using
     * Bindings. Cursor capture, camera rotation, and player InputHandle
     * writes are gated by MenuManager.isInputLocked().
     */

    // Internal
    private InputSystem inputSystem;
    private PlayerManager playerManager;
    private MenuManager menuManager;
    private InventoryBranch inventoryBranch;

    // Internal \\

    @Override
    protected void get() {
        this.inputSystem = get(InputSystem.class);
        this.playerManager = get(PlayerManager.class);
        this.menuManager = get(MenuManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
    }

    @Override
    protected void update() {

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        handleInventoryInput();

        if (menuManager.isInputLocked())
            return;

        updateCameraRotation();
        writePlayerInput();
    }

    // Input \\

    private void handleInventoryInput() {

        if (!inputSystem.bindingJustPressed(Bindings.INVENTORY))
            return;

        inventoryBranch.toggleInventory(
                playerManager.getPlayerForWindow(context.getWindow().getWindowID()),
                context.getWindow());
    }

    private void updateCameraRotation() {

        CameraInstance camera = playerManager.getCameraForWindow(context.getWindow().getWindowID());

        if (camera == null)
            return;

        camera.setRotation(inputSystem.getMouseDelta());
    }

    private void writePlayerInput() {

        int windowID = context.getWindow().getWindowID();
        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);

        if (camera == null)
            return;

        InputHandle handle = player.getInputHandle();
        Vector3 direction = camera.getDirection();

        handle.setForward(inputSystem.bindingHeld(Bindings.MOVE_FORWARD));
        handle.setBack(inputSystem.bindingHeld(Bindings.MOVE_BACK));
        handle.setLeft(inputSystem.bindingHeld(Bindings.MOVE_LEFT));
        handle.setRight(inputSystem.bindingHeld(Bindings.MOVE_RIGHT));
        handle.setJump(inputSystem.bindingHeld(Bindings.JUMP));
        handle.setWalk(inputSystem.bindingHeld(Bindings.WALK));
        handle.setSprint(inputSystem.bindingHeld(Bindings.SPRINT));
        handle.setPrimaryAction(inputSystem.isLeftDown());
        handle.setSecondaryAction(inputSystem.isRightDown());
        handle.setFacingDirection(direction.x, direction.y, direction.z);
    }
}