package com.internal.runtime.input;

import com.internal.bootstrap.entitypipeline.entity.EntityInstance;
import com.internal.bootstrap.entitypipeline.playermanager.PlayerManager;
import com.internal.bootstrap.inputpipeline.input.InputHandle;
import com.internal.bootstrap.inputpipeline.inputsystem.InputSystem;
import com.internal.bootstrap.menupipeline.menueventsmanager.menus.InventoryBranch;
import com.internal.bootstrap.menupipeline.menumanager.MenuManager;
import com.internal.bootstrap.renderpipeline.camera.CameraInstance;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.vectors.Vector3;

public class PlayerInputSystem extends SystemPackage {

    /*
     * Translates raw InputSystem state into player actions each frame using
     * keybindings read from settings at startup. Captures cursor via
     * InputSystem.captureCursor(). Camera rotation and player InputHandle writes
     * are gated by MenuManager.isInputLocked(). Inventory toggle passes the
     * context window so the menu is bound to the correct render target.
     */

    // Internal
    private InputSystem inputSystem;
    private PlayerManager playerManager;
    private MenuManager menuManager;
    private InventoryBranch inventoryBranch;

    // Cached Bindings — Movement
    private int forwardKey;
    private int backKey;
    private int leftKey;
    private int rightKey;
    private int jumpKey;
    private int walkKey;
    private int sprintKey;

    // Cached Bindings — Actions
    private int inventoryKey;

    // Internal \\

    @Override
    protected void create() {

        // Cached Bindings — Movement
        this.forwardKey = internal.settings.keyForward;
        this.backKey = internal.settings.keyBack;
        this.leftKey = internal.settings.keyLeft;
        this.rightKey = internal.settings.keyRight;
        this.jumpKey = internal.settings.keyJump;
        this.walkKey = internal.settings.keyWalk;
        this.sprintKey = internal.settings.keySprint;

        // Cached Bindings — Actions
        this.inventoryKey = internal.settings.keyInventory;
    }

    @Override
    protected void get() {

        // Internal
        this.inputSystem = get(InputSystem.class);
        this.playerManager = get(PlayerManager.class);
        this.menuManager = get(MenuManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
    }

    @Override
    protected void update() {

        if (!playerManager.hasPlayer())
            return;

        handleInventoryInput();

        if (menuManager.isInputLocked())
            return;

        updateCameraRotation();
        writePlayerInput();
    }

    // Input \\

    private void handleInventoryInput() {

        if (!inputSystem.keyJustPressed(inventoryKey))
            return;

        inventoryBranch.toggleInventory(playerManager.getPlayer(), context.getWindow());
    }

    private void updateCameraRotation() {

        CameraInstance camera = playerManager.getCamera();

        if (camera == null)
            return;

        camera.setRotation(inputSystem.getMouseDelta());
    }

    private void writePlayerInput() {

        EntityInstance player = playerManager.getPlayer();
        CameraInstance camera = playerManager.getCamera();

        if (camera == null)
            return;

        InputHandle handle = player.getInputHandle();
        Vector3 direction = camera.getDirection();

        handle.setForward(inputSystem.keyHeld(forwardKey));
        handle.setBack(inputSystem.keyHeld(backKey));
        handle.setLeft(inputSystem.keyHeld(leftKey));
        handle.setRight(inputSystem.keyHeld(rightKey));
        handle.setJump(inputSystem.keyHeld(jumpKey));
        handle.setWalk(inputSystem.keyHeld(walkKey));
        handle.setSprint(inputSystem.keyHeld(sprintKey));
        handle.setPrimaryAction(inputSystem.isLeftClick());
        handle.setSecondaryAction(inputSystem.isRightClick());
        handle.setFacingDirection(direction.x, direction.y, direction.z);
    }
}