package application.runtime.player;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.runtime.input.InputSystem;
import engine.root.SystemPackage;

public class PlayerSystem extends SystemPackage {

    /*
     * Triggers player spawning at runtime startup. Passes the context window
     * and the context's RawInputHandle to spawnPlayer() — both caller-owned,
     * no internal lookups. The spawned player only becomes a saved character
     * once the main menu starts or loads one; on dispose SaveManager writes
     * back whichever character this window is playing. The editor reuses
     * RuntimeContext unchanged.
     */

    // Internal
    private PlayerManager playerManager;
    private InputSystem inputManager;
    private SaveManager saveManager;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.inputManager = get(InputSystem.class);
        this.saveManager = get(SaveManager.class);
    }

    @Override
    protected void awake() {
        playerManager.spawnPlayer(context.getWindow(), inputManager.getRawInputHandle());
    }

    @Override
    protected void dispose() {
        saveManager.saveCharacter(context.getWindow());
    }
}