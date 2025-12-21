package com.AdventureRPG.core.shaders.uniforms.matrixArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix3;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix3ArrayUniform extends UniformAttribute<float[]> {

    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Matrix3ArrayUniform(int elementCount) {
        super(new float[elementCount * 9]); // 3x3 = 9 floats per matrix
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 36); // 9 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Matrix3[] matrices) {
        for (int i = 0; i < elementCount; i++) {
            int offset = i * 9;
            Matrix3 m = matrices[i];
            value[offset] = m.m00;
            value[offset + 1] = m.m10;
            value[offset + 2] = m.m20;
            value[offset + 3] = m.m01;
            value[offset + 4] = m.m11;
            value[offset + 5] = m.m21;
            value[offset + 6] = m.m02;
            value[offset + 7] = m.m12;
            value[offset + 8] = m.m22;
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
