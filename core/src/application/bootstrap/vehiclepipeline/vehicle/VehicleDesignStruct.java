package application.bootstrap.vehiclepipeline.vehicle;

import application.bootstrap.furnishingpipeline.furnishing.FurnishingSlotStruct;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleDesignStruct extends StructPackage {

    /*
     * Everything a vehicle type is assembled from, however it was written: its
     * names and category, its parts in the order they fill the model grid with
     * their boxes laid in, its design draft and centre of mass height in
     * blocks, how its data tunes its handling, and the places it comes
     * furnished at. A vehicle file and a ship drawn at the drafting table both
     * become one, and VehicleManager assembles every one the same way.
     */

    // Identity
    private final String vehicleName;
    private final String displayName;
    private final VehicleCategory category;

    // Parts
    private final ObjectArrayList<VehiclePartStruct> parts;

    // Hull — model blocks
    private final float draft;
    private final float centerOfMassHeight;

    // Handling
    private final VehicleHandlingStruct handling;

    // Cargo
    private final ObjectArrayList<FurnishingSlotStruct> cargoSlots;

    // Constructor \\

    public VehicleDesignStruct(
            String vehicleName,
            String displayName,
            VehicleCategory category,
            ObjectArrayList<VehiclePartStruct> parts,
            float draft,
            float centerOfMassHeight,
            VehicleHandlingStruct handling,
            ObjectArrayList<FurnishingSlotStruct> cargoSlots) {

        // Identity
        this.vehicleName = vehicleName;
        this.displayName = displayName;
        this.category = category;

        // Parts
        this.parts = parts;

        // Hull
        this.draft = draft;
        this.centerOfMassHeight = centerOfMassHeight;

        // Handling
        this.handling = handling;

        // Cargo
        this.cargoSlots = cargoSlots;
    }

    // Accessible \\

    public String getVehicleName() {
        return vehicleName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public VehicleCategory getCategory() {
        return category;
    }

    public ObjectArrayList<VehiclePartStruct> getParts() {
        return parts;
    }

    public float getDraft() {
        return draft;
    }

    public float getCenterOfMassHeight() {
        return centerOfMassHeight;
    }

    public VehicleHandlingStruct getHandling() {
        return handling;
    }

    public ObjectArrayList<FurnishingSlotStruct> getCargoSlots() {
        return cargoSlots;
    }
}
