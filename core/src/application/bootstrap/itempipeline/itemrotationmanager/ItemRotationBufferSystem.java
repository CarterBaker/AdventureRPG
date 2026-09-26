package application.bootstrap.itempipeline.itemrotationmanager;

import application.bootstrap.shaderpipeline.ubo.UBOHandle;
import application.bootstrap.shaderpipeline.ubomanager.UBOManager;
import engine.root.EngineSetting;
import engine.root.SystemPackage;
import engine.util.mathematics.extras.Direction3Vector;
import engine.util.mathematics.matrices.Matrix4;

public class ItemRotationBufferSystem extends SystemPackage {

    /*
     * Pushes the 24 item face-spin rotation matrices to the ItemRotationData
     * UBO once at awake. Never updated again — rotation data is static.
     */

    // Internal
    private UBOManager uboManager;

    // Internal \\

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        pushItemRotationData();
    }

    // Buffer \\

    private void pushItemRotationData() {

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName(EngineSetting.ITEM_ROTATION_DATA_UBO);

        Matrix4[] rotations = new Matrix4[24];

        for (Direction3Vector face : Direction3Vector.VALUES) {
            for (int spin = 0; spin < 4; spin++) {
                int index = face.ordinal() * 4 + spin;
                rotations[index] = buildRotation(face, spin);
            }
        }

        ubo.updateUniform(EngineSetting.UNIFORM_ROTATIONS, rotations);
        uboManager.push(ubo);
    }

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