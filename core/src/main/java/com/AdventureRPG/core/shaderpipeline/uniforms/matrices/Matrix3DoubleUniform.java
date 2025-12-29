package com.AdventureRPG.core.shaderpipeline.uniforms.matrices;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix3;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix3Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import java.nio.ByteBuffer;

public class Matrix3DoubleUniform extends UniformAttribute<Matrix3Double> {

    // Internal
    private final ByteBuffer uboBuffer;
    private final Matrix3 uniformBuffer;

    public Matrix3DoubleUniform() {

        // Internal
        super(new Matrix3Double());
        this.uboBuffer = BufferUtils.newByteBuffer(48); // (std140): 3 columns * (vector4) 4 floats * 4 bytes = 48 bytes
        this.uniformBuffer = new Matrix3();
    }

    @Override
    protected void push(int handle, Matrix3Double value) {

        // Column 0
        uniformBuffer.val[0] = (float) value.val[0]; // m00
        uniformBuffer.val[1] = (float) value.val[3]; // m10
        uniformBuffer.val[2] = (float) value.val[6]; // m20

        // Column 1
        uniformBuffer.val[3] = (float) value.val[1]; // m01
        uniformBuffer.val[4] = (float) value.val[4]; // m11
        uniformBuffer.val[5] = (float) value.val[7]; // m21

        // Column 2
        uniformBuffer.val[6] = (float) value.val[2]; // m02
        uniformBuffer.val[7] = (float) value.val[5]; // m12
        uniformBuffer.val[8] = (float) value.val[8]; // m22

        Gdx.gl.glUniformMatrix3fv(handle, 1, false, uniformBuffer.val, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        // Column 0
        uboBuffer.putFloat((float) value.val[0]); // m00
        uboBuffer.putFloat((float) value.val[3]); // m10
        uboBuffer.putFloat((float) value.val[6]); // m20
        uboBuffer.putFloat(0f); // padding

        // Column 1
        uboBuffer.putFloat((float) value.val[1]); // m01
        uboBuffer.putFloat((float) value.val[4]); // m11
        uboBuffer.putFloat((float) value.val[7]); // m21
        uboBuffer.putFloat(0f); // padding

        // Column 2
        uboBuffer.putFloat((float) value.val[2]); // m02
        uboBuffer.putFloat((float) value.val[5]); // m12
        uboBuffer.putFloat((float) value.val[8]); // m22
        uboBuffer.putFloat(0f); // padding

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Matrix3Double value) {
        this.value.set(value);
    }
}