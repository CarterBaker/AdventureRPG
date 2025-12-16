package com.AdventureRPG.core.shaderpipeline.uniforms.matrices;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix2Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix2DoubleUniform extends UniformAttribute<Matrix2Double> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix2DoubleUniform() {
        super(new Matrix2Double());
        this.buffer = BufferUtils.newByteBuffer(16); // as floats
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix2Double value) {
        floatBuffer.clear();
        // Column-major order, convert to float
        floatBuffer.put((float) value.m00);
        floatBuffer.put((float) value.m10);
        floatBuffer.put((float) value.m01);
        floatBuffer.put((float) value.m11);
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix2fv(handle, 1, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        // Column-major order for UBO
        buffer.putFloat((float) value.m00);
        buffer.putFloat((float) value.m10);
        buffer.putFloat((float) value.m01);
        buffer.putFloat((float) value.m11);
        buffer.flip();
        return buffer;
    }
}