package application.bootstrap.vehiclepipeline.vehicle;

import engine.root.EngineSetting;
import engine.root.StructPackage;
import engine.util.mathematics.vectors.Vector3;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

public class VehicleHullStruct extends StructPackage {

    /*
     * Everything VehicleBuilder works out about a hull from its watertight
     * parts. The dry volume is a grid of cells the sea cannot reach inside the
     * hull; its columns each sample the sea once and flood at their deck edge,
     * its lumps are what the sea lifts, and the hull displaces exactly its own
     * mass at its design draft, so it floats there, level. Its centre
     * of mass lies over the centroid of that displacement at the height the
     * data sets, and it turns about it with the inertia its length and beam
     * give it. Contact points are where the hull's keel and ends meet the land.
     * The mask is the hull's outline at the waterline, sampled at stations
     * along its length, that keeps the sea surface out of it. Positions are
     * model blocks, mass is tonnes.
     */

    // Mass
    private final float mass;
    private final Vector3 inertia;
    private final Vector3 centerOfMass;
    private final float draft;

    // Dry Volume — cells of VEHICLE_DRY_CELL_SUB_VOXELS on a side, x fastest
    private final boolean[] dryCells;
    private final int dryOriginX;
    private final int dryOriginY;
    private final int dryOriginZ;
    private final int dryCellsX;
    private final int dryCellsY;
    private final int dryCellsZ;
    private final float dryVolume;

    // Columns
    private final ObjectArrayList<VehicleColumnStruct> columns;
    private final int edgeColumnCount;

    // Lumps
    private final ObjectArrayList<VehicleLumpStruct> lumps;

    // Contact — three floats each
    private final FloatArrayList contactPoints;

    // Mask
    private final float maskMinX;
    private final float maskLength;
    private final float maskBottom;
    private final float maskTop;
    private final float maskCenterZ;
    private final float[] maskHalfBeams;

    // Constructor \\

    public VehicleHullStruct(
            float mass,
            Vector3 inertia,
            Vector3 centerOfMass,
            float draft,
            boolean[] dryCells,
            int dryOriginX,
            int dryOriginY,
            int dryOriginZ,
            int dryCellsX,
            int dryCellsY,
            int dryCellsZ,
            float dryVolume,
            ObjectArrayList<VehicleColumnStruct> columns,
            ObjectArrayList<VehicleLumpStruct> lumps,
            FloatArrayList contactPoints,
            float maskMinX,
            float maskLength,
            float maskBottom,
            float maskTop,
            float maskCenterZ,
            float[] maskHalfBeams) {

        // Mass
        this.mass = mass;
        this.inertia = inertia;
        this.centerOfMass = centerOfMass;
        this.draft = draft;

        // Dry Volume
        this.dryCells = dryCells;
        this.dryOriginX = dryOriginX;
        this.dryOriginY = dryOriginY;
        this.dryOriginZ = dryOriginZ;
        this.dryCellsX = dryCellsX;
        this.dryCellsY = dryCellsY;
        this.dryCellsZ = dryCellsZ;
        this.dryVolume = dryVolume;

        // Columns
        this.columns = columns;
        this.edgeColumnCount = countEdgeColumns(columns);

        // Lumps
        this.lumps = lumps;

        // Contact
        this.contactPoints = contactPoints;

        // Mask
        this.maskMinX = maskMinX;
        this.maskLength = maskLength;
        this.maskBottom = maskBottom;
        this.maskTop = maskTop;
        this.maskCenterZ = maskCenterZ;
        this.maskHalfBeams = maskHalfBeams;
    }

    private static int countEdgeColumns(ObjectArrayList<VehicleColumnStruct> columns) {

        int count = 0;

        for (int i = 0; i < columns.size(); i++)
            if (columns.get(i).isEdge())
                count++;

        return count;
    }

    // Dry Volume \\

    // True when a point given in model blocks lies in a dry cell of the hull
    public boolean isDry(float x, float y, float z) {

        float resolution = EngineSetting.SUB_VOXEL_RESOLUTION;
        int cellSize = EngineSetting.VEHICLE_DRY_CELL_SUB_VOXELS;
        int cellX = Math.floorDiv((int) Math.floor(x * resolution) - dryOriginX, cellSize);
        int cellY = Math.floorDiv((int) Math.floor(y * resolution) - dryOriginY, cellSize);
        int cellZ = Math.floorDiv((int) Math.floor(z * resolution) - dryOriginZ, cellSize);

        if (cellX < 0 || cellY < 0 || cellZ < 0 || cellX >= dryCellsX || cellY >= dryCellsY || cellZ >= dryCellsZ)
            return false;

        return dryCells[cellX + dryCellsX * (cellY + dryCellsY * cellZ)];
    }

    // Accessible \\

    public float getMass() {
        return mass;
    }

    public Vector3 getInertia() {
        return inertia;
    }

    public Vector3 getCenterOfMass() {
        return centerOfMass;
    }

    public float getDraft() {
        return draft;
    }

    public float getDryVolume() {
        return dryVolume;
    }

    public ObjectArrayList<VehicleColumnStruct> getColumns() {
        return columns;
    }

    public int getEdgeColumnCount() {
        return edgeColumnCount;
    }

    public ObjectArrayList<VehicleLumpStruct> getLumps() {
        return lumps;
    }

    public FloatArrayList getContactPoints() {
        return contactPoints;
    }

    public int getContactPointCount() {
        return contactPoints.size() / EngineSetting.AXIS_COUNT;
    }

    public float getMaskMinX() {
        return maskMinX;
    }

    public float getMaskLength() {
        return maskLength;
    }

    public float getMaskBottom() {
        return maskBottom;
    }

    public float getMaskTop() {
        return maskTop;
    }

    public float getMaskCenterZ() {
        return maskCenterZ;
    }

    public float[] getMaskHalfBeams() {
        return maskHalfBeams;
    }
}
