package com.AdventureRPG.core.shaders.uniforms.matrixArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix4ArrayUniform extends UniformAttribute<Matrix4[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix4ArrayUniform(int count) {
        super(new Matrix4[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Matrix4();

        this.buffer = BufferUtils.newByteBuffer(count * 64); // count * 16 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix4[] value) {
        floatBuffer.clear();
        for (Matrix4 mat : value) {
            // Column-major order
            floatBuffer.put(mat.m00);
            floatBuffer.put(mat.m10);
            floatBuffer.put(mat.m20);
            floatBuffer.put(mat.m30);
            floatBuffer.put(mat.m01);
            floatBuffer.put(mat.m11);
            floatBuffer.put(mat.m21);
            floatBuffer.put(mat.m31);
            floatBuffer.put(mat.m02);
            floatBuffer.put(mat.m12);
            floatBuffer.put(mat.m22);
            floatBuffer.put(mat.m32);
            floatBuffer.put(mat.m03);
            floatBuffer.put(mat.m13);
            floatBuffer.put(mat.m23);
            floatBuffer.put(mat.m33);
        }
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix4fv(handle, value.length, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Matrix4 mat : value) {
            // Column-major order for UBO
            buffer.putFloat(mat.m00);
            buffer.putFloat(mat.m10);
            buffer.putFloat(mat.m20);
            buffer.putFloat(mat.m30);
            buffer.putFloat(mat.m01);
            buffer.putFloat(mat.m11);
            buffer.putFloat(mat.m21);
            buffer.putFloat(mat.m31);
            buffer.putFloat(mat.m02);
            buffer.putFloat(mat.m12);
            buffer.putFloat(mat.m22);
            buffer.putFloat(mat.m32);
            buffer.putFloat(mat.m03);
            buffer.putFloat(mat.m13);
            buffer.putFloat(mat.m23);
            buffer.putFloat(mat.m33);
        }
        buffer.flip();
        return buffer;
    }
}