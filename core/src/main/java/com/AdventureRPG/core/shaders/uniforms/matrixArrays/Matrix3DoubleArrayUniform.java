package com.AdventureRPG.core.shaders.uniforms.matrixArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Methematics.Matrices.Matrix3Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class Matrix3DoubleArrayUniform extends UniformAttribute<Matrix3Double[]> {

    private ByteBuffer buffer;
    private FloatBuffer floatBuffer;

    public Matrix3DoubleArrayUniform(int count) {
        super(new Matrix3Double[count]);
        for (int i = 0; i < count; i++)
            value[i] = new Matrix3Double();

        this.buffer = BufferUtils.newByteBuffer(count * 36); // as floats
        this.floatBuffer = buffer.asFloatBuffer();
    }

    @Override
    protected void push(int handle, Matrix3Double[] value) {
        floatBuffer.clear();
        for (Matrix3Double mat : value) {
            // Column-major order, convert to float
            floatBuffer.put((float) mat.m00);
            floatBuffer.put((float) mat.m10);
            floatBuffer.put((float) mat.m20);
            floatBuffer.put((float) mat.m01);
            floatBuffer.put((float) mat.m11);
            floatBuffer.put((float) mat.m21);
            floatBuffer.put((float) mat.m02);
            floatBuffer.put((float) mat.m12);
            floatBuffer.put((float) mat.m22);
        }
        floatBuffer.flip();

        Gdx.gl.glUniformMatrix3fv(handle, value.length, false, floatBuffer);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        buffer.clear();
        for (Matrix3Double mat : value) {
            // Column-major order for UBO
            buffer.putFloat((float) mat.m00);
            buffer.putFloat((float) mat.m10);
            buffer.putFloat((float) mat.m20);
            buffer.putFloat((float) mat.m01);
            buffer.putFloat((float) mat.m11);
            buffer.putFloat((float) mat.m21);
            buffer.putFloat((float) mat.m02);
            buffer.putFloat((float) mat.m12);
            buffer.putFloat((float) mat.m22);
        }
        buffer.flip();
        return buffer;
    }
}