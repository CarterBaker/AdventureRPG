package com.AdventureRPG.core.shaders.uniforms.matrixArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix4Double;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix4DoubleArrayUniform extends UniformAttribute<float[]> {

    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Matrix4DoubleArrayUniform(int elementCount) {
        super(new float[elementCount * 16]);
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 64);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Matrix4Double[] matrices) {
        for (int i = 0; i < elementCount; i++) {
            int offset = i * 16;
            Matrix4Double m = matrices[i];
            value[offset] = (float) m.m00;
            value[offset + 1] = (float) m.m10;
            value[offset + 2] = (float) m.m20;
            value[offset + 3] = (float) m.m30;
            value[offset + 4] = (float) m.m01;
            value[offset + 5] = (float) m.m11;
            value[offset + 6] = (float) m.m21;
            value[offset + 7] = (float) m.m31;
            value[offset + 8] = (float) m.m02;
            value[offset + 9] = (float) m.m12;
            value[offset + 10] = (float) m.m22;
            value[offset + 11] = (float) m.m32;
            value[offset + 12] = (float) m.m03;
            value[offset + 13] = (float) m.m13;
            value[offset + 14] = (float) m.m23;
            value[offset + 15] = (float) m.m33;
        }
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniformMatrix4fv(handle, elementCount, false, data, 0);
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
