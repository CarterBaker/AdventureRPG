package com.AdventureRPG.core.shaderpipeline.uniforms.matrixArrays;

import com.AdventureRPG.core.shaderpipeline.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix4Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix4DoubleArrayUniform extends UniformAttribute<Matrix4Double[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix4DoubleArrayUniform(int count) {
        super(new Matrix4Double[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Matrix4Double();

        this.buffer = BufferUtils.newByteBuffer(count * 64); // as floats
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix4Double[] value) {
        floatBuffer.clear();
        for (Matrix4Double mat : value) {
            // Column-major order, convert to float
            floatBuffer.put((float) mat.m00);
            floatBuffer.put((float) mat.m10);
            floatBuffer.put((float) mat.m20);
            floatBuffer.put((float) mat.m30);
            floatBuffer.put((float) mat.m01);
            floatBuffer.put((float) mat.m11);
            floatBuffer.put((float) mat.m21);
            floatBuffer.put((float) mat.m31);
            floatBuffer.put((float) mat.m02);
            floatBuffer.put((float) mat.m12);
            floatBuffer.put((float) mat.m22);
            floatBuffer.put((float) mat.m32);
            floatBuffer.put((float) mat.m03);
            floatBuffer.put((float) mat.m13);
            floatBuffer.put((float) mat.m23);
            floatBuffer.put((float) mat.m33);
        }
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix4fv(handle, value.length, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Matrix4Double mat : value) {
            // Column-major order for UBO
            buffer.putFloat((float) mat.m00);
            buffer.putFloat((float) mat.m10);
            buffer.putFloat((float) mat.m20);
            buffer.putFloat((float) mat.m30);
            buffer.putFloat((float) mat.m01);
            buffer.putFloat((float) mat.m11);
            buffer.putFloat((float) mat.m21);
            buffer.putFloat((float) mat.m31);
            buffer.putFloat((float) mat.m02);
            buffer.putFloat((float) mat.m12);
            buffer.putFloat((float) mat.m22);
            buffer.putFloat((float) mat.m32);
            buffer.putFloat((float) mat.m03);
            buffer.putFloat((float) mat.m13);
            buffer.putFloat((float) mat.m23);
            buffer.putFloat((float) mat.m33);
        }
        buffer.flip();
        return buffer;
    }
}