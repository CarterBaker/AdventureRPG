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
     * blocks, its axle or hinge where it has one, how far it swings open, the
     * mast, yard, sail or door it belongs to once assembly links it, and
     * the meshes it is drawn with. The
     * control a part works is its own when it is one, or the one it is a
     * fitting of, so a hand on a wheel's stand takes the wheel.
     * Boxes are six ints each, minimum inclusive and maximum exclusive. Rope
     * and netting are flat quads instead, drawn from both sides with their
     * texture laid on them and never filling a sub-voxel: a face index then
     * four corners in winding order, in sub-voxels, thirteen ints each.
     */

    // Identity
    private final String partName;
    private final VehiclePartRole role;
    private final String textureName;
    private final String linkName;

    // Pivot
    private final Vector3 pivot;
    private final int axis;
    private final float openAngle;

    // Links
    private int mastIndex;
    private int yardIndex;
    private int sailIndex;
    private int doorIndex;
    private int controlIndex;

    // Boxes
    private final IntArrayList boxes;

    // Quads
    private final IntArrayList quads;

    // Bounds — around every box and quad
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
            int axis,
            float openAngle) {

        // Identity
        this.partName = partName;
        this.role = role;
        this.textureName = textureName;
        this.linkName = linkName;

        // Pivot
        this.pivot = pivot;
        this.axis = axis;
        this.openAngle = openAngle;

        // Links
        this.mastIndex = EngineSetting.INDEX_NOT_FOUND;
        this.yardIndex = EngineSetting.INDEX_NOT_FOUND;
        this.sailIndex = EngineSetting.INDEX_NOT_FOUND;
        this.doorIndex = EngineSetting.INDEX_NOT_FOUND;
        this.controlIndex = EngineSetting.INDEX_NOT_FOUND;

        // Boxes
        this.boxes = new IntArrayList();

        // Quads
        this.quads = new IntArrayList();

        // Bounds
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

        includeBounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public int getBoxCount() {
        return boxes.size() / EngineSetting.BOX_INT_STRIDE;
    }

    // One bound of a box: 0 to 2 its minimum on x, y and z, 3 to 5 its maximum
    public int getBoxBound(int boxIndex, int bound) {
        return boxes.getInt(boxIndex * EngineSetting.BOX_INT_STRIDE + bound);
    }

    // Quads \\

    // A flat quad through four corners in winding order, read by the shader as the face given
    public void addQuad(
            int face,
            int ax, int ay, int az,
            int bx, int by, int bz,
            int cx, int cy, int cz,
            int dx, int dy, int dz) {

        quads.add(face);
        quads.add(ax);
        quads.add(ay);
        quads.add(az);
        quads.add(bx);
        quads.add(by);
        quads.add(bz);
        quads.add(cx);
        quads.add(cy);
        quads.add(cz);
        quads.add(dx);
        quads.add(dy);
        quads.add(dz);

        includeBounds(
                Math.min(Math.min(ax, bx), Math.min(cx, dx)),
                Math.min(Math.min(ay, by), Math.min(cy, dy)),
                Math.min(Math.min(az, bz), Math.min(cz, dz)),
                Math.max(Math.max(ax, bx), Math.max(cx, dx)),
                Math.max(Math.max(ay, by), Math.max(cy, dy)),
                Math.max(Math.max(az, bz), Math.max(cz, dz)));
    }

    public int getQuadCount() {
        return quads.size() / EngineSetting.VEHICLE_QUAD_INT_STRIDE;
    }

    public int getQuadFace(int quadIndex) {
        return quads.getInt(quadIndex * EngineSetting.VEHICLE_QUAD_INT_STRIDE);
    }

    // One coordinate of a quad's corner in sub-voxels: corners 0 to 3 in winding order, axes x, y and z
    public int getQuadCorner(int quadIndex, int corner, int axis) {
        return quads.getInt(quadIndex * EngineSetting.VEHICLE_QUAD_INT_STRIDE + 1
                + corner * EngineSetting.AXIS_COUNT + axis);
    }

    // The box around one quad: 0 to 2 its minimum on x, y and z, 3 to 5 its maximum
    public int getQuadBound(int quadIndex, int bound) {

        int axis = bound % EngineSetting.AXIS_COUNT;
        boolean maximum = bound >= EngineSetting.AXIS_COUNT;
        int value = getQuadCorner(quadIndex, 0, axis);

        for (int corner = 1; corner < EngineSetting.QUAD_VERTEX_COUNT; corner++)
            value = maximum
                    ? Math.max(value, getQuadCorner(quadIndex, corner, axis))
                    : Math.min(value, getQuadCorner(quadIndex, corner, axis));

        return value;
    }

    private void includeBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.minX = Math.min(this.minX, minX);
        this.minY = Math.min(this.minY, minY);
        this.minZ = Math.min(this.minZ, minZ);
        this.maxX = Math.max(this.maxX, maxX);
        this.maxY = Math.max(this.maxY, maxY);
        this.maxZ = Math.max(this.maxZ, maxZ);
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

    public void setDoorIndex(int doorIndex) {
        this.doorIndex = doorIndex;
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

    // The angle in radians a swinging part turns through as it opens, signed by the way it turns
    public float getOpenAngle() {
        return openAngle;
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

    // The door this part opens and shuts as, INDEX_NOT_FOUND when it is none
    public int getDoorIndex() {
        return doorIndex;
    }

    public boolean isDoor() {
        return doorIndex != EngineSetting.INDEX_NOT_FOUND;
    }

    // The part whose control this part works, INDEX_NOT_FOUND when it works none
    public int getControlIndex() {
        return controlIndex;
    }

    public boolean isEmpty() {
        return boxes.isEmpty() && quads.isEmpty();
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
