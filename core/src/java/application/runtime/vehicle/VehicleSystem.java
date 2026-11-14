package application.runtime.vehicle;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleRenderSystem;
import application.runtime.world.WorldSystem;
import engine.root.SystemPackage;

public class VehicleSystem extends SystemPackage {

    /*
     * Draws the vehicles into this window's world target, placed relative to
     * the chunk this window's player stands in — the frame its camera sees the
     * world from, free camera included. All vehicle drawing is
     * VehicleRenderSystem's; this only resolves the window.
     */

    // Internal
    private PlayerManager playerManager;
    private VehicleRenderSystem vehicleRenderSystem;
    private WorldSystem worldSystem;

    // Internal \\

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.vehicleRenderSystem = get(VehicleRenderSystem.class);
        this.worldSystem = get(WorldSystem.class);
    }

    // Render \\

    @Override
    protected void render() {

        int windowID = context.getWindow().getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID))
            return;

        EntityInstance player = playerManager.getPlayerForWindow(windowID);

        vehicleRenderSystem.pushVehicles(
                player.getWorldHandle(),
                player.getWorldPositionStruct().getChunkCoordinate(),
                worldSystem.getWorldFbo(),
                context.getWindow());
    }
}
