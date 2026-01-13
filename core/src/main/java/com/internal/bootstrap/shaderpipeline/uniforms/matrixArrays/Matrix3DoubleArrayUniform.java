package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.matrices.Matrix3Double;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix3DoubleArrayUniform extends UniformAttribute<Matrix3Double[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix3DoubleArrayUniform(int elementCount) {

        // Internal
        super(new Matrix3Double[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 48);
        this.uniformBuffer = uboBuffer.asFloatBuffer();

        // Initialize array with Matrix3Double instances
        for (int i = 0; i < elementCount; i++)
            value[i] = new Matrix3Double();
    }

    @Override
    protected void push(int handle, Matrix3Double[] matrices) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {

            Matrix3Double matrix = matrices[i];

            // Column 0
            uniformBuffer.put((float) matrix.val[0]); // m00
            uniformBuffer.put((float) matrix.val[3]); // m10
            uniformBuffer.put((float) matrix.val[6]); // m20

            // Column 1
            uniformBuffer.put((float) matrix.val[1]); // m01
            uniformBuffer.put((float) matrix.val[4]); // m11
            uniformBuffer.put((float) matrix.val[7]); // m21

            // Column 2
            uniformBuffer.put((float) matrix.val[2]); // m02
            uniformBuffer.put((float) matrix.val[5]); // m12
            uniformBuffer.put((float) matrix.val[8]); // m22
        }

        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix3fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++) {

            Matrix3Double matrix = value[i];

            // Column 0
            uboBuffer.putFloat((float) matrix.val[0]); // m00
            uboBuffer.putFloat((float) matrix.val[3]); // m10
            uboBuffer.putFloat((float) matrix.val[6]); // m20
            uboBuffer.putFloat(0f); // padding

            // Column 1
            uboBuffer.putFloat((float) matrix.val[1]); // m01
            uboBuffer.putFloat((float) matrix.val[4]); // m11
            uboBuffer.putFloat((float) matrix.val[7]); // m21
            uboBuffer.putFloat(0f); // padding

            // Column 2
            uboBuffer.putFloat((float) matrix.val[2]); // m02
            uboBuffer.putFloat((float) matrix.val[5]); // m12
            uboBuffer.putFloat((float) matrix.val[8]); // m22
            uboBuffer.putFloat(0f); // padding
        }

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Matrix3Double[] matrices) {
        for (int i = 0; i < elementCount; i++)
            this.value[i].set(matrices[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}