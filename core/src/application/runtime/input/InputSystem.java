package application.runtime.input;

import application.bootstrap.entitypipeline.entity.EntityInputHandle;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.kernel.inputpipeline.input.RawInputHandle;
import application.kernel.inputpipeline.inputmanager.InputManager;
import engine.assets.camera.CameraInstance;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class InputSystem extends SystemPackage {

    /*
     * Runtime input bridge for this context. Owns the context's RawInputHandle,
     * refreshed from InputManager each frame, and drives camera rotation and
     * the player's facing direction. Movement bindings are translated by
     * PlayerInputSystem inside PlayerManager.
     */

    // Internal
    private InputManager bootstrapInput;
    private PlayerManager playerManager;

    // Raw input — owned here, passed to PlayerManager at spawn
    private RawInputHandle rawInputHandle;

    // Internal \\

    @Override
    protected void create() {
        this.rawInputHandle = create(RawInputHandle.class);
    }

    @Override
    protected void get() {
        this.bootstrapInput = get(InputManager.class);
        this.playerManager = get(PlayerManager.class);
    }

    @Override
    protected void update() {

        bootstrapInput.writeRawInput(rawInputHandle, context.getWindow());
        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        if (context.getWindow().getMenuListHandle().isInputLocked())
            return;

        updateCameraRotation(windowID);
        writeFacingDirection(windowID);
    }

    // Input \\

    private void updateCameraRotation(int windowID) {
        CameraInstance camera = playerManager.getCameraForWindow(windowID);
        if (camera == null)
            return;
        camera.setRotation(bootstrapInput.getMouseDelta(context.getWindow()));
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