package com.AdventureRPG.core.shaderpipeline.uniforms.matrixArrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix2Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix2DoubleArrayUniform extends UniformAttribute<Matrix2Double[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix2DoubleArrayUniform(int count) {
        super(new Matrix2Double[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Matrix2Double();

        this.buffer = BufferUtils.newByteBuffer(count * 16); // as floats
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix2Double[] value) {
        floatBuffer.clear();
        for (Matrix2Double mat : value) {
            // Column-major order, convert to float
            floatBuffer.put((float) mat.m00);
            floatBuffer.put((float) mat.m10);
            floatBuffer.put((float) mat.m01);
            floatBuffer.put((float) mat.m11);
        }
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix2fv(handle, value.length, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Matrix2Double mat : value) {
            // Column-major order for UBO
            buffer.putFloat((float) mat.m00);
            buffer.putFloat((float) mat.m10);
            buffer.putFloat((float) mat.m01);
            buffer.putFloat((float) mat.m11);
        }
        buffer.flip();
        return buffer;
    }
}