package com.AdventureRPG.core.shaders.uniforms.matrices;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix4;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix4Uniform extends UniformAttribute<Matrix4> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix4Uniform() {
        super(new Matrix4());
        this.buffer = BufferUtils.newByteBuffer(64); // 16 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix4 value) {
        floatBuffer.clear();
        // Column-major order
        floatBuffer.put(value.m00);
        floatBuffer.put(value.m10);
        floatBuffer.put(value.m20);
        floatBuffer.put(value.m30);
        floatBuffer.put(value.m01);
        floatBuffer.put(value.m11);
        floatBuffer.put(value.m21);
        floatBuffer.put(value.m31);
        floatBuffer.put(value.m02);
        floatBuffer.put(value.m12);
        floatBuffer.put(value.m22);
        floatBuffer.put(value.m32);
        floatBuffer.put(value.m03);
        floatBuffer.put(value.m13);
        floatBuffer.put(value.m23);
        floatBuffer.put(value.m33);
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix4fv(handle, 1, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        // Column-major order for UBO
        buffer.putFloat(value.m00);
        buffer.putFloat(value.m10);
        buffer.putFloat(value.m20);
        buffer.putFloat(value.m30);
        buffer.putFloat(value.m01);
        buffer.putFloat(value.m11);
        buffer.putFloat(value.m21);
        buffer.putFloat(value.m31);
        buffer.putFloat(value.m02);
        buffer.putFloat(value.m12);
        buffer.putFloat(value.m22);
        buffer.putFloat(value.m32);
        buffer.putFloat(value.m03);
        buffer.putFloat(value.m13);
        buffer.putFloat(value.m23);
        buffer.putFloat(value.m33);
        buffer.flip();
        return buffer;
    }
}