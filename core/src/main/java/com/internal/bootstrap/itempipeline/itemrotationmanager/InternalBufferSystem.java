package com.internal.bootstrap.itempipeline.itemrotationmanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.core.engine.SystemPackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;
import com.internal.core.util.mathematics.matrices.Matrix4;

public class InternalBufferSystem extends SystemPackage {

    private UBOManager uboManager;

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        pushItemRotationData();
    }

    // Item Rotation Data \\

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
        ubo.push();
    }

    private Matrix4 buildRotation(Direction3Vector face, int spin) {
        Matrix4 faceRot = faceRotation(face);
        Matrix4 spinRot = axisRotation(face.x, face.y, face.z, spin * 90f);
        return spinRot.multiply(faceRot);
    }

    // Rotation around Y axis by degrees
    private Matrix4 rotY(float deg) {
        float r = (float) Math.toRadians(deg);
        float c = (float) Math.cos(r);
        float s = (float) Math.sin(r);
        return new Matrix4(
                c, 0, s, 0,
                0, 1, 0, 0,
                -s, 0, c, 0,
                0, 0, 0, 1);
    }

    // Rotation around X axis by degrees
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

    // Rodrigues axis-angle rotation
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

    private Matrix4 faceRotation(Direction3Vector face) {
        switch (face) {
            case SOUTH:
                return new Matrix4(); // identity
            case EAST:
                return rotY(-90f);
            case NORTH:
                return rotY(180f);
            case WEST:
                return rotY(90f);
            case UP:
                return rotX(-90f);
            case DOWN:
                return rotX(90f);
            default:
                return new Matrix4();
        }
    }
}