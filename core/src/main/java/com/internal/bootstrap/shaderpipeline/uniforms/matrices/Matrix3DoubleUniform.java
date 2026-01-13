package com.internal.bootstrap.shaderpipeline.uniforms.matrices;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.matrices.Matrix3;
import com.internal.core.util.mathematics.matrices.Matrix3Double;

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

        for (int i = 0; i < 9; i++)
            uniformBuffer.val[i] = (float) value.val[i];

        Gdx.gl.glUniformMatrix3fv(handle, 1, false, uniformBuffer.val, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        // Column 0
        uboBuffer.putFloat((float) value.val[0]); // m00
        uboBuffer.putFloat((float) value.val[1]); // m10
        uboBuffer.putFloat((float) value.val[2]); // m20
        uboBuffer.putFloat(0f); // padding

        // Column 1
        uboBuffer.putFloat((float) value.val[3]); // m01
        uboBuffer.putFloat((float) value.val[4]); // m11
        uboBuffer.putFloat((float) value.val[5]); // m21
        uboBuffer.putFloat(0f); // padding

        // Column 2
        uboBuffer.putFloat((float) value.val[6]); // m02
        uboBuffer.putFloat((float) value.val[7]); // m12
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