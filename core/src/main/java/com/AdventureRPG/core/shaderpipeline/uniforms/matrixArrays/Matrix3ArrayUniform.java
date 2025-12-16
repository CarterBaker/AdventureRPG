package com.AdventureRPG.core.shaderpipeline.uniforms.matrixArrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix3;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix3ArrayUniform extends UniformAttribute<Matrix3[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix3ArrayUniform(int count) {
        super(new Matrix3[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Matrix3();

        this.buffer = BufferUtils.newByteBuffer(count * 36); // count * 9 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix3[] value) {
        floatBuffer.clear();
        for (Matrix3 mat : value) {
            // Column-major order
            floatBuffer.put(mat.m00);
            floatBuffer.put(mat.m10);
            floatBuffer.put(mat.m20);
            floatBuffer.put(mat.m01);
            floatBuffer.put(mat.m11);
            floatBuffer.put(mat.m21);
            floatBuffer.put(mat.m02);
            floatBuffer.put(mat.m12);
            floatBuffer.put(mat.m22);
        }
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix3fv(handle, value.length, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Matrix3 mat : value) {
            // Column-major order for UBO
            buffer.putFloat(mat.m00);
            buffer.putFloat(mat.m10);
            buffer.putFloat(mat.m20);
            buffer.putFloat(mat.m01);
            buffer.putFloat(mat.m11);
            buffer.putFloat(mat.m21);
            buffer.putFloat(mat.m02);
            buffer.putFloat(mat.m12);
            buffer.putFloat(mat.m22);
        }
        buffer.flip();
        return buffer;
    }
}