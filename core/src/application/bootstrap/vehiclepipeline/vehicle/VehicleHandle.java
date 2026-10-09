package application.bootstrap.vehiclepipeline.vehicle;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import engine.root.EngineSetting;
import engine.root.HandlePackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleHandle extends HandlePackage {

    /*
     * Persistent reference to one loaded vehicle type. Registered and owned by
     * VehicleManager. Delegates all accessors through VehicleData; every
     * vehicle spawned of the type shares it.
     */

    // Internal
    private VehicleData vehicleData;

    // Constructor \\

    public void constructor(VehicleData vehicleData) {

        // Internal
        this.vehicleData = vehicleData;
    }

    // Accessible \\

    public VehicleData getVehicleData() {
        return vehicleData;
    }

    public String getVehicleName() {
        return vehicleData.getVehicleName();
    }

    public String getDisplayName() {
        return vehicleData.getDisplayName();
    }

    public VehicleCategory getCategory() {
        return vehicleData.getCategory();
    }

    public int getPartCount() {
        return vehicleData.getParts().size();
    }

    public VehiclePartStruct getPart(int partIndex) {
        return vehicleData.getParts().get(partIndex);
    }

    public int getMastCount() {
        return vehicleData.getMastParts().size();
    }

    public VehiclePartStruct getMast(int mastIndex) {
        return getPart(vehicleData.getMastParts().getInt(mastIndex));
    }

    public int getSailCount() {
        return vehicleData.getSails().size();
    }

    public VehicleSailStruct getSail(int sailIndex) {
        return vehicleData.getSails().get(sailIndex);
    }

    public boolean hasHelm() {
        return vehicleData.getHelmPart() != EngineSetting.INDEX_NOT_FOUND;
    }

    public VehiclePartStruct getHelm() {
        return getPart(vehicleData.getHelmPart());
    }

    public boolean hasRudder() {
        return vehicleData.getRudderPart() != EngineSetting.INDEX_NOT_FOUND;
    }

    public VehiclePartStruct getRudder() {
        return getPart(vehicleData.getRudderPart());
    }

    public boolean hasCapstan() {
        return vehicleData.getCapstanPart() != EngineSetting.INDEX_NOT_FOUND;
    }

    public int getDoorCount() {
        return vehicleData.getDoorParts().size();
    }

    public VehiclePartStruct getDoor(int doorIndex) {
        return getPart(vehicleData.getDoorParts().getInt(doorIndex));
    }

    public SubVoxelGridStruct getSolidGrid() {
        return vehicleData.getSolidGrid();
    }

    public int getClimbZoneCount() {
        return vehicleData.getClimbZones().size() / EngineSetting.BOX_INT_STRIDE;
    }

    // One bound of a climb zone: 0 to 2 its minimum on x, y and z in model sub-voxels, 3 to 5 its maximum
    public int getClimbZoneBound(int zoneIndex, int bound) {
        return vehicleData.getClimbZones().getInt(zoneIndex * EngineSetting.BOX_INT_STRIDE + bound);
    }

    public ObjectArrayList<MeshInstance> getHullMeshes() {
        return vehicleData.getHullMeshes();
    }

    public int getMinX() {
        return vehicleData.getMinX();
    }

    public int getMinY() {
        return vehicleData.getMinY();
    }

    public int getMinZ() {
        return vehicleData.getMinZ();
    }

    public int getMaxX() {
        return vehicleData.getMaxX();
    }

    public int getMaxY() {
        return vehicleData.getMaxY();
    }

    public int getMaxZ() {
        return vehicleData.getMaxZ();
    }

    public float getBoundingRadius() {
        return vehicleData.getBoundingRadius();
    }

    public VehicleHullStruct getHull() {
        return vehicleData.getHull();
    }

    public VehicleHandlingStruct getHandling() {
        return vehicleData.getHandling();
    }

    public int getCargoSlotCount() {
        return vehicleData.getCargoSlots().size();
    }

    public VehicleCargoSlotStruct getCargoSlot(int slotIndex) {
        return vehicleData.getCargoSlots().get(slotIndex);
    }
}
