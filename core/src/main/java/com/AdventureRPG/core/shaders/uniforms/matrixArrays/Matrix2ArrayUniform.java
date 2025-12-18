package com.AdventureRPG.core.shaders.uniforms.matrixArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix2ArrayUniform extends UniformAttribute<Matrix2[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix2ArrayUniform(int count) {
        super(new Matrix2[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Matrix2();

        this.buffer = BufferUtils.newByteBuffer(count * 16); // count * 4 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix2[] value) {
        floatBuffer.clear();
        for (Matrix2 mat : value) {
            // Column-major order
            floatBuffer.put(mat.m00);
            floatBuffer.put(mat.m10);
            floatBuffer.put(mat.m01);
            floatBuffer.put(mat.m11);
        }
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix2fv(handle, value.length, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Matrix2 mat : value) {
            // Column-major order for UBO
            buffer.putFloat(mat.m00);
            buffer.putFloat(mat.m10);
            buffer.putFloat(mat.m01);
            buffer.putFloat(mat.m11);
        }
        buffer.flip();
        return buffer;
    }
}