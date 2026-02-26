package com.internal.bootstrap.shaderpipeline.uniforms.matrixArrays;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.matrices.Matrix2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix2ArrayUniform extends UniformAttribute<Matrix2[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix2ArrayUniform(int elementCount) {
        // Internal
        super(new Matrix2[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 32); // (std140): 2 columns * (vec4 padded) 4 floats *
                                                                       // 4 bytes = 32 bytes per element
        this.uniformBuffer = uboBuffer.asFloatBuffer();
        for (int i = 0; i < elementCount; i++)
            value[i] = new Matrix2();
    }

    @Override
    public UniformAttribute<?> createDefault() {
        return new Matrix2ArrayUniform(elementCount);
    }

    @Override
    protected void push(int handle, Matrix2[] matrices) {
        uniformBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix2 m = matrices[i];
            // Column 0
            uniformBuffer.put(m.val[0]); // m00
            uniformBuffer.put(m.val[1]); // m10
            // Column 1
            uniformBuffer.put(m.val[2]); // m01
            uniformBuffer.put(m.val[3]); // m11
        }
        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix2fv(handle, elementCount, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        uboBuffer.clear();
        for (int i = 0; i < elementCount; i++) {
            Matrix2 m = value[i];
            // Column 0
            uboBuffer.putFloat(m.val[0]); // m00
            uboBuffer.putFloat(m.val[1]); // m10
            uboBuffer.putFloat(0f); // padding
            uboBuffer.putFloat(0f); // padding
            // Column 1
            uboBuffer.putFloat(m.val[2]); // m01
            uboBuffer.putFloat(m.val[3]); // m11
            uboBuffer.putFloat(0f); // padding
            uboBuffer.putFloat(0f); // padding
        }
        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    protected void applyValue(Matrix2[] matrices) {
        for (int i = 0; i < elementCount; i++)
            this.value[i].set(matrices[i]);
    }

    public int elementCount() {
        return elementCount;
    }
}