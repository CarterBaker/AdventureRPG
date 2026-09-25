package application.runtime.player;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.runtime.input.InputSystem;
import engine.root.SystemPackage;

public class PlayerSystem extends SystemPackage {

    /*
     * Triggers player spawning at runtime startup. Passes the context window
     * and the context's RawInputHandle to spawnPlayer() — both caller-owned,
     * no internal lookups — then hands the spawned player to SaveManager to
     * restore, and back to it on dispose to save. The editor reuses
     * RuntimeContext unchanged.
     */

    // Internal
    private PlayerManager playerManager;
    private InputSystem inputManager;
    private SaveManager saveManager;

    // Player
    private EntityInstance player;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.inputManager = get(InputSystem.class);
        this.saveManager = get(SaveManager.class);
    }

    @Override
    protected void awake() {

        this.player = playerManager.spawnPlayer(context.getWindow(), inputManager.getRawInputHandle());
        saveManager.restorePlayer(context.getWindow(), player);
    }

    @Override
    protected void dispose() {
        saveManager.savePlayer(context.getWindow(), player);
    }
}