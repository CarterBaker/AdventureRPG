package application.bootstrap.vehiclepipeline.vehicle;

import application.bootstrap.geometrypipeline.mesh.MeshInstance;
import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehiclePartStruct extends StructPackage {

    /*
     * One named part of a vehicle: its role, the texture its sub-voxels draw
     * with, the boxes of sub-voxels it fills in the vehicle's model grid and
     * the bounds around them. A part that moves keeps its pivot in model
     * blocks, its axle where it has one, the mast, yard or sail it belongs
     * to once VehicleBuilder links it, and the meshes it is drawn with. The
     * control a part works is its own when it is one, or the one it is a
     * fitting of, so a hand on a wheel's stand takes the wheel.
     * Boxes are six ints each, minimum inclusive and maximum exclusive.
     */

    // Identity
    private final String partName;
    private final VehiclePartRole role;
    private final String textureName;
    private final String linkName;

    // Pivot
    private final Vector3 pivot;
    private final int axis;

    // Links
    private int mastIndex;
    private int yardIndex;
    private int sailIndex;
    private int controlIndex;

    // Boxes
    private final IntArrayList boxes;
    private int minX;
    private int minY;
    private int minZ;
    private int maxX;
    private int maxY;
    private int maxZ;

    // Render
    private final ObjectArrayList<MeshInstance> meshes;

    // Constructor \\

    public VehiclePartStruct(
            String partName,
            VehiclePartRole role,
            String textureName,
            String linkName,
            Vector3 pivot,
            int axis) {

        // Identity
        this.partName = partName;
        this.role = role;
        this.textureName = textureName;
        this.linkName = linkName;

        // Pivot
        this.pivot = pivot;
        this.axis = axis;

        // Links
        this.mastIndex = EngineSetting.INDEX_NOT_FOUND;
        this.yardIndex = EngineSetting.INDEX_NOT_FOUND;
        this.sailIndex = EngineSetting.INDEX_NOT_FOUND;
        this.controlIndex = EngineSetting.INDEX_NOT_FOUND;

        // Boxes
        this.boxes = new IntArrayList();
        this.minX = Integer.MAX_VALUE;
        this.minY = Integer.MAX_VALUE;
        this.minZ = Integer.MAX_VALUE;
        this.maxX = Integer.MIN_VALUE;
        this.maxY = Integer.MIN_VALUE;
        this.maxZ = Integer.MIN_VALUE;

        // Render
        this.meshes = new ObjectArrayList<>();
    }

    // Boxes \\

    public void addBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {

        boxes.add(minX);
        boxes.add(minY);
        boxes.add(minZ);
        boxes.add(maxX);
        boxes.add(maxY);
        boxes.add(maxZ);

        this.minX = Math.min(this.minX, minX);
        this.minY = Math.min(this.minY, minY);
        this.minZ = Math.min(this.minZ, minZ);
        this.maxX = Math.max(this.maxX, maxX);
        this.maxY = Math.max(this.maxY, maxY);
        this.maxZ = Math.max(this.maxZ, maxZ);
    }

    public int getBoxCount() {
        return boxes.size() / EngineSetting.BOX_INT_STRIDE;
    }

    // One bound of a box: 0 to 2 its minimum on x, y and z, 3 to 5 its maximum
    public int getBoxBound(int boxIndex, int bound) {
        return boxes.getInt(boxIndex * EngineSetting.BOX_INT_STRIDE + bound);
    }

    // Links \\

    public void setMastIndex(int mastIndex) {
        this.mastIndex = mastIndex;
    }

    public void setYardIndex(int yardIndex) {
        this.yardIndex = yardIndex;
    }

    public void setSailIndex(int sailIndex) {
        this.sailIndex = sailIndex;
    }

    public void setControlIndex(int controlIndex) {
        this.controlIndex = controlIndex;
    }

    // Render \\

    public void addMesh(MeshInstance meshInstance) {
        meshes.add(meshInstance);
    }

    // Accessible \\

    public String getPartName() {
        return partName;
    }

    public VehiclePartRole getRole() {
        return role;
    }

    public String getTextureName() {
        return textureName;
    }

    public String getLinkName() {
        return linkName;
    }

    public boolean hasLink() {
        return !linkName.isEmpty();
    }

    public Vector3 getPivot() {
        return pivot;
    }

    public int getAxis() {
        return axis;
    }

    public int getMastIndex() {
        return mastIndex;
    }

    public int getYardIndex() {
        return yardIndex;
    }

    public int getSailIndex() {
        return sailIndex;
    }

    // The part whose control this part works, INDEX_NOT_FOUND when it works none
    public int getControlIndex() {
        return controlIndex;
    }

    public boolean isEmpty() {
        return boxes.isEmpty();
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

    public ObjectArrayList<MeshInstance> getMeshes() {
        return meshes;
    }
}
