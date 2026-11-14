package application.runtime.player;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.savepipeline.savemanager.SaveManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.bootstrap.worldpipeline.worldmanager.WorldManager;
import application.runtime.input.InputSystem;
import engine.root.SystemPackage;

public class PlayerSystem extends SystemPackage {

    /*
     * Triggers player spawning at runtime startup. Passes the context window,
     * the context's RawInputHandle and the world it plays in to spawnPlayer()
     * — all caller-owned, no internal lookups. The world is the active one
     * unless useWorld() names another before the player spawns. The spawned player only becomes a saved character
     * once the main menu starts or loads one; on dispose SaveManager writes
     * back whichever character this window is playing and the player is
     * despawned with its window. The editor reuses RuntimeContext unchanged.
     */

    // Internal
    private PlayerManager playerManager;
    private InputSystem inputManager;
    private SaveManager saveManager;
    private WorldManager worldManager;

    // World
    private WorldHandle worldHandle;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.inputManager = get(InputSystem.class);
        this.saveManager = get(SaveManager.class);
        this.worldManager = get(WorldManager.class);
    }

    @Override
    protected void awake() {
        playerManager.spawnPlayer(
                context.getWindow(),
                inputManager.getRawInputHandle(),
                worldHandle != null ? worldHandle : worldManager.getActiveWorld());
    }

    // World \\

    // The world the player spawns in; only a world named before it spawns is used
    public void useWorld(WorldHandle worldHandle) {
        this.worldHandle = worldHandle;
    }

    @Override
    protected void dispose() {
        saveManager.saveCharacter(context.getWindow());
        playerManager.despawnPlayer(context.getWindow());
    }
}