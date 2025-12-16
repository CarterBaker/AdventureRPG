package com.AdventureRPG.core.shaderpipeline.uniforms.matrices;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix4Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix4DoubleUniform extends UniformAttribute<Matrix4Double> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix4DoubleUniform() {
        super(new Matrix4Double());
        this.buffer = BufferUtils.newByteBuffer(64); // as floats
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix4Double value) {
        floatBuffer.clear();
        // Column-major order, convert to float
        floatBuffer.put((float) value.m00);
        floatBuffer.put((float) value.m10);
        floatBuffer.put((float) value.m20);
        floatBuffer.put((float) value.m30);
        floatBuffer.put((float) value.m01);
        floatBuffer.put((float) value.m11);
        floatBuffer.put((float) value.m21);
        floatBuffer.put((float) value.m31);
        floatBuffer.put((float) value.m02);
        floatBuffer.put((float) value.m12);
        floatBuffer.put((float) value.m22);
        floatBuffer.put((float) value.m32);
        floatBuffer.put((float) value.m03);
        floatBuffer.put((float) value.m13);
        floatBuffer.put((float) value.m23);
        floatBuffer.put((float) value.m33);
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix4fv(handle, 1, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        // Column-major order for UBO
        buffer.putFloat((float) value.m00);
        buffer.putFloat((float) value.m10);
        buffer.putFloat((float) value.m20);
        buffer.putFloat((float) value.m30);
        buffer.putFloat((float) value.m01);
        buffer.putFloat((float) value.m11);
        buffer.putFloat((float) value.m21);
        buffer.putFloat((float) value.m31);
        buffer.putFloat((float) value.m02);
        buffer.putFloat((float) value.m12);
        buffer.putFloat((float) value.m22);
        buffer.putFloat((float) value.m32);
        buffer.putFloat((float) value.m03);
        buffer.putFloat((float) value.m13);
        buffer.putFloat((float) value.m23);
        buffer.putFloat((float) value.m33);
        buffer.flip();
        return buffer;
    }
}