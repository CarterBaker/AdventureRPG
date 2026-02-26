package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.matrices.Matrix4;

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
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 64); // (std140): 4 columns * (vec4) 4 floats * 4
                                                                       // bytes = 64 bytes per element
        this.uniformBuffer = uboBuffer.asFloatBuffer();
        for (int i = 0; i < elementCount; i++)
            ((Matrix4[]) value)[i] = new Matrix4();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix4ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Object value) {
        uniformBuffer.clear();
        if (value instanceof com.badlogic.gdx.math.Matrix4[] gdxMatrices) {
            for (int i = 0; i < elementCount; i++) {
                com.badlogic.gdx.math.Matrix4 m = gdxMatrices[i];
                // Column 0
                uniformBuffer.put(m.val[0]);
                uniformBuffer.put(m.val[1]);
                uniformBuffer.put(m.val[2]);
                uniformBuffer.put(m.val[3]);
                // Column 1
                uniformBuffer.put(m.val[4]);
                uniformBuffer.put(m.val[5]);
                uniformBuffer.put(m.val[6]);
                uniformBuffer.put(m.val[7]);
                // Column 2
                uniformBuffer.put(m.val[8]);
                uniformBuffer.put(m.val[9]);
                uniformBuffer.put(m.val[10]);
                uniformBuffer.put(m.val[11]);
                // Column 3
                uniformBuffer.put(m.val[12]);
                uniformBuffer.put(m.val[13]);
                uniformBuffer.put(m.val[14]);
                uniformBuffer.put(m.val[15]);
            }
        } else if (value instanceof Matrix4[] internalMatrices) {
            for (int i = 0; i < elementCount; i++) {
                Matrix4 m = internalMatrices[i];
                // Column 0
                uniformBuffer.put(m.val[0]);
                uniformBuffer.put(m.val[1]);
                uniformBuffer.put(m.val[2]);
                uniformBuffer.put(m.val[3]);
                // Column 1
                uniformBuffer.put(m.val[4]);
                uniformBuffer.put(m.val[5]);
                uniformBuffer.put(m.val[6]);
                uniformBuffer.put(m.val[7]);
                // Column 2
                uniformBuffer.put(m.val[8]);
                uniformBuffer.put(m.val[9]);
                uniformBuffer.put(m.val[10]);
                uniformBuffer.put(m.val[11]);
                // Column 3
                uniformBuffer.put(m.val[12]);
                uniformBuffer.put(m.val[13]);
                uniformBuffer.put(m.val[14]);
                uniformBuffer.put(m.val[15]);
            }
        } else {
            throw new IllegalArgumentException(
                    "push(int, Matrix4[]): Expected Matrix4[] or com.badlogic.gdx.math.Matrix4[], got "
                            + value.getClass());
        }
        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix4fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        Matrix4[] matrices = (Matrix4[]) value;
        uboBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix4 m = matrices[i];
            // Column 0
            uboBuffer.putFloat(m.val[0]); // m00
            uboBuffer.putFloat(m.val[4]); // m10
            uboBuffer.putFloat(m.val[8]); // m20
            uboBuffer.putFloat(m.val[12]); // m30
            // Column 1
            uboBuffer.putFloat(m.val[1]); // m01
            uboBuffer.putFloat(m.val[5]); // m11
            uboBuffer.putFloat(m.val[9]); // m21
            uboBuffer.putFloat(m.val[13]); // m31
            // Column 2
            uboBuffer.putFloat(m.val[2]); // m02
            uboBuffer.putFloat(m.val[6]); // m12
            uboBuffer.putFloat(m.val[10]); // m22
            uboBuffer.putFloat(m.val[14]); // m32
            // Column 3
            uboBuffer.putFloat(m.val[3]); // m03
            uboBuffer.putFloat(m.val[7]); // m13
            uboBuffer.putFloat(m.val[11]); // m23
            uboBuffer.putFloat(m.val[15]); // m33
        }
        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    protected void applyValue(Object value) {
        Matrix4[] target = (Matrix4[]) this.value;
        if (value instanceof com.badlogic.gdx.math.Matrix4[] gdxMatrices)
            for (int i = 0; i < elementCount; i++)
                target[i].fromGDX(gdxMatrices[i]);
        else if (value instanceof Matrix4[] internalMatrices)
            for (int i = 0; i < elementCount; i++)
                target[i].set(internalMatrices[i]);
        else
            throw new IllegalArgumentException(
                    "applyValue(Matrix4[]): Expected Matrix4[] or com.badlogic.gdx.math.Matrix4[], got "
                            + value.getClass());
    }

    @Override
    protected void applyObject(Object value) {
        applyValue(value);
    }

    public int elementCount() {
        return elementCount;
    }
}