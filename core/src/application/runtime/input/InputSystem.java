package application.runtime.input;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.entitypipeline.util.EntityInputHandle;
import application.bootstrap.menupipeline.menumanager.MenuManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.runtime.menueventsmanager.menus.InventoryBranch;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.settings.KeyBindings;
import engine.util.mathematics.vectors.Vector3;

public class InputSystem extends SystemPackage {

    /*
     * Runtime input bridge. Owns the RawInputHandle for this context —
     * written each frame by the bootstrap InputSystem snapshot.
     * Responsible for camera rotation and facing direction only.
     * Movement key → EntityInputHandle translation is handled by
     * PlayerInputSystem inside PlayerManager. This class no longer
     * touches movement bindings directly.
     */

    // Internal
    private application.kernel.inputpipeline.inputsystem.InputSystem bootstrapInput;
    private PlayerManager playerManager;
    private MenuManager menuManager;
    private InventoryBranch inventoryBranch;

    // Raw input — owned here, passed to PlayerManager at spawn
    private RawInputHandle rawInputHandle;

    // Internal \\

    @Override
    protected void create() {
        this.rawInputHandle = create(RawInputHandle.class);
    }

    @Override
    protected void get() {
        this.bootstrapInput = get(application.kernel.inputpipeline.inputsystem.InputSystem.class);
        this.playerManager = get(PlayerManager.class);
        this.menuManager = get(MenuManager.class);
        this.inventoryBranch = get(InventoryBranch.class);
    }

    @Override
    protected void update() {

        // Snapshot raw hardware state into this context's handle
        bootstrapInput.writeRawInput(rawInputHandle);

        int windowID = context.getWindow().getWindowID();
        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        handleInventoryInput(windowID);

        if (menuManager.isInputLocked(context.getWindow()))
            return;

        updateCameraRotation(windowID);
        writeFacingDirection(windowID);
    }

    // Input \\

    private void handleInventoryInput(int windowID) {
        if (!rawInputHandle.isBindingClicked(KeyBindings.INVENTORY))
            return;
        inventoryBranch.toggleInventory(
                playerManager.getPlayerForWindow(windowID),
                context.getWindow());
    }

    private void updateCameraRotation(int windowID) {
        CameraInstance camera = playerManager.getCameraForWindow(windowID);
        if (camera == null)
            return;
        camera.setRotation(bootstrapInput.getMouseDelta());
    }

    private void writeFacingDirection(int windowID) {
        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        CameraInstance camera = playerManager.getCameraForWindow(windowID);
        if (player == null || camera == null)
            return;
        EntityInputHandle handle = player.getEntityInputHandle();
        Vector3 direction = camera.getDirection();
        handle.setFacingDirection(direction.x, direction.y, direction.z);
    }

    // Accessible \\

    public RawInputHandle getRawInputHandle() {
        return rawInputHandle;
    }
}