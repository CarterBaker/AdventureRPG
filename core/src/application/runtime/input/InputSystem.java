package application.runtime.input;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.inputpipeline.input.InputHandle;
import application.bootstrap.menupipeline.menueventsmanager.menus.InventoryBranch;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import engine.root.EngineContext;
import engine.root.SystemPackage;
import engine.util.display.camera.CameraInstance;
import engine.util.mathematics.vectors.Vector2;
import engine.util.mathematics.vectors.Vector3;
import engine.util.settings.KeyBindings;

public class InputSystem extends SystemPackage {

    /*
     * Translates raw InputSystem state into player actions each frame using
     * Bindings. Cursor capture, camera rotation, and player InputHandle
     * writes are gated by MenuManager.isInputLocked().
     */

    // Internal
    private application.bootstrap.inputpipeline.inputsystem.InputSystem inputSystem;
    private PlayerManager playerManager;
    private MenuManager menuManager;
    private InventoryBranch inventoryBranch;

    // Internal \\

    @Override
    protected void get() {
        this.inputSystem = get(application.bootstrap.inputpipeline.inputsystem.InputSystem.class);
        this.playerManager = get(PlayerManager.class);
        this.menuManager = get(MenuManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
    }

    @Override
    protected void update() {
        int windowID = context.getWindow().getWindowID();
        if (!playerManager.hasPlayerForWindow(windowID))
            return;
        handleInventoryInput(windowID);
        if (menuManager.isInputLocked())
            return;
        updateCameraRotation(windowID);
        writePlayerInput(windowID);
    }

    // Input \\

    private void handleInventoryInput(int windowID) {
        if (!inputSystem.bindingClicked(KeyBindings.INVENTORY))
            return;
        inventoryBranch.toggleInventory(
                playerManager.getPlayerForWindow(windowID),
                context.getWindow());
    }

    private void updateCameraRotation(int windowID) {
        CameraInstance camera = playerManager.getCameraForWindow(windowID);
        if (camera == null)
            return;
        camera.setRotation(inputSystem.getMouseDelta());
    }

    private void writePlayerInput(int windowID) {
        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);
        if (camera == null)
            return;
        InputHandle handle = player.getInputHandle();
        Vector3 direction = camera.getDirection();
        handle.setForward(inputSystem.bindingHeld(KeyBindings.MOVE_FORWARD));
        handle.setBack(inputSystem.bindingHeld(KeyBindings.MOVE_BACK));
        handle.setLeft(inputSystem.bindingHeld(KeyBindings.MOVE_LEFT));
        handle.setRight(inputSystem.bindingHeld(KeyBindings.MOVE_RIGHT));
        handle.setJump(inputSystem.bindingHeld(KeyBindings.JUMP));
        handle.setWalk(inputSystem.bindingHeld(KeyBindings.WALK));
        handle.setSprint(inputSystem.bindingHeld(KeyBindings.SPRINT));
        handle.setPrimaryAction(inputSystem.bindingHeld(KeyBindings.PRIMARY));
        handle.setSecondaryAction(inputSystem.bindingHeld(KeyBindings.SECONDARY));
        handle.setFacingDirection(direction.x, direction.y, direction.z);
    }
}