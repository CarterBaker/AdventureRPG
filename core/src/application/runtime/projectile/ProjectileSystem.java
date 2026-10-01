package application.runtime.projectile;

import application.bootstrap.combatpipeline.projectilemanager.ProjectileRenderSystem;
import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class ProjectileSystem extends SystemPackage {

    /*
     * Draws the items in flight into this window's world target, placed
     * relative to the chunk this window's player stands in — the frame its
     * camera sees the world from, free camera included. All projectile
     * drawing is ProjectileRenderSystem's; this only resolves the window.
     */

    // Internal
    private PlayerManager playerManager;
    private ProjectileRenderSystem projectileRenderSystem;
    private WorldSystem worldSystem;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.projectileRenderSystem = get(ProjectileRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // Render \\

    @Override
    protected void render() {

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        projectileRenderSystem.pushProjectiles(
                player.getWorldHandle(),
                player.getWorldPositionStruct().getChunkCoordinate(),
                worldSystem.getWorldFbo(),
                context.getWindow());
    }
}
