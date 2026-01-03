package com.AdventureRPG.core.shaderpipeline.uniforms.matrixArrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix4ArrayUniform extends UniformAttribute<Object> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix4ArrayUniform(int elementCount) {

        // Internal
        super(new Matrix4[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 64);
        this.uniformBuffer = uboBuffer.asFloatBuffer();

        // Initialize array with Matrix4 instances
        for (int i = 0; i < elementCount; i++)
            ((Matrix4[]) value)[i] = new Matrix4();
    }

    @Override
    protected void push(int handle, Object value) {

        uniformBuffer.clear();

        // Push libGDX matrix array
        if (value instanceof com.badlogic.gdx.math.Matrix4[] gdxMatrices) {
            for (int i = 0; i < elementCount; i++) {

                com.badlogic.gdx.math.Matrix4 matrix = gdxMatrices[i];

                // Column 0
                uniformBuffer.put(matrix.val[0]);
                uniformBuffer.put(matrix.val[1]);
                uniformBuffer.put(matrix.val[2]);
                uniformBuffer.put(matrix.val[3]);

                // Column 1
                uniformBuffer.put(matrix.val[4]);
                uniformBuffer.put(matrix.val[5]);
                uniformBuffer.put(matrix.val[6]);
                uniformBuffer.put(matrix.val[7]);

                // Column 2
                uniformBuffer.put(matrix.val[8]);
                uniformBuffer.put(matrix.val[9]);
                uniformBuffer.put(matrix.val[10]);
                uniformBuffer.put(matrix.val[11]);

                // Column 3
                uniformBuffer.put(matrix.val[12]);
                uniformBuffer.put(matrix.val[13]);
                uniformBuffer.put(matrix.val[14]);
                uniformBuffer.put(matrix.val[15]);
            }
        }

        // Push internal matrix array
        else if (value instanceof Matrix4[] internalMatrices) {
            for (int i = 0; i < elementCount; i++) {

                Matrix4 matrix = internalMatrices[i];

                // Column 0
                uniformBuffer.put(matrix.val[0]);
                uniformBuffer.put(matrix.val[1]);
                uniformBuffer.put(matrix.val[2]);
                uniformBuffer.put(matrix.val[3]);

                // Column 1
                uniformBuffer.put(matrix.val[4]);
                uniformBuffer.put(matrix.val[5]);
                uniformBuffer.put(matrix.val[6]);
                uniformBuffer.put(matrix.val[7]);

                // Column 2
                uniformBuffer.put(matrix.val[8]);
                uniformBuffer.put(matrix.val[9]);
                uniformBuffer.put(matrix.val[10]);
                uniformBuffer.put(matrix.val[11]);

                // Column 3
                uniformBuffer.put(matrix.val[12]);
                uniformBuffer.put(matrix.val[13]);
                uniformBuffer.put(matrix.val[14]);
                uniformBuffer.put(matrix.val[15]);
            }
        }

        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix4fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        Matrix4[] matrices = (Matrix4[]) value;
        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++) {

            Matrix4 matrix = matrices[i];

            // Column 0
            uboBuffer.putFloat(matrix.val[0]); // m00
            uboBuffer.putFloat(matrix.val[4]); // m10
            uboBuffer.putFloat(matrix.val[8]); // m20
            uboBuffer.putFloat(matrix.val[12]); // m30

            // Column 1
            uboBuffer.putFloat(matrix.val[1]); // m01
            uboBuffer.putFloat(matrix.val[5]); // m11
            uboBuffer.putFloat(matrix.val[9]); // m21
            uboBuffer.putFloat(matrix.val[13]); // m31

            // Column 2
            uboBuffer.putFloat(matrix.val[2]); // m02
            uboBuffer.putFloat(matrix.val[6]); // m12
            uboBuffer.putFloat(matrix.val[10]); // m22
            uboBuffer.putFloat(matrix.val[14]); // m32

            // Column 3
            uboBuffer.putFloat(matrix.val[3]); // m03
            uboBuffer.putFloat(matrix.val[7]); // m13
            uboBuffer.putFloat(matrix.val[11]); // m23
            uboBuffer.putFloat(matrix.val[15]); // m33
        }

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Object value) {

        Matrix4[] target = (Matrix4[]) this.value;

        // From libGDX matrix array
        if (value instanceof com.badlogic.gdx.math.Matrix4[])
            for (int i = 0; i < elementCount; i++)
                target[i].fromGDX(((com.badlogic.gdx.math.Matrix4[]) value)[i]);

        // From internal Matrix array
        else if (value instanceof Matrix4[])
            for (int i = 0; i < elementCount; i++)
                target[i].set(((Matrix4[]) value)[i]);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "Expected Matrix4[] or com.badlogic.gdx.math.Matrix4[], got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}