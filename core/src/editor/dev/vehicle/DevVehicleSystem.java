package editor.dev.vehicle;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleData;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.bootstrap.worldpipeline.util.WorldPositionUtility;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class DevVehicleSystem extends SystemPackage {

    /*
     * Spawns and removes vehicles around this Dev window's player for testing.
     * Driven by the command console's spawnvehicle command, which names a
     * vehicle by its full name or by a local or display name only one vehicle
     * carries, and its removevehicle command. A vehicle spawns broadside in
     * front of the player, clear of it, at anchor: its waterline on the water
     * where there is water, its keel level with the player's feet where there
     * is not. A free-flying window has no character to spawn beside.
     */

    // Internal
    private PlayerManager playerManager;
    private VehicleManager vehicleManager;

    // Scratch
    private WorldPositionStruct spawnScratch;

    // Base \\

    @Override
    protected void create() {

        // Scratch
        this.spawnScratch = new WorldPositionStruct();
    }

    @Override
    protected void get() {
        this.playerManager = get(PlayerManager.class);
        this.vehicleManager = get(VehicleManager.class);
    }

    // Management \\

    public void spawnVehicle(String vehicleQuery) {

        WindowInstance window = context.getWindow();
        int windowID = window.getWindowID();
        VehicleHandle vehicleHandle = vehicleManager.findVehicleHandle(vehicleQuery);

        if (vehicleHandle == null) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_VEHICLE_UNKNOWN + vehicleQuery);
            return;
        }

        if (!playerManager.hasPlayerForWindow(windowID) || playerManager.isFreeCameraForWindow(windowID)) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_VEHICLE_NO_CHARACTER
                    + vehicleHandle.getDisplayName());
            return;
        }

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        Vector3 view = playerManager.getCameraForWindow(windowID).getDirection();
        float viewLength = (float) Math.sqrt(view.x * view.x + view.z * view.z);
        float forwardX = viewLength > 0f ? view.x / viewLength : 0f;
        float forwardZ = viewLength > 0f ? view.z / viewLength : 1f;

        VehicleData vehicleData = vehicleHandle.getVehicleData();
        Vector3 centerOfMass = vehicleHandle.getHull().getCenterOfMass();
        float halfBeam = (vehicleData.getMaxZ() - vehicleData.getMinZ()) * 0.5f / EngineSetting.SUB_VOXEL_RESOLUTION;
        float distance = halfBeam + EditorSetting.DEV_VEHICLE_SPAWN_CLEARANCE;
        Vector3 feet = player.getWorldPositionStruct().getPosition();
        Vector3 size = player.getSize();

        spawnScratch.setChunkCoordinate(player.getWorldPositionStruct().getChunkCoordinate());
        spawnScratch.getPosition().set(
                feet.x + size.x * 0.5f + forwardX * distance,
                feet.y + centerOfMass.y,
                feet.z + size.z * 0.5f + forwardZ * distance);
        WorldPositionUtility.settleChunk(player.getWorldHandle(), spawnScratch);

        vehicleManager.spawn(
                vehicleHandle,
                player.getWorldHandle(),
                spawnScratch.getChunkCoordinate(),
                spawnScratch.getPosition(),
                (float) Math.atan2(forwardX, forwardZ) + (float) Math.PI * 0.5f);

        log(window.getTitle() + EditorSetting.COMMAND_MESSAGE_VEHICLE_SPAWNED + vehicleHandle.getDisplayName());
    }

    // The vehicle nearest the player within reach leaves the world
    public void removeVehicle() {

        WindowInstance window = context.getWindow();
        int windowID = window.getWindowID();

        if (!playerManager.hasPlayerForWindow(windowID)) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_VEHICLE_NONE_NEAR);
            return;
        }

        EntityInstance player = playerManager.getPlayerForWindow(windowID);
        WorldPositionStruct worldPosition = player.getWorldPositionStruct();
        Vector3 position = worldPosition.getPosition();
        VehicleInstance vehicle = vehicleManager.findVehicleNear(
                player.getWorldHandle(),
                worldPosition.getChunkCoordinate(),
                position.x,
                position.y,
                position.z,
                EditorSetting.DEV_VEHICLE_REMOVE_REACH);

        if (vehicle == null) {
            errorLog(window.getTitle() + EditorSetting.COMMAND_MESSAGE_VEHICLE_NONE_NEAR);
            return;
        }

        vehicleManager.despawn(vehicle);
        log(window.getTitle() + EditorSetting.COMMAND_MESSAGE_VEHICLE_REMOVED
                + vehicle.getVehicleHandle().getDisplayName());
    }
}
