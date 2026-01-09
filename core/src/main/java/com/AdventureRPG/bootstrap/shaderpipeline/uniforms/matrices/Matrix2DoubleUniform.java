package com.AdventureRPG.bootstrap.shaderpipeline.uniforms.matrices;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import com.AdventureRPG.bootstrap.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.mathematics.matrices.Matrix2Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

public class Matrix2DoubleUniform extends UniformAttribute<Matrix2Double> {

    // Internal
    private final ByteBuffer uboBuffer;
    private final FloatBuffer uniformBuffer;

    public Matrix2DoubleUniform() {

        // Internal
        super(new Matrix2Double());
        this.uboBuffer = BufferUtils.newByteBuffer(32); // (std140): 2 columns * (vector4) 4 floats * 4 bytes = 32 bytes
        this.uniformBuffer = uboBuffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix2Double value) {

        uniformBuffer.clear();

        // Column 0
        uniformBuffer.put((float) value.val[0]); // m00
        uniformBuffer.put((float) value.val[1]); // m10

        // Column 1
        uniformBuffer.put((float) value.val[2]); // m01
        uniformBuffer.put((float) value.val[3]); // m11

        uniformBuffer.flip();
        Gdx.gl.glUniformMatrix2fv(handle, 1, false, uniformBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        // Column 0
        uboBuffer.putFloat((float) value.val[0]); // m00
        uboBuffer.putFloat((float) value.val[1]); // m10
        uboBuffer.putFloat(0f); // padding
        uboBuffer.putFloat(0f); // padding

        // Column 1
        uboBuffer.putFloat((float) value.val[2]); // m01
        uboBuffer.putFloat((float) value.val[3]); // m11
        uboBuffer.putFloat(0f); // padding
        uboBuffer.putFloat(0f); // padding

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(Matrix2Double value) {
        this.value.set(value);
    }

}