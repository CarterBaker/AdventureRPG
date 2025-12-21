package com.AdventureRPG.core.shaders.uniforms.matrixArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.AdventureRPG.core.util.Mathematics.Matrices.Matrix2;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class Matrix2ArrayUniform extends UniformAttribute<float[]> {

    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public Matrix2ArrayUniform(int elementCount) {
        super(new float[elementCount * 4]); // 2x2 = 4 floats per matrix
        this.elementCount = elementCount;
        this.buffer = BufferUtils.newByteBuffer(elementCount * 16); // 4 floats * 4 bytes
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Matrix2[] matrices) {
        for (int i = 0; i < elementCount; i++) {
            int offset = i * 4;
            value[offset] = matrices[i].m00;
            value[offset + 1] = matrices[i].m10;
            value[offset + 2] = matrices[i].m01;
            value[offset + 3] = matrices[i].m11;
        }
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniformMatrix2fv(handle, elementCount, false, data, 0);
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
