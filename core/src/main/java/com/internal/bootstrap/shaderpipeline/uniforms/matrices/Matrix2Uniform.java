package com.internal.bootstrap.shaderpipeline.uniforms.matrices;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;
import com.internal.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.internal.core.util.mathematics.matrices.Matrix2;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix2Uniform extends UniformAttribute<Matrix2> {

    // Internal
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix2Uniform() {

        // Internal
        super(new Matrix2());
        this.uboBuffer = BufferUtils.newByteBuffer(32); // (std140): 2 columns * (vector4) 4 floats * 4 bytes = 32 bytes
        this.uniformBuffer = uboBuffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix2 value) {

        uniformBuffer.clear();

        // Column 0
        uniformBuffer.put(value.val[0]); // m00
        uniformBuffer.put(value.val[1]); // m10

        // Column 1
        uniformBuffer.put(value.val[2]); // m01
        uniformBuffer.put(value.val[3]); // m11

        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix2fv(handle, 1, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        // Column 0
        uboBuffer.putFloat(value.val[0]); // m00
        uboBuffer.putFloat(value.val[1]); // m10
        uboBuffer.putFloat(0f); // padding
        uboBuffer.putFloat(0f); // padding

        // Column 1
        uboBuffer.putFloat(value.val[2]); // m01
        uboBuffer.putFloat(value.val[3]); // m11
        uboBuffer.putFloat(0f); // padding
        uboBuffer.putFloat(0f); // padding

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Matrix2 value) {
        this.value.set(value);
        super.set(value);
    }

}