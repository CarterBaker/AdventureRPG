package com.AdventureRPG.core.shaderpipeline.uniforms.matrixArrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix4Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix4DoubleArrayUniform extends UniformAttribute<Matrix4Double[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix4DoubleArrayUniform(int elementCount) {

        // Internal
        super(new Matrix4Double[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 64);
        this.uniformBuffer = uboBuffer.asFloatBuffer();

        // Initialize array with Matrix4Double instances
        for (int i = 0; i < elementCount; i++)
            value[i] = new Matrix4Double();
    }

    @Override
    protected void push(int handle, Matrix4Double[] matrices) {

        uniformBuffer.clear();

        for (int i = 0; i < elementCount; i++) {

            Matrix4Double matrix = matrices[i];

            // Column 0
            uniformBuffer.put((float) matrix.val[0]); // m00
            uniformBuffer.put((float) matrix.val[4]); // m10
            uniformBuffer.put((float) matrix.val[8]); // m20
            uniformBuffer.put((float) matrix.val[12]); // m30

            // Column 1
            uniformBuffer.put((float) matrix.val[1]); // m01
            uniformBuffer.put((float) matrix.val[5]); // m11
            uniformBuffer.put((float) matrix.val[9]); // m21
            uniformBuffer.put((float) matrix.val[13]); // m31

            // Column 2
            uniformBuffer.put((float) matrix.val[2]); // m02
            uniformBuffer.put((float) matrix.val[6]); // m12
            uniformBuffer.put((float) matrix.val[10]); // m22
            uniformBuffer.put((float) matrix.val[14]); // m32

            // Column 3
            uniformBuffer.put((float) matrix.val[3]); // m03
            uniformBuffer.put((float) matrix.val[7]); // m13
            uniformBuffer.put((float) matrix.val[11]); // m23
            uniformBuffer.put((float) matrix.val[15]); // m33
        }

        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix4fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++) {

            Matrix4Double matrix = value[i];

            // Column 0
            uboBuffer.putFloat((float) matrix.val[0]); // m00
            uboBuffer.putFloat((float) matrix.val[4]); // m10
            uboBuffer.putFloat((float) matrix.val[8]); // m20
            uboBuffer.putFloat((float) matrix.val[12]); // m30

            // Column 1
            uboBuffer.putFloat((float) matrix.val[1]); // m01
            uboBuffer.putFloat((float) matrix.val[5]); // m11
            uboBuffer.putFloat((float) matrix.val[9]); // m21
            uboBuffer.putFloat((float) matrix.val[13]); // m31

            // Column 2
            uboBuffer.putFloat((float) matrix.val[2]); // m02
            uboBuffer.putFloat((float) matrix.val[6]); // m12
            uboBuffer.putFloat((float) matrix.val[10]); // m22
            uboBuffer.putFloat((float) matrix.val[14]); // m32

            // Column 3
            uboBuffer.putFloat((float) matrix.val[3]); // m03
            uboBuffer.putFloat((float) matrix.val[7]); // m13
            uboBuffer.putFloat((float) matrix.val[11]); // m23
            uboBuffer.putFloat((float) matrix.val[15]); // m33
        }

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Matrix4Double[] matrices) {
        for (int i = 0; i < elementCount; i++)
            this.value[i].set(matrices[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}