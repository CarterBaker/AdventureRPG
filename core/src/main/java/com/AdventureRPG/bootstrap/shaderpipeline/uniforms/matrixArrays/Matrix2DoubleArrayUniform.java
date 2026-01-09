package com.AdventureRPG.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.AdventureRPG.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix2Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix2DoubleArrayUniform extends UniformAttribute<Matrix2Double[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix2DoubleArrayUniform(int elementCount) {

        // Internal
        super(new Matrix2Double[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 32);
        this.uniformBuffer = uboBuffer.asFloatBuffer();

        // Initialize array with Matrix2Double instances
        for (int i = 0; i < elementCount; i++)
            value[i] = new Matrix2Double();
    }

    @Override
    protected void push(int handle, Matrix2Double[] matrices) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {

            Matrix2Double matrix = matrices[i];

            // Column 0
            uniformBuffer.put((float) matrix.val[0]); // m00
            uniformBuffer.put((float) matrix.val[2]); // m10

            // Column 1
            uniformBuffer.put((float) matrix.val[1]); // m01
            uniformBuffer.put((float) matrix.val[3]); // m11
        }

        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix2fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++) {

            Matrix2Double matrix = value[i];

            // Column 0
            uboBuffer.putFloat((float) matrix.val[0]); // m00
            uboBuffer.putFloat((float) matrix.val[2]); // m10
            uboBuffer.putFloat(0f); // padding
            uboBuffer.putFloat(0f); // padding

            // Column 1
            uboBuffer.putFloat((float) matrix.val[1]); // m01
            uboBuffer.putFloat((float) matrix.val[3]); // m11
            uboBuffer.putFloat(0f); // padding
            uboBuffer.putFloat(0f); // padding
        }

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Matrix2Double[] matrices) {
        for (int i = 0; i < elementCount; i++)
            this.value[i].set(matrices[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}