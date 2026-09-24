package application.debug.freecamera;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.runtime.input.InputSystem;
import engine.root.SystemPackage;

public class FreeCameraSystem extends SystemPackage {

    /*
     * Triggers free camera spawning at debug startup — the DebugContext
     * counterpart of PlayerSystem. Passes the context window and the
     * context's RawInputHandle to spawnFreeCamera(), both caller-owned, so
     * the window streams the world around a camera that flies with no
     * physics and no character.
     */

    // Internal
    private PlayerManager playerManager;
    private InputSystem inputSystem;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.inputSystem = get(InputSystem.class);
    }

    @Override
    protected void awake() {
        playerManager.spawnFreeCamera(context.getWindow(), inputSystem.getRawInputHandle());
    }
}
