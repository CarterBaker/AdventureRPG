package com.internal.bootstrap.itempipeline.itemrotationmanager;

import com.internal.bootstrap.shaderpipeline.ubo.UBOData;
import com.internal.bootstrap.shaderpipeline.ubo.UBOHandle;
import com.internal.bootstrap.shaderpipeline.ubomanager.UBOManager;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformData;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformType;
import com.internal.core.engine.ManagerPackage;
import com.internal.core.util.mathematics.Extras.Direction3Vector;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
 * Builds and uploads 24 rotation matrices — one per orientation (facing*4+spin)
 * — into a shared UBO at awake time. Never changes after boot.
 * Requires UniformType.MAT4 to exist in your UniformType enum.
 */
public class ItemRotationManager extends ManagerPackage {

    private InternalBufferSystem internalBufferSystem;

    private UBOManager uboManager;
    private UBOHandle rotationUBOHandle;

    // Internal \\

    @Override
    protected void create() {
        this.internalBufferSystem = create(InternalBufferSystem.class);
    }

    @Override
    protected void get() {
        this.uboManager = get(UBOManager.class);
    }

    @Override
    protected void awake() {
        buildRotationUBO();
    }

    // UBO \\

    private void buildRotationUBO() {

        UBOData data = create(UBOData.class);
        data.constructor("ItemRotationData", UBOData.UNSPECIFIED_BINDING);

        UniformData uniform = create(UniformData.class);
        uniform.constructor(UniformType.MATRIX4, "u_rotations", 24);
        data.addUniform(uniform);

        rotationUBOHandle = uboManager.buildBuffer(data);

        // 24 orientations * 16 floats per mat4 * 4 bytes per float = 1536 bytes
        ByteBuffer buf = ByteBuffer.allocateDirect(24 * 64).order(ByteOrder.nativeOrder());

        for (int facingIdx = 0; facingIdx < Direction3Vector.LENGTH; facingIdx++) {
            Direction3Vector facing = Direction3Vector.VALUES[facingIdx];
            for (int spin = 0; spin < 4; spin++) {
                float[] mat4 = buildOrientationMatrix(facing, spin);
                for (float f : mat4)
                    buf.putFloat(f);
            }
        }

        buf.flip();

        com.internal.bootstrap.shaderpipeline.ubomanager.GLSLUtility
                .updateUniformBuffer(rotationUBOHandle.getGpuHandle(), 0, buf);
    }

    // Matrix Building \\

    private float[] buildOrientationMatrix(Direction3Vector facing, int spin) {
        float[] base = baseRotation(facing);
        float angle = spin * (float) (Math.PI / 2.0);
        float[] spinMat = axisAngleRotation(facing.x, facing.y, facing.z, angle);
        float[] combined = multiply3x3(spinMat, base);
        return toColumnMajorMat4(combined);
    }

    private float[] baseRotation(Direction3Vector facing) {
        switch (facing) {
            case UP:
                return new float[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
            case DOWN:
                return new float[] { 1, 0, 0, 0, -1, 0, 0, 0, -1 };
            case NORTH:
                return new float[] { 1, 0, 0, 0, 0, -1, 0, 1, 0 };
            case SOUTH:
                return new float[] { 1, 0, 0, 0, 0, 1, 0, -1, 0 };
            case EAST:
                return new float[] { 0, 1, 0, -1, 0, 0, 0, 0, 1 };
            case WEST:
                return new float[] { 0, -1, 0, 1, 0, 0, 0, 0, 1 };
            default:
                return new float[] { 1, 0, 0, 0, 1, 0, 0, 0, 1 };
        }
    }

    private float[] axisAngleRotation(float nx, float ny, float nz, float theta) {
        float c = (float) Math.cos(theta);
        float s = (float) Math.sin(theta);
        float t = 1.0f - c;
        return new float[] {
                t * nx * nx + c, t * nx * ny - s * nz, t * nx * nz + s * ny,
                t * nx * ny + s * nz, t * ny * ny + c, t * ny * nz - s * nx,
                t * nx * nz - s * ny, t * ny * nz + s * nx, t * nz * nz + c
        };
    }

    private float[] multiply3x3(float[] A, float[] B) {
        float[] R = new float[9];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                for (int k = 0; k < 3; k++)
                    R[i * 3 + j] += A[i * 3 + k] * B[k * 3 + j];
        return R;
    }

    // Row-major 3x3 → column-major mat4 for GL std140
    private float[] toColumnMajorMat4(float[] R) {
        return new float[] {
                R[0], R[3], R[6], 0,
                R[1], R[4], R[7], 0,
                R[2], R[5], R[8], 0,
                0, 0, 0, 1
        };
    }

    // Accessible \\

    public UBOHandle getRotationUBOHandle() {
        return rotationUBOHandle;
    }
}