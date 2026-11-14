package application.bootstrap.vehiclepipeline.vehiclemanager;

import application.bootstrap.vehiclepipeline.vehicle.VehicleInstance;
import engine.root.BranchPackage;
import engine.root.EngineSetting;

class VehicleDoorBranch extends BranchPackage {

    /*
     * A vehicle's doors, hatches, lids and portcullises. Once a step each one
     * swings toward open or shut, as VehicleManager last set it, at the rate
     * one full swing takes VEHICLE_DOOR_SWING_SECONDS; how far it stands open
     * is all it keeps, and VehicleSpaceUtility poses and clears it from that.
     */

    // Swing \\

    void swing(VehicleInstance vehicle, float timeStep) {

        float swingStep = timeStep / EngineSetting.VEHICLE_DOOR_SWING_SECONDS;

        for (int doorIndex = 0; doorIndex < vehicle.getVehicleHandle().getDoorCount(); doorIndex++) {

            float target = vehicle.isDoorOpen(doorIndex) ? 1f : 0f;
            float opening = vehicle.getDoorOpening(doorIndex);

            vehicle.setDoorOpening(doorIndex, opening + Math.max(-swingStep, Math.min(swingStep, target - opening)));
        }
    }
}
