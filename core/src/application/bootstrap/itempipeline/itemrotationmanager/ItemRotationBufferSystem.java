package application.bootstrap.itempipeline.itemrotationmanager;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.matrices.Matrix4;

public class ItemRotationBufferSystem extends SystemPackage {

    /*
     * Builds the 24 item face-spin rotation matrices at create and pushes them
     * to the ItemRotationData UBO once at awake. Never updated again — rotation
     * data is static. The same matrices are kept for code that places world
     * items on the CPU, and rounded into whole quarter turns for code that
     * turns sub-voxel cells, so the shader, the transforms and every cell an
     * item claims always agree on how an item is turned.
     */

    // Internal
    private UBOManager uboManager;

    // Rotations
    private Matrix4[] rotations;

    // Cell Rotations — each orientation's rounded 3x3 rotation, row by row
    private int[] cellRotations;
    private int cellRotationStride;

    // Internal \\

    @Override
    protected void create() {

        // Cell Rotations
        this.cellRotationStride = EngineSetting.AXIS_COUNT * EngineSetting.AXIS_COUNT;

        buildRotations();
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        pushItemRotationData();
    }

    // Buffer \\

    private void buildRotations() {

        this.rotations = new Matrix4[24];
        this.cellRotations = new int[rotations.length * cellRotationStride];

        for (Direction3Vector face : Direction3Vector.VALUES) {
            for (int spin = 0; spin < 4; spin++) {
                int index = face.ordinal() * 4 + spin;
                rotations[index] = buildRotation(face, spin);
                writeCellRotation(index, rotations[index]);
            }
        }
    }

    private void pushItemRotationData() {

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(EngineSetting.ITEM_ROTATION_DATA_UBO);

        ubo.updateUniform(EngineSetting.UNIFORM_ROTATIONS, rotations);
        uboManager.push(ubo);
    }

    private void writeCellRotation(int orientation, Matrix4 rotation) {

        int base = orientation * cellRotationStride;

        cellRotations[base] = Math.round(rotation.getM00());
        cellRotations[base + 1] = Math.round(rotation.getM01());
        cellRotations[base + 2] = Math.round(rotation.getM02());
        cellRotations[base + 3] = Math.round(rotation.getM10());
        cellRotations[base + 4] = Math.round(rotation.getM11());
        cellRotations[base + 5] = Math.round(rotation.getM12());
        cellRotations[base + 6] = Math.round(rotation.getM20());
        cellRotations[base + 7] = Math.round(rotation.getM21());
        cellRotations[base + 8] = Math.round(rotation.getM22());
    }

    // Accessible \\

    // The rotation about the item's block centre for a packed orientation
    public Matrix4 getRotation(int orientation) {
        return rotations[orientation];
    }

    // One axis of the model-grid cell a cell turns into — its centre turned about the grid's centre
    public int rotateCell(int orientation, int axis, int x, int y, int z) {

        int base = orientation * cellRotationStride + axis * EngineSetting.AXIS_COUNT;

        return toCell(cellRotations[base] * toCentre(x)
                + cellRotations[base + 1] * toCentre(y)
                + cellRotations[base + 2] * toCentre(z));
    }

    // One axis of the model-grid cell that turns into this one — the inverse of rotateCell()
    public int unrotateCell(int orientation, int axis, int x, int y, int z) {

        int base = orientation * cellRotationStride + axis;

        return toCell(cellRotations[base] * toCentre(x)
                + cellRotations[base + EngineSetting.AXIS_COUNT] * toCentre(y)
                + cellRotations[base + 2 * EngineSetting.AXIS_COUNT] * toCentre(z));
    }

    // A cell's centre as a doubled offset from the grid's centre, so a quarter turn stays whole
    private static int toCentre(int cell) {
        return 2 * cell + 1 - EngineSetting.SUB_VOXEL_RESOLUTION;
    }

    private static int toCell(int centre) {
        return (centre + EngineSetting.SUB_VOXEL_RESOLUTION - 1) / 2;
    }

    // Build \\

    private Matrix4 buildRotation(Direction3Vector face, int spin) {
        Matrix4 faceRot = faceRotation(face);
        Matrix4 spinRot = axisRotation(face.x, face.y, face.z, spin * EngineSetting.QUARTER_TURN_DEGREES);
        return spinRot.multiply(faceRot);
    }

    private Matrix4 faceRotation(Direction3Vector face) {
        switch (face) {
            case UP:
                return new Matrix4();
            case DOWN:
                return rotX(EngineSetting.HALF_TURN_DEGREES);
            case NORTH:
                return rotX(EngineSetting.QUARTER_TURN_DEGREES);
            case SOUTH:
                return rotX(-EngineSetting.QUARTER_TURN_DEGREES);
            case EAST:
                return rotZ(-EngineSetting.QUARTER_TURN_DEGREES);
            case WEST:
                return rotZ(EngineSetting.QUARTER_TURN_DEGREES);
            default:
                return new Matrix4();
        }
    }

    private Matrix4 rotX(float deg) {
        float r = (float) Math.toRadians(deg);
        float c = (float) Math.cos(r);
        float s = (float) Math.sin(r);
        return new Matrix4(
                1, 0, 0, 0,
                0, c, -s, 0,
                0, s, c, 0,
                0, 0, 0, 1);
    }

    private Matrix4 rotZ(float deg) {
        float r = (float) Math.toRadians(deg);
        float c = (float) Math.cos(r);
        float s = (float) Math.sin(r);
        return new Matrix4(
                c, -s, 0, 0,
                s, c, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1);
    }

    private Matrix4 axisRotation(float ax, float ay, float az, float deg) {

        float r = (float) Math.toRadians(deg);
        float c = (float) Math.cos(r);
        float s = (float) Math.sin(r);
        float t = 1f - c;
        float len = (float) Math.sqrt(ax * ax + ay * ay + az * az);

        if (len == 0f)
            return new Matrix4();

        ax /= len;
        ay /= len;
        az /= len;

        return new Matrix4(
                t * ax * ax + c, t * ax * ay - s * az, t * ax * az + s * ay, 0,
                t * ax * ay + s * az, t * ay * ay + c, t * ay * az - s * ax, 0,
                t * ax * az - s * ay, t * ay * az + s * ax, t * az * az + c, 0,
                0, 0, 0, 1);
    }
}