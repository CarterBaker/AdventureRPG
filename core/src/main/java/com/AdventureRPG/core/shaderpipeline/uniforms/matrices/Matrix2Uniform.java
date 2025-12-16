package com.AdventureRPG.core.shaderpipeline.uniforms.matrices;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix2Uniform extends UniformAttribute<Matrix2> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix2Uniform() {
        super(new Matrix2());
        this.buffer = BufferUtils.newByteBuffer(16); // 4 floats * 4 bytes (2x2 matrix)
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix2 value) {
        floatBuffer.clear();
        // Column-major order
        floatBuffer.put(value.m00);
        floatBuffer.put(value.m10);
        floatBuffer.put(value.m01);
        floatBuffer.put(value.m11);
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix2fv(handle, 1, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        // Column-major order for UBO
        buffer.putFloat(value.m00);
        buffer.putFloat(value.m10);
        buffer.putFloat(value.m01);
        buffer.putFloat(value.m11);
        buffer.flip();
        return buffer;
    }
}