package application.runtime.player;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.runtime.input.InputSystem;
import engine.root.SystemPackage;

public class PlayerSystem extends SystemPackage {

    /*
     * Triggers player spawning at runtime startup. Passes the context window
     * and the context's RawInputHandle to spawnPlayer() — both caller-owned,
     * no internal lookups. The editor reuses RuntimeContext unchanged.
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
        playerManager.spawnPlayer(context.getWindow(), inputSystem.getRawInputHandle());
    }
}