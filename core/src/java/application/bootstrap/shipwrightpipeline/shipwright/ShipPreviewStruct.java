package application.bootstrap.shipwrightpipeline.shipwright;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import application.bootstrap.vehiclepipeline.vehicle.VehiclePartStruct;
import engine.root.StructPackage;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class ShipPreviewStruct extends StructPackage {

    /*
     * A picture of a fitting the drafting table offers that no item stands
     * for, such as the helm or the capstan: the parts it is built of, made by
     * the very builders a ship's fittings are, meshed at rest as a vehicle
     * meshes them, and the sub-voxel box they fill, to fit the picture into a
     * catalogue tile. Made once when the table's manager wakes and kept.
     */

    // Meshes
    private final ObjectArrayList<MeshInstance> meshes;

    // Bounds — model sub-voxels
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    // Constructor \\

    public ShipPreviewStruct(ObjectArrayList<VehiclePartStruct> parts, ObjectArrayList<MeshInstance> meshes) {

        // Meshes
        this.meshes = meshes;

        // Bounds
        int lowX = Integer.MAX_VALUE;
        int lowY = Integer.MAX_VALUE;
        int lowZ = Integer.MAX_VALUE;
        int highX = Integer.MIN_VALUE;
        int highY = Integer.MIN_VALUE;
        int highZ = Integer.MIN_VALUE;

        for (int i = 0; i < parts.size(); i++) {

            VehiclePartStruct part = parts.get(i);

            lowX = Math.min(lowX, part.getMinX());
            lowY = Math.min(lowY, part.getMinY());
            lowZ = Math.min(lowZ, part.getMinZ());
            highX = Math.max(highX, part.getMaxX());
            highY = Math.max(highY, part.getMaxY());
            highZ = Math.max(highZ, part.getMaxZ());
        }

        this.minX = lowX;
        this.minY = lowY;
        this.minZ = lowZ;
        this.maxX = highX;
        this.maxY = highY;
        this.maxZ = highZ;
    }

    // Accessible \\

    public ObjectArrayList<MeshInstance> getMeshes() {
        return meshes;
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

    public int getSizeX() {
        return maxX - minX;
    }

    public int getSizeY() {
        return maxY - minY;
    }

    public int getSizeZ() {
        return maxZ - minZ;
    }
}
