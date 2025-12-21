package com.AdventureRPG.core.shaders.uniforms.scalarArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public final class BooleanArrayUniform extends UniformAttribute<int[]> {

    private final int elementCount;
    private final ByteBuffer buffer;
    private final IntBuffer intBuffer;

    public BooleanArrayUniform(int elementCount) {
        super(new int[elementCount]);
        this.elementCount = elementCount;

        this.buffer = BufferUtils.newByteBuffer(elementCount * 4);
        this.intBuffer = buffer.asIntBuffer();
    }

    public void set(Boolean[] values) {
        for (int i = 0; i < elementCount; i++)
            value[i] = values[i] ? 1 : 0;
    }

    @Override
    protected void push(int handle, int[] data) {
        Gdx.gl.glUniform1iv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {
        intBuffer.clear();
        intBuffer.put(value);
        intBuffer.flip();
        return buffer;
    }

    public int elementCount() {
        return elementCount;
    }
}
