package program.bootstrap.itempipeline.itemrotationmanager;

import program.bootstrap.shaderpipeline.ubo.UBOHandle;
import program.bootstrap.shaderpipeline.ubomanager.UBOManager;
import program.core.engine.SystemPackage;
import program.core.util.mathematics.extras.Direction3Vector;
import program.core.util.mathematics.matrices.Matrix4;

public class InternalBufferSystem extends SystemPackage {

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

        UBOHandle ubo = uboManager.getUBOHandleFromUBOName("ItemRotationData");

        Matrix4[] rotations = new Matrix4[24];

        for (Direction3Vector face : Direction3Vector.VALUES) {
            for (int spin = 0; spin < 4; spin++) {
                int index = face.ordinal() * 4 + spin;
                rotations[index] = buildRotation(face, spin);
            }
        }

        ubo.updateUniform("u_rotations", rotations);
        uboManager.push(ubo);
    }

    private Matrix4 buildRotation(Direction3Vector face, int spin) {
        Matrix4 faceRot = faceRotation(face);
        Matrix4 spinRot = axisRotation(face.x, face.y, face.z, spin * 90f);
        return spinRot.multiply(faceRot);
    }

    private Matrix4 faceRotation(Direction3Vector face) {
        switch (face) {
            case UP:
                return new Matrix4();
            case DOWN:
                return rotX(180f);
            case NORTH:
                return rotX(90f);
            case SOUTH:
                return rotX(-90f);
            case EAST:
                return rotZ(-90f);
            case WEST:
                return rotZ(90f);
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