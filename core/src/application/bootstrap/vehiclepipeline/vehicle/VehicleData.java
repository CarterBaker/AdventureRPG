package application.bootstrap.vehiclepipeline.vehicle;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.geometrypipeline.subvoxel.SubVoxelGridStruct;
import engine.root.DataPackage;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleData extends DataPackage {

    /*
     * Immutable vehicle definition built from ARPG by VehicleBuilder. Holds its
     * identity and category, every part in data order with the masts, sails, helm, rudder,
     * capstan and doors its rig and controls work through, the solid sub-voxels
     * riders and cargo collide with, the zones its ladders can be climbed in,
     * the merged meshes of every part that never moves, the bounds of its
     * model grid, the hull's mass and dry volume, how it handles, and the
     * places it comes furnished at, each with its table of items. Owned by
     * VehicleHandle for the engine lifetime and shared by every vehicle of
     * the type.
     */

    // Identity
    private final String vehicleName;
    private final String displayName;
    private final VehicleCategory category;

    // Parts
    private final ObjectArrayList<VehiclePartStruct> parts;
    private final IntArrayList mastParts;
    private final ObjectArrayList<VehicleSailStruct> sails;
    private final int helmPart;
    private final int rudderPart;
    private final int capstanPart;
    private final IntArrayList doorParts;

    // Grid
    private final SubVoxelGridStruct solidGrid;
    private final IntArrayList climbZones;

    // Render
    private final ObjectArrayList<MeshInstance> hullMeshes;

    // Bounds — model sub-voxels, minimum inclusive and maximum exclusive
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;
    private final float boundingRadius;

    // Physics
    private final VehicleHullStruct hull;
    private final VehicleHandlingStruct handling;

    // Cargo
    private final ObjectArrayList<VehicleCargoSlotStruct> cargoSlots;

    // Constructor \\

    public VehicleData(
            String vehicleName,
            String displayName,
            VehicleCategory category,
            ObjectArrayList<VehiclePartStruct> parts,
            IntArrayList mastParts,
            ObjectArrayList<VehicleSailStruct> sails,
            int helmPart,
            int rudderPart,
            int capstanPart,
            IntArrayList doorParts,
            SubVoxelGridStruct solidGrid,
            IntArrayList climbZones,
            ObjectArrayList<MeshInstance> hullMeshes,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            float boundingRadius,
            VehicleHullStruct hull,
            VehicleHandlingStruct handling,
            ObjectArrayList<VehicleCargoSlotStruct> cargoSlots) {

        // Identity
        this.vehicleName = vehicleName;
        this.displayName = displayName;
        this.category = category;

        // Parts
        this.parts = parts;
        this.mastParts = mastParts;
        this.sails = sails;
        this.helmPart = helmPart;
        this.rudderPart = rudderPart;
        this.capstanPart = capstanPart;
        this.doorParts = doorParts;

        // Grid
        this.solidGrid = solidGrid;
        this.climbZones = climbZones;

        // Render
        this.hullMeshes = hullMeshes;

        // Bounds
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.boundingRadius = boundingRadius;

        // Physics
        this.hull = hull;
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

    public IntArrayList getMastParts() {
        return mastParts;
    }

    public ObjectArrayList<VehicleSailStruct> getSails() {
        return sails;
    }

    public int getHelmPart() {
        return helmPart;
    }

    public int getRudderPart() {
        return rudderPart;
    }

    public int getCapstanPart() {
        return capstanPart;
    }

    public IntArrayList getDoorParts() {
        return doorParts;
    }

    public SubVoxelGridStruct getSolidGrid() {
        return solidGrid;
    }

    public IntArrayList getClimbZones() {
        return climbZones;
    }

    public ObjectArrayList<MeshInstance> getHullMeshes() {
        return hullMeshes;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public float getBoundingRadius() {
        return boundingRadius;
    }

    public VehicleHullStruct getHull() {
        return hull;
    }

    public VehicleHandlingStruct getHandling() {
        return handling;
    }

    public ObjectArrayList<VehicleCargoSlotStruct> getCargoSlots() {
        return cargoSlots;
    }
}
