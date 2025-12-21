package com.AdventureRPG.core.shaders.uniforms.matrixArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix3Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix3DoubleArrayUniform extends UniformAttribute<float[]> {

    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Matrix3DoubleArrayUniform(int elementCount) {
        super(new float[elementCount * 9]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 36);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Matrix3Double[] matrices) {
        for (int i = 0; i < elementCount; i++) {
            int offset = i * 9;
            Matrix3Double m = matrices[i];
            value[offset] = (float) m.m00;
            value[offset + 1] = (float) m.m10;
            value[offset + 2] = (float) m.m20;
            value[offset + 3] = (float) m.m01;
            value[offset + 4] = (float) m.m11;
            value[offset + 5] = (float) m.m21;
            value[offset + 6] = (float) m.m02;
            value[offset + 7] = (float) m.m12;
            value[offset + 8] = (float) m.m22;
        }
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniformMatrix3fv(handle, elementCount, false, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        floatBuffer.clear();
        floatBuffer.put(value);
        floatBuffer.flip();
        return buffer;
    }

    public int elementCount() {
        return elementCount;
    }
}
