package com.AdventureRPG.core.shaders.uniforms.scalarArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public final class DoubleArrayUniform extends UniformAttribute<float[]> {

    private final int elementCount;
    private final ByteBuffer buffer;
    private final FloatBuffer floatBuffer;

    public DoubleArrayUniform(int elementCount) {
        super(new float[elementCount]);
        this.elementCount = elementCount;

        this.buffer = BufferUtils.newByteBuffer(elementCount * 4);
        this.floatBuffer = buffer.asFloatBuffer();
    }

    public void set(Double[] values) {
        for (int i = 0; i < elementCount; i++)
            value[i] = values[i].floatValue();
    }

    @Override
    protected void push(int handle, float[] data) {
        Gdx.gl.glUniform1fv(handle, elementCount, data, 0);
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
