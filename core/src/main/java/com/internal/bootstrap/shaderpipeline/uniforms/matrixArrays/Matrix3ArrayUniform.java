package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.matrices.Matrix3;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix3ArrayUniform extends UniformAttribute<Object> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix3ArrayUniform(int elementCount) {

        // Internal
        super(new Matrix3[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 48);
        this.uniformBuffer = uboBuffer.asFloatBuffer();

        // Initialize array with Matrix3 instances
        for (int i = 0; i < elementCount; i++)
            ((Matrix3[]) value)[i] = new Matrix3();
    }

    @Override
    protected void push(int handle, Object value) {

        uniformBuffer.clear();

        // Push libGDX matrix array
        if (value instanceof com.badlogic.gdx.math.Matrix3[] gdxMatrices) {

            for (int i = 0; i < elementCount; i++) {

                com.badlogic.gdx.math.Matrix3 m = gdxMatrices[i];

                // Column 0
                uniformBuffer.put(m.val[0]);
                uniformBuffer.put(m.val[1]);
                uniformBuffer.put(m.val[2]);

                // Column 1
                uniformBuffer.put(m.val[3]);
                uniformBuffer.put(m.val[4]);
                uniformBuffer.put(m.val[5]);

                // Column 2
                uniformBuffer.put(m.val[6]);
                uniformBuffer.put(m.val[7]);
                uniformBuffer.put(m.val[8]);
            }
        }

        // Push internal matrix array
        else if (value instanceof Matrix3[] internalMatrices) {

            for (int i = 0; i < elementCount; i++) {

                Matrix3 m = internalMatrices[i];

                // Column 0
                uniformBuffer.put(m.val[0]);
                uniformBuffer.put(m.val[1]);
                uniformBuffer.put(m.val[2]);

                // Column 1
                uniformBuffer.put(m.val[3]);
                uniformBuffer.put(m.val[4]);
                uniformBuffer.put(m.val[5]);

                // Column 2
                uniformBuffer.put(m.val[6]);
                uniformBuffer.put(m.val[7]);
                uniformBuffer.put(m.val[8]);
            }
        }

        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix3fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        Matrix3[] matrices = (Matrix3[]) value;
        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++) {

            Matrix3 m = matrices[i];

            // Column 0
            uboBuffer.putFloat(m.val[0]); // m00
            uboBuffer.putFloat(m.val[3]); // m10
            uboBuffer.putFloat(m.val[6]); // m20
            uboBuffer.putFloat(0f); // padding

            // Column 1
            uboBuffer.putFloat(m.val[1]); // m01
            uboBuffer.putFloat(m.val[4]); // m11
            uboBuffer.putFloat(m.val[7]); // m21
            uboBuffer.putFloat(0f); // padding

            // Column 2
            uboBuffer.putFloat(m.val[2]); // m02
            uboBuffer.putFloat(m.val[5]); // m12
            uboBuffer.putFloat(m.val[8]); // m22
            uboBuffer.putFloat(0f); // padding
        }

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Object value) {

        Matrix3[] target = (Matrix3[]) this.value;

        // From libGDX matrix array
        if (value instanceof com.badlogic.gdx.math.Matrix3[])
            for (int i = 0; i < elementCount; i++)
                target[i].fromGDX(((com.badlogic.gdx.math.Matrix3[]) value)[i]);

        // From internal Matrix3 array
        else if (value instanceof Matrix3[])
            for (int i = 0; i < elementCount; i++)
                target[i].set(((Matrix3[]) value)[i]);

        else // TODO: Add my own error
            throw new IllegalArgumentException(
                    "Expected Matrix3[] or com.badlogic.gdx.math.Matrix3[], got " + value.getClass());
    }

    public int elementCount() {
        return elementCount;
    }
}