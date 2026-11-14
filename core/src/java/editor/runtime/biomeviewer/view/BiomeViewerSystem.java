package editor.runtime.biomeviewer.view;

import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.worldpipeline.world.WorldHandle;
import application.runtime.player.PlayerSystem;
import editor.bootstrap.biomeplannerpipeline.biomeplannermanager.BiomePlannerManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Coordinate2Long;

public class BiomeViewerSystem extends SystemPackage {

    /*
     * Sends this tab's player to the biome planner's world before it spawns,
     * then, once every system is awake, stands it over the middle of the
     * world, the centre of the planner's grid, and lets the camera fly free
     * from there.
     */

    // Internal
    private PlayerSystem playerSystem;
    private PlayerManager playerManager;
    private BiomePlannerManager biomePlannerManager;

    // Base \\

    @Override
    protected void get() {
        this.playerSystem = get(PlayerSystem.class);
        this.playerManager = get(PlayerManager.class);
        this.biomePlannerManager = get(BiomePlannerManager.class);
    }

    @Override
    protected void awake() {
        playerSystem.useWorld(biomePlannerManager.getWorldHandle());
    }

    @Override
    protected void start() {

        int windowID = context.getWindow().getWindowID();
        WorldHandle worldHandle = biomePlannerManager.getWorldHandle();
        int centre = EngineSetting.CHUNK_SIZE / 2;
        long chunkCoordinate = Coordinate2Long.pack(
                worldHandle.getWorldScale().x / EngineSetting.CHUNK_SIZE / 2,
                worldHandle.getWorldScale().y / EngineSetting.CHUNK_SIZE / 2);

        playerManager.teleportPlayerForWindow(windowID, chunkCoordinate, centre, centre);
        playerManager.setFreeCameraForWindow(windowID, true);
    }
}
