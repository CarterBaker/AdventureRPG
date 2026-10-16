package editor.dev.vehicle;

import application.bootstrap.entitypipeline.entity.EntityInstance;
import application.bootstrap.entitypipeline.playermanager.PlayerManager;
import application.bootstrap.vehiclepipeline.vehicle.VehicleHandle;
import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import application.bootstrap.vehiclepipeline.vehiclemanager.VehicleManager;
import application.bootstrap.worldpipeline.util.WorldPositionStruct;
import application.kernel.windowpipeline.window.WindowInstance;
import editor.runtime.EditorSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.vectors.Vector3;

public class DevVehicleSystem extends SystemPackage {

    /*
     * Spawns and removes vehicles around this Dev window's player for testing.
     * Driven by the command console's spawnvehicle command, which names a
     * vehicle by its full name or by a local or display name only one vehicle
     * carries, and its removevehicle command. A vehicle is launched beside the
     * player along its view through VehicleManager.spawnBeside(). A
     * free-flying window has no character to spawn beside.
     */

    // Internal
    private PlayerManager playerManager;
    private VehicleManager vehicleManager;

    // Base \\

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

        vehicleManager.spawnBeside(
                vehicleHandle,
                playerManager.getPlayerForWindow(windowID),
                playerManager.getCameraForWindow(windowID).getDirection());

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
