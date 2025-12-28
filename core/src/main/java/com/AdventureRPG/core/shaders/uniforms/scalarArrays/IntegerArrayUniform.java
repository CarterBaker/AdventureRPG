package com.AdventureRPG.core.shaders.uniforms.scalarArrays;

import com.AdventureRPG.core.shaders.uniforms.UniformAttribute;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.BufferUtils;

import java.nio.ByteBuffer;

public final class IntegerArrayUniform extends UniformAttribute<int[]> {

    // Internal
    private final int elementCount;
    private final ByteBuffer uboBuffer;

    public IntegerArrayUniform(int elementCount) {

        // Internal
        super(new int[elementCount]);
        this.elementCount = elementCount;
        this.uboBuffer = BufferUtils.newByteBuffer(elementCount * 4);
    }

    @Override
    protected void push(int handle, int[] data) {
        Gdx.gl.glUniform1iv(handle, elementCount, data, 0);
    }

    @Override
    public ByteBuffer getByteBuffer() {

        uboBuffer.clear();

        for (int i = 0; i < elementCount; i++)
            uboBuffer.putInt(value[i]);

        uboBuffer.flip();
        return uboBuffer;
    }

    @Override
    public void set(int[] values) {
        System.arraycopy(values, 0, this.value, 0, elementCount);
    }

    public int elementCount() {
        return elementCount;
    }
}